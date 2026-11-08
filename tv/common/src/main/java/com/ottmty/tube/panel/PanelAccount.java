package com.ottmty.tube.panel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Cálculos de la vigencia de la cuenta del panel: cuánto falta, cómo se muestra la fecha y el
 * contador, y la regla de cuándo avisar.
 */
public final class PanelAccount {
    /** El aviso aparece cuando faltan 5 días o menos (demos incluidos). */
    public static final int NOTICE_DAYS = 5;

    private static final long DAY_MS = 86_400_000L;

    private PanelAccount() {
    }

    /** Milisegundos que faltan, según la hora del servidor. Sin vencimiento: Long.MAX_VALUE. */
    public static long remainingMs(long expiresAt, long offsetMs) {
        if (expiresAt <= 0L) return Long.MAX_VALUE;
        return expiresAt * 1000 - (System.currentTimeMillis() + offsetMs);
    }

    /** Días que faltan, redondeados hacia arriba (queda 1 día aunque falten 3 horas). */
    public static long daysLeft(long remainingMs) {
        if (remainingMs <= 0L) return 0L;
        return (long) Math.ceil(remainingMs / (double) DAY_MS);
    }

    /** ¿Toca mostrar el aviso? Solo con vencimiento, sin vencer todavía y a 5 días o menos. */
    public static boolean shouldNotify(long expiresAt, long offsetMs) {
        if (expiresAt <= 0L) return false;
        long left = remainingMs(expiresAt, offsetMs);
        return left > 0L && daysLeft(left) <= NOTICE_DAYS;
    }

    public static String formatDate(long expiresAt) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(expiresAt * 1000));
    }

    /** "2 d 05:12:33" o "05:12:33". */
    public static String formatCountdown(long remainingMs) {
        long total = Math.max(remainingMs, 0L) / 1000;
        long days = total / 86_400;
        long h = (total % 86_400) / 3_600;
        long m = (total % 3_600) / 60;
        long s = total % 60;
        String clock = String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
        return days > 0 ? days + " d " + clock : clock;
    }
}
