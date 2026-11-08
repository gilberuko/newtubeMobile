package com.ottmty.tube.panel;

import android.app.Activity;
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

/**
 * Aviso de la cuenta: aparece al iniciar sesión y al abrir la app cuando a la cuenta le quedan
 * 5 días o menos (demos incluidos). Se muestra una vez por apertura de la app.
 */
public final class PanelNotice {
    private static String sShownFor = "";

    private PanelNotice() {
    }

    public static void showIfDue(final Activity a) {
        if (a == null || a.isFinishing()) return;
        final String user = PanelSession.getUser(a);
        if (user.isEmpty() || user.equals(sShownFor)) return;
        final long expires = PanelSession.getExpires(a);
        final long offset = PanelSession.getOffsetMs(a);
        final boolean trial = PanelSession.isTrial(a);
        if (!PanelAccount.shouldNotify(expires, offset)) return;
        sShownFor = user;

        final boolean tv = PanelConfig.IS_TV;
        final long remaining0 = PanelAccount.remainingMs(expires, offset);
        final long days = PanelAccount.daysLeft(remaining0);
        final boolean today = days <= 1L && remaining0 < 86_400_000L;

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = PanelUi.dp(a, 24);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(PanelUi.rounded(a, PanelUi.CARD, 24, Color.TRANSPARENT, 0));

        PanelUi.MarkView mark = new PanelUi.MarkView(a);
        box.addView(mark, new LinearLayout.LayoutParams(PanelUi.dp(a, 56), PanelUi.dp(a, 56)));

        TextView title = PanelUi.text(a, trial ? "Tu demo está activo" : "Tu servicio está por vencer", tv ? 22 : 19, PanelUi.WHITE, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = PanelUi.dp(a, 10);
        box.addView(title, titleLp);

        final TextView big = PanelUi.text(a, "", trial ? 36 : 52, PanelUi.RED, true);
        big.setGravity(Gravity.CENTER);
        box.addView(big, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        String captionText;
        if (trial) captionText = "Tiempo restante del demo";
        else if (today) captionText = "";
        else captionText = days == 1L ? "día restante" : "días restantes";
        TextView caption = PanelUi.text(a, captionText, 15, PanelUi.GRAY, false);
        caption.setGravity(Gravity.CENTER);
        if (captionText.isEmpty()) caption.setVisibility(android.view.View.GONE);
        box.addView(caption, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView expiresView = PanelUi.text(a, "Vence el " + PanelAccount.formatDate(expires), 14, PanelUi.WHITE, false);
        expiresView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams expLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        expLp.topMargin = PanelUi.dp(a, 8);
        box.addView(expiresView, expLp);

        TextView renew = PanelUi.text(a, "Renueva ahora para no quedarte sin servicio.", 13, PanelUi.GRAY, false);
        renew.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams renewLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        renewLp.topMargin = PanelUi.dp(a, 2);
        box.addView(renew, renewLp);

        final Dialog dialog = new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Button close = PanelUi.button(a, "Entendido", true);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(a, 52));
        closeLp.topMargin = PanelUi.dp(a, 18);
        box.addView(close, closeLp);

        // El contador (demo) o los días se refrescan cada segundo mientras el aviso está abierto.
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable tick = new Runnable() {
            @Override
            public void run() {
                long left = PanelAccount.remainingMs(expires, offset);
                if (trial) big.setText(PanelAccount.formatCountdown(left));
                else big.setText(today ? "HOY" : String.valueOf(days));
                handler.postDelayed(this, 1000);
            }
        };
        dialog.setOnDismissListener(d -> handler.removeCallbacks(tick));
        dialog.setContentView(box);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(PanelUi.dp(a, tv ? 380 : 320), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        tick.run();
        dialog.show();
        close.requestFocus();
    }
}
