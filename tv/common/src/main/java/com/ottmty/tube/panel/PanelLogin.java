package com.ottmty.tube.panel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Inicio de sesión con usuario y contraseña ligado al panel ibo.
 *
 * Mismo flujo que Spoty y la app OTTMTY: se pide al panel la lista de servidores
 * (RTXRebrand.php y, si no sirve, dns.php) y se le pregunta a cada servidor Xtream
 * (player_api.php) si el usuario y la contraseña son válidos y si la cuenta sigue vigente.
 *
 * Todos los métodos bloquean: se llaman desde un hilo de fondo.
 */
public final class PanelLogin {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public enum Status { SUCCESS, INVALID, EXPIRED, NO_CONNECTION, DEVICE_LIMIT }

    /** Respuesta del panel sobre el registro de este dispositivo. UNKNOWN: el servidor no lo maneja o no hubo conexión. */
    public enum DeviceState { OK, LIMIT, REMOVED, UNKNOWN }

    public static final class Device {
        public final DeviceState state;
        /** Máximo de dispositivos que permite la cuenta (0 si no se sabe). */
        public final int max;

        Device(DeviceState state, int max) {
            this.state = state;
            this.max = max;
        }
    }

    public static final class Result {
        public final Status status;
        /** Servidor donde se validó la cuenta. */
        public final String host;
        /** Vencimiento en segundos (0 = sin vencimiento). */
        public final long expiresAt;
        /** Si la cuenta es un demo. */
        public final boolean trial;
        /** Diferencia entre la hora del servidor y la del equipo (para que el contador no dependa del reloj del equipo). */
        public final long serverOffsetMs;
        /** Si el panel confirmó el registro de este dispositivo en la cuenta. */
        public final boolean deviceRegistered;
        /** Máximo de dispositivos de la cuenta (solo cuando status es DEVICE_LIMIT). */
        public final int deviceMax;

        Result(Status status, String host, long expiresAt, boolean trial, long serverOffsetMs) {
            this(status, host, expiresAt, trial, serverOffsetMs, false, 0);
        }

        Result(Status status, String host, long expiresAt, boolean trial, long serverOffsetMs, boolean deviceRegistered, int deviceMax) {
            this.status = status;
            this.host = host;
            this.expiresAt = expiresAt;
            this.trial = trial;
            this.serverOffsetMs = serverOffsetMs;
            this.deviceRegistered = deviceRegistered;
            this.deviceMax = deviceMax;
        }

        static Result of(Status status) {
            return new Result(status, "", 0L, false, 0L);
        }
    }

    private enum State { ACTIVE, EXPIRED, INVALID, NO_SERVER }

    private static final class Probe {
        final State state;
        final long expiresAt;
        final boolean trial;
        final long offsetMs;

        Probe(State state, long expiresAt, boolean trial, long offsetMs) {
            this.state = state;
            this.expiresAt = expiresAt;
            this.trial = trial;
            this.offsetMs = offsetMs;
        }

        Probe(State state) {
            this(state, 0L, false, 0L);
        }
    }

    private PanelLogin() {
    }

    /**
     * Primer inicio de sesión: prueba todos los servidores del panel. Si la cuenta es válida registra este
     * dispositivo en ella (máximo 4 por cuenta en el panel PRUEBAS XUI); sin lugar devuelve DEVICE_LIMIT.
     */
    public static Result login(String user, String pass, String deviceId, String label) {
        List<String> hosts;
        try {
            hosts = portals();
        } catch (Exception e) {
            hosts = new ArrayList<>();
        }
        if (hosts.isEmpty()) {
            return Result.of(Status.NO_CONNECTION);
        }
        boolean invalid = false;
        boolean expired = false;
        for (String host : hosts) {
            Probe probe = probe(host, user, pass);
            switch (probe.state) {
                case ACTIVE: {
                    Device d = registerDevice(host, user, pass, deviceId, label);
                    if (d.state == DeviceState.LIMIT) {
                        return new Result(Status.DEVICE_LIMIT, host, 0L, false, 0L, false, d.max);
                    }
                    return new Result(Status.SUCCESS, host, probe.expiresAt, probe.trial, probe.offsetMs,
                            d.state == DeviceState.OK, d.max);
                }
                case EXPIRED:
                    expired = true;
                    break;
                case INVALID:
                    invalid = true;
                    break;
                default:
                    break;
            }
        }
        if (expired) return Result.of(Status.EXPIRED);
        if (invalid) return Result.of(Status.INVALID);
        return Result.of(Status.NO_CONNECTION);
    }

