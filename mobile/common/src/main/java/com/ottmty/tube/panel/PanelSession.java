package com.ottmty.tube.panel;

import android.content.Context;
import android.content.SharedPreferences;

/** Sesión guardada del panel: usuario, contraseña, servidor y vigencia. */
public final class PanelSession {
    private static final String PREFS = "ottmty_panel_session";
    private static final String USER = "user";
    private static final String PASS = "pass";
    private static final String HOST = "host";
    private static final String EXPIRES = "expires";
    private static final String TRIAL = "trial";
    private static final String OFFSET = "offset";

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

    public static void save(Context c, String user, String pass, PanelLogin.Result r) {
        prefs(c).edit()
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
