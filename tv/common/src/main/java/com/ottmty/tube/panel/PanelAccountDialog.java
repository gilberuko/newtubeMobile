package com.ottmty.tube.panel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Datos de la cuenta vinculada (para Configuración): usuario, vigencia y cerrar sesión. */
public final class PanelAccountDialog {
    private PanelAccountDialog() {
    }

    /** Texto de la vigencia: fecha de vencimiento, o "Demo" con su contador. */
    static String validityText(Activity a) {
        long expires = PanelSession.getExpires(a);
        if (expires <= 0L) return "Sin fecha de vencimiento";
        if (PanelSession.isTrial(a)) {
            long left = PanelAccount.remainingMs(expires, PanelSession.getOffsetMs(a));
            return "Demo · quedan " + PanelAccount.formatCountdown(left);
        }
        return PanelAccount.formatDate(expires);
    }

    /** Igual que show(Activity), pero acepta cualquier Context (se busca la pantalla que lo envuelve). */
    public static void show(android.content.Context c) {
        android.content.Context cur = c;
        while (cur instanceof android.content.ContextWrapper) {
            if (cur instanceof Activity) {
                show((Activity) cur);
                return;
            }
            cur = ((android.content.ContextWrapper) cur).getBaseContext();
        }
    }

    public static void show(final Activity a) {
        if (a == null || a.isFinishing()) return;
        final boolean tv = PanelConfig.IS_TV;

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = PanelUi.dp(a, 24);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(PanelUi.rounded(a, PanelUi.CARD, 24, Color.TRANSPARENT, 0));

        PanelUi.MarkView mark = new PanelUi.MarkView(a);
        box.addView(mark, new LinearLayout.LayoutParams(PanelUi.dp(a, 56), PanelUi.dp(a, 56)));

        TextView title = PanelUi.text(a, "CUENTA VINCULADA", tv ? 22 : 20, PanelUi.WHITE, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = PanelUi.dp(a, 12);
        box.addView(title, titleLp);

        TextView user = PanelUi.text(a, "Usuario: " + PanelSession.getUser(a), 16, PanelUi.WHITE, false);
        user.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams userLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        userLp.topMargin = PanelUi.dp(a, 16);
        box.addView(user, userLp);

        final TextView validity = PanelUi.text(a, "Vigencia: " + validityText(a), 16, PanelUi.WHITE, false);
        validity.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valLp.topMargin = PanelUi.dp(a, 6);
        box.addView(validity, valLp);

        final Dialog dialog = new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Button logout = PanelUi.button(a, "Cerrar sesión", true);
        logout.setOnClickListener(v -> confirmLogout(a, dialog));
        LinearLayout.LayoutParams logoutLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(a, 52));
        logoutLp.topMargin = PanelUi.dp(a, 22);
        box.addView(logout, logoutLp);

        Button close = PanelUi.button(a, "Cerrar", false);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(a, 52));
        closeLp.topMargin = PanelUi.dp(a, 10);
        box.addView(close, closeLp);

        // En un demo el contador corre mientras la ventana está abierta.
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable tick = new Runnable() {
            @Override
            public void run() {
                validity.setText("Vigencia: " + validityText(a));
                handler.postDelayed(this, 1000);
            }
        };
        if (PanelSession.isTrial(a) && PanelSession.getExpires(a) > 0L) handler.postDelayed(tick, 1000);
        dialog.setOnDismissListener(d -> handler.removeCallbacks(tick));
        dialog.setContentView(box);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(PanelUi.dp(a, tv ? 400 : 320), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
        close.requestFocus();
    }

    private static void confirmLogout(final Activity a, final Dialog parent) {
        new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("¿Cerrar sesión?")
                .setMessage("Para volver a entrar necesitarás tu usuario y contraseña.")
                .setPositiveButton("Cerrar sesión", (d, w) -> {
                    parent.dismiss();
                    PanelGate.logout(a, "");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
