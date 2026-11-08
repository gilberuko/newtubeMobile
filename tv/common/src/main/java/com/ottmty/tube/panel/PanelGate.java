package com.ottmty.tube.panel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/** Puerta de entrada: sin sesión solo se ve el login; con sesión se revisa que la cuenta siga vigente. */
public final class PanelGate {
    /** Cada cuánto se vuelve a preguntar al servidor (salvo en la apertura de la app). */
    private static final long CHECK_EVERY_MS = 5 * 60 * 1000L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static long sLastCheck = 0L;
    private static boolean sChecking = false;
    private static long sLoginStartedAt = 0L;

    private PanelGate() {
    }

    /**
     * Debe llamarse al inicio de las pantallas de entrada. Si no hay sesión abre el login y cierra
     * la pantalla; devuelve true para que quien llama deje de hacer su trabajo.
     */
    public static boolean requireLogin(Activity a) {
        if (PanelSession.isLoggedIn(a)) return false;
        // Varias pantallas pueden llamar a la vez: el login se abre una sola vez.
        long now = SystemClock.elapsedRealtime();
        if (sLoginStartedAt == 0L || now - sLoginStartedAt > 1500L) {
            sLoginStartedAt = now;
            goToLogin(a);
        }
        a.finish();
        return true;
    }

    public static void goToLogin(Context c) {
        Intent i = new Intent(c.getApplicationContext(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        c.getApplicationContext().startActivity(i);
    }

    /** Cierra la sesión y vuelve al login. */
    public static void logout(Context c, String notice) {
        PanelSession.clear(c);
        PanelSession.setNotice(notice);
        sLastCheck = 0L;
        goToLogin(c);
    }

    /**
     * Revisa la cuenta contra el servidor. Sin conexión conserva la sesión; solo una cuenta
     * inválida o vencida la cierra. {@code force} ignora el intervalo (apertura de la app).
     */
    public static void revalidate(Context c, boolean force) {
        final Context app = c.getApplicationContext();
        if (!PanelSession.isLoggedIn(app) || sChecking) return;
        long now = SystemClock.elapsedRealtime();
        if (!force && sLastCheck != 0L && now - sLastCheck < CHECK_EVERY_MS) return;
        sChecking = true;
        final String host = PanelSession.getHost(app);
        final String user = PanelSession.getUser(app);
        final String pass = PanelSession.getPass(app);
        new Thread(() -> {
            final PanelLogin.Result r = PanelLogin.revalidate(host, user, pass);
            MAIN.post(() -> {
                sChecking = false;
                switch (r.status) {
                    case SUCCESS:
                        sLastCheck = SystemClock.elapsedRealtime();
                        PanelSession.updateAccount(app, r);
                        break;
                    case INVALID:
                        logout(app, "Usuario o contraseña incorrectos.");
                        break;
                    case EXPIRED:
                        logout(app, "Tu cuenta venció. Renuévala para entrar.");
                        break;
                    default:
                        break; // sin conexión: se conserva la sesión
                }
            });
        }).start();
    }
}