    /**
     * Revisa una sesión ya guardada contra el servidor donde se validó. Sin conexión devuelve
     * NO_CONNECTION y quien llama conserva la sesión.
     */
    public static Result revalidate(String host, String user, String pass) {
        Probe probe = probe(host, user, pass);
        switch (probe.state) {
            case ACTIVE:
                return new Result(Status.SUCCESS, host, probe.expiresAt, probe.trial, probe.offsetMs);
            case EXPIRED:
                return Result.of(Status.EXPIRED);
            case INVALID:
                return Result.of(Status.INVALID);
            default:
                return Result.of(Status.NO_CONNECTION);
        }
    }

    // ---- Dispositivos de la cuenta -------------------------------------------

    /** Registra este dispositivo en la cuenta (si ya estaba, solo anota la conexión). */
    public static Device registerDevice(String host, String user, String pass, String deviceId, String label) {
        return deviceCall(host, user, pass, "login", deviceId, label);
    }

    /** Pregunta si este dispositivo sigue registrado (REMOVED = lo quitaron desde el panel). */
    public static Device checkDevice(String host, String user, String pass, String deviceId) {
        return deviceCall(host, user, pass, "check", deviceId, "");
    }

    /** Cerrar sesión: libera el lugar de este dispositivo. */
    public static void releaseDevice(String host, String user, String pass, String deviceId) {
        deviceCall(host, user, pass, "logout", deviceId, "");
    }

    private static Device deviceCall(String host, String user, String pass, String mode, String deviceId, String label) {
        try {
            // El registro lo lleva el panel (ottmty-panel), no el servidor Xtream.
            String url = PanelConfig.PANEL_BASE + "/api/app_device.php?username=" + URLEncoder.encode(user, "UTF-8")
                    + "&password=" + URLEncoder.encode(pass, "UTF-8")
                    + "&modo=" + mode
                    + "&app=" + PanelConfig.APP_KEY
                    + "&device=" + URLEncoder.encode(deviceId, "UTF-8")
                    + "&label=" + URLEncoder.encode(label == null ? "" : label, "UTF-8");
            JSONObject d = new JSONObject(get(url)).optJSONObject("app_device");
            if (d == null) return new Device(DeviceState.UNKNOWN, 0);
            int max = d.optInt("max", 0);
            String status = d.optString("status");
            if ("ok".equals(status)) return new Device(DeviceState.OK, max);
            if ("limit".equals(status)) return new Device(DeviceState.LIMIT, max);
            if ("removed".equals(status)) return new Device(DeviceState.REMOVED, max);
            return new Device(DeviceState.UNKNOWN, max);
        } catch (Exception e) {
            // Panel sin esta función o sin conexión: no se bloquea a nadie por eso.
            return new Device(DeviceState.UNKNOWN, 0);
        }
    }

    // ---- Panel: lista de servidores ----------------------------------------

