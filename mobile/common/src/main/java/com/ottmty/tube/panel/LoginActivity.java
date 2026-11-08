package com.ottmty.tube.panel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Pantalla de inicio de sesión (tema oscuro tipo YouTube) ligada al panel ibo.
 * Sin sesión, la app no pasa de aquí.
 */
public class LoginActivity extends Activity {
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private EditText mUser;
    private EditText mPass;
    private Button mButton;
    private TextView mError;
    private ProgressBar mProgress;
    private boolean mBusy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PanelSession.isLoggedIn(this)) {
            openApp();
            return;
        }
        buildUi();
        String notice = PanelSession.takeNotice();
        if (!notice.isEmpty()) showError(notice);
    }

    private void buildUi() {
        final boolean tv = PanelConfig.IS_TV;
        final Context c = this;

        ScrollView scroll = new ScrollView(c);
        scroll.setBackgroundColor(PanelUi.BG);
        scroll.setFillViewport(true);

        LinearLayout outer = new LinearLayout(c);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        int pad = PanelUi.dp(c, tv ? 32 : 24);
        outer.setPadding(pad, pad, pad, pad);
        scroll.addView(outer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        outer.addView(col, new LinearLayout.LayoutParams(
                tv ? PanelUi.dp(c, 460) : ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        PanelUi.MarkView mark = new PanelUi.MarkView(c);
        int markSize = PanelUi.dp(c, tv ? 96 : 80);
        col.addView(mark, new LinearLayout.LayoutParams(markSize, markSize));

        TextView title = PanelUi.text(c, PanelConfig.APP_NAME, tv ? 32 : 28, PanelUi.WHITE, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = PanelUi.dp(c, 12);
        col.addView(title, titleLp);

        TextView subtitle = PanelUi.text(c, "Inicia sesión con tu cuenta", 15, PanelUi.GRAY, false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = PanelUi.dp(c, 4);
        subLp.bottomMargin = PanelUi.dp(c, 24);
        col.addView(subtitle, subLp);

        mUser = field(c, "Usuario", false);
        mPass = field(c, "Contraseña", true);
        mUser.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mPass.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mPass.setOnEditorActionListener((v, actionId, event) -> {
            submit();
            return true;
        });
        col.addView(mUser, fieldParams(c, 0));
        col.addView(mPass, fieldParams(c, 12));

        mError = PanelUi.text(c, "", 14, PanelUi.ERROR, false);
        mError.setGravity(Gravity.CENTER);
        mError.setVisibility(View.GONE);
        LinearLayout.LayoutParams errLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errLp.topMargin = PanelUi.dp(c, 14);
        col.addView(mError, errLp);

        mButton = PanelUi.button(c, "Entrar", true);
        mButton.setOnClickListener(v -> submit());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(c, 52));
        btnLp.topMargin = PanelUi.dp(c, 20);
        col.addView(mButton, btnLp);

        mProgress = new ProgressBar(c);
        mProgress.getIndeterminateDrawable().setColorFilter(PanelUi.RED, PorterDuff.Mode.SRC_IN);
        mProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progLp = new LinearLayout.LayoutParams(PanelUi.dp(c, 32), PanelUi.dp(c, 32));
        progLp.topMargin = PanelUi.dp(c, 16);
        col.addView(mProgress, progLp);

        TextView footer = PanelUi.text(c, "Usa el usuario y la contraseña que te dieron.", 13, PanelUi.GRAY, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footLp.topMargin = PanelUi.dp(c, 22);
        col.addView(footer, footLp);

        setContentView(scroll);
        mUser.requestFocus();
    }

    private EditText field(Context c, String hint, boolean password) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(PanelUi.GRAY);
        e.setTextColor(PanelUi.WHITE);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setBackground(PanelUi.fieldBackground(c));
        int h = PanelUi.dp(c, 16);
        e.setPadding(h, PanelUi.dp(c, 14), h, PanelUi.dp(c, 14));
        e.setInputType(password
                ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS));
        e.setFocusable(true);
        return e;
    }

    private LinearLayout.LayoutParams fieldParams(Context c, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = PanelUi.dp(c, topDp);
        return lp;
    }

    private void submit() {
        if (mBusy) return;
        final String user = mUser.getText().toString().trim();
        final String pass = mPass.getText().toString();
        if (user.isEmpty() || pass.isEmpty()) {
            showError("Ingresa tu usuario y tu contraseña.");
            return;
        }
        hideKeyboard();
        setBusy(true);
        new Thread(() -> {
            final PanelLogin.Result result = PanelLogin.login(user, pass, PanelSession.getDeviceId(this), PanelSession.getDeviceLabel());
            mMain.post(() -> onResult(user, pass, result));
        }).start();
    }

    private void onResult(String user, String pass, PanelLogin.Result r) {
        if (isFinishing()) return;
        setBusy(false);
        switch (r.status) {
            case SUCCESS:
                PanelSession.save(this, user, pass, r);
                openApp();
                break;
            case EXPIRED:
                showError("Tu cuenta venció. Renuévala para entrar.");
                break;
            case DEVICE_LIMIT:
                showError("Esta cuenta ya tiene sus " + (r.deviceMax > 0 ? r.deviceMax : 4)
                        + " dispositivos registrados. Cierra sesión en otro equipo o pide que lo quiten.");
                break;
            case INVALID:
                showError("Usuario o contraseña incorrectos.");
                break;
            default:
                showError("No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.");
                break;
        }
    }

    private void setBusy(boolean busy) {
        mBusy = busy;
        mButton.setEnabled(!busy);
        mButton.setText(busy ? "Verificando…" : "Entrar");
        mProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (busy) mError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        mError.setText(message);
        mError.setVisibility(View.VISIBLE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (imm != null && focus != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
    }

    private void openApp() {
        Intent i = new Intent();
        i.setClassName(getPackageName(), PanelConfig.SPLASH_CLASS);
        startActivity(i);
        finish();
    }
}
