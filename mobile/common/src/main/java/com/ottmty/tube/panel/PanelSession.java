package com.ottmty.tube.panel;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import java.util.UUID;

/** Sesión guardada del panel: usuario, contraseña, servidor y vigencia. */
public final class PanelSession {
    private static final String PREFS = "ottmty_panel_session";
    private static final String USER = "user";
    private static final String PASS = "pass";
    private static final String HOST = "host";
    private static final String EXPIRES = "expires";
    private static final String TRIAL = "trial";
    private static final String OFFSET = "offset";
    private static final String DEVICE_REGISTERED = "device_registered";
    /** El identificador del dispositivo vive aparte para que cerrar sesión no lo borre. */
    private static final String DEVICE_PREFS = "ottmty_panel_device";
    private static final String DEVICE_ID = "id";

    /** Motivo por el que se cerró la sesión; el login lo muestra una vez. */
    private static String sNotice = "";

    private PanelSession() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isLoggedIn(Context c) {
        return !getUser(c).isEmpty();
    }

    public static String getUser(Context c) {
        return prefs(c).getString(USER, "");
    }

    public static String getPass(Context c) {
        return prefs(c).getString(PASS, "");
    }

    public static String getHost(Context c) {
        return prefs(c).getString(HOST, "");
    }

    public static long getExpires(Context c) {
        return prefs(c).getLong(EXPIRES, 0L);
    }

    public static boolean isTrial(Context c) {
        return prefs(c).getBoolean(TRIAL, false);
    }

    public static long getOffsetMs(Context c) {
        return prefs(c).getLong(OFFSET, 0L);
    }

    /**
     * Identificador de este dispositivo para el panel (de 8 a 64 letras y números). Usa el ANDROID_ID;
     * si el equipo no lo da, se genera uno al azar y se guarda.
     */
    public static String getDeviceId(Context c) {
        Context app = c.getApplicationContext();
        SharedPreferences p = app.getSharedPreferences(DEVICE_PREFS, Context.MODE_PRIVATE);
        String id = p.getString(DEVICE_ID, "");
        if (!id.isEmpty()) return id;
        String android = "";
        try {
            android = Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception ignored) {
            // se genera uno al azar
        }
        if (android == null || !android.matches("[0-9A-Fa-f]{8,64}") || "9774d56d682e549c".equalsIgnoreCase(android)) {
            android = UUID.randomUUID().toString().replace("-", "");
        }
        id = android.toLowerCase();
        p.edit().putString(DEVICE_ID, id).apply();
        return id;
    }

    /** Nombre del equipo que se ve en el panel (por ejemplo "samsung SM-A546E"). */
    public static String getDeviceLabel() {
        String maker = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.trim();
        String model = Build.MODEL == null ? "" : Build.MODEL.trim();
        if (model.toLowerCase().startsWith(maker.toLowerCase())) return model;
        return (maker + " " + model).trim();
    }

    /** Si el panel ya confirmó el registro de este dispositivo en la cuenta de la sesión. */
    public static boolean isDeviceRegistered(Context c) {
        return prefs(c).getBoolean(DEVICE_REGISTERED, false);
    }

    public static void setDeviceRegistered(Context c, boolean registered) {
        prefs(c).edit().putBoolean(DEVICE_REGISTERED, registered).apply();
    }

    public static void save(Context c, String user, String pass, PanelLogin.Result r) {
        prefs(c).edit()
                .putBoolean(DEVICE_REGISTERED, r.deviceRegistered)
                .putString(USER, user)
                .putString(PASS, pass)
                .putString(HOST, r.host)
                .putLong(EXPIRES, r.expiresAt)
                .putBoolean(TRIAL, r.trial)
                .putLong(OFFSET, r.serverOffsetMs)
                .apply();
    }

    public static void updateAccount(Context c, PanelLogin.Result r) {
        prefs(c).edit()
                .putLong(EXPIRES, r.expiresAt)
                .putBoolean(TRIAL, r.trial)
                .putLong(OFFSET, r.serverOffsetMs)
                .apply();
    }

    public static void clear(Context c) {
        prefs(c).edit().clear().apply();
    }

    public static void setNotice(String notice) {
        sNotice = notice == null ? "" : notice;
    }

    /** Devuelve el aviso pendiente y lo borra. */
    public static String takeNotice() {
        String n = sNotice;
        sNotice = "";
        return n;
    }
}