    private static List<String> portals() throws Exception {
        // 1) RTXRebrand.php: JSON { "<clave>": "<hex al revés>" } con "portals"
        try {
            JSONObject j = new JSONObject(get(PanelConfig.PANEL_BASE + "/api/RTXRebrand.php"));
            Iterator<String> keys = j.keys();
            String key = keys.next();
            JSONObject json = new JSONObject(hexReversed(j.getString(key)));
            JSONArray arr = json.getJSONArray("portals");
            List<String> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                String url = trimSlash(arr.getJSONObject(i).optString("rtxurls"));
                if (!url.isEmpty()) out.add(url);
            }
            if (!out.isEmpty()) return out;
        } catch (Exception ignored) {
            // se intenta el segundo método
        }
        // 2) dns.php: AES-256-CBC en hexadecimal
        JSONArray arr = new JSONArray(decryptDns(get(PanelConfig.PANEL_BASE + "/api/dns.php")));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            String url = trimSlash(arr.getJSONObject(i).optString("url"));
            if (!url.isEmpty()) out.add(url);
        }
        return out;
    }

    private static String trimSlash(String s) {
        String t = s == null ? "" : s.trim();
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    // ---- Servidor Xtream ----------------------------------------------------

    private static Probe probe(String host, String user, String pass) {
        try {
            String base = host.toLowerCase().startsWith("http") ? host : "http://" + host;
            String url = trimSlash(base) + "/player_api.php?username=" + URLEncoder.encode(user, "UTF-8")
                    + "&password=" + URLEncoder.encode(pass, "UTF-8");
            JSONObject json = new JSONObject(get(url));
            JSONObject info = json.optJSONObject("user_info");
            if (info == null) return new Probe(State.INVALID);
            Object auth = info.opt("auth");
            if (!"1".equals(auth == null ? "0" : auth.toString())) return new Probe(State.INVALID);
            String status = info.optString("status");
            long exp = parseLong(info.optString("exp_date"));
            Object trialValue = info.opt("is_trial");
            boolean trial = trialValue != null && "1".equals(trialValue.toString());
            // Hora del servidor (si la manda), para que el contador no dependa del reloj del equipo.
            JSONObject serverInfo = json.optJSONObject("server_info");
            long serverNow = serverInfo == null ? 0L : parseLong(serverInfo.optString("timestamp_now"));
            long offset = serverNow > 0L ? serverNow * 1000 - System.currentTimeMillis() : 0L;
            long now = (System.currentTimeMillis() + offset) / 1000;
            if ("Expired".equalsIgnoreCase(status)) return new Probe(State.EXPIRED);
            if (exp > 0L && exp < now) return new Probe(State.EXPIRED);
            if ("Active".equalsIgnoreCase(status)) return new Probe(State.ACTIVE, exp, trial, offset);
            return new Probe(State.INVALID);
        } catch (IOException e) {
            return new Probe(State.NO_SERVER);
        } catch (Exception e) {
            return new Probe(State.INVALID);
        }
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    // ---- HTTP y descifrado --------------------------------------------------

    private static String get(String urlText) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(urlText).openConnection();
        try {
            c.setConnectTimeout(10000);
            c.setReadTimeout(12000);
            c.setRequestProperty("Accept", "application/json");
            int code = c.getResponseCode();
            if (code < 200 || code > 299) throw new IOException("HTTP " + code);
            InputStream in = c.getInputStream();
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return new String(out.toByteArray(), UTF8);
            } finally {
                in.close();
            }
        } finally {
            c.disconnect();
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) throw new IllegalArgumentException("respuesta no válida");
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("respuesta no válida");
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    /** Texto volteado y convertido de hexadecimal a texto. */
    private static String hexReversed(String text) {
        return new String(hexToBytes(new StringBuilder(text.trim()).reverse().toString()), UTF8);
    }

    /** Hexadecimal cifrado con AES-256-CBC más, al final, la clave (16 caracteres) al revés. */
    private static String decryptDns(String text) throws Exception {
        String t = text.trim();
        if (t.length() <= 16) throw new IllegalArgumentException("respuesta no válida");
        String key = new StringBuilder(t.substring(t.length() - 16)).reverse().toString();
        byte[] data = hexToBytes(t.substring(0, t.length() - 16));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec((key + key).getBytes(UTF8), "AES"),
                new IvParameterSpec("R4tghjg^425(@#Gg".getBytes(UTF8)));
        return new String(cipher.doFinal(data), UTF8);
    }
}
