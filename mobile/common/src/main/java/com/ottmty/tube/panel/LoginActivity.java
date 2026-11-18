package com.ottmty.tube.panel;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Pantalla de inicio de sesión (tema oscuro tipo YouTube) ligada al panel ibo.
 * Sin sesión, la app no pasa de aquí.
 *
 * Diseño "Brasas y brillo": resplandor rojo y brasas que suben de fondo, la marca con un punto que
 * la orbita, destello que cruza el nombre, campos con icono y borde rojo al enfocar, entrada
 * escalonada y sacudida del formulario cuando hay un error. Los efectos viven en {@link LoginFx}.
 */
public class LoginActivity extends Activity {
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private EditText mUser;
    private EditText mPass;
    private Button mButton;
    private TextView mError;
    private ProgressBar mProgress;
    private boolean mBusy;
    private TitleTextView mTitle;
    private LinearLayout mCol;
    private static final long SHINE_FIRST_MS = 1400;
    private static final long SHINE_EVERY_MS = 5000;
    private final Runnable mShineLoop = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || mTitle == null) return;
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setDuration(1500);
            a.addUpdateListener(an -> mTitle.setShine((float) an.getAnimatedValue()));
            a.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    if (mTitle != null) mTitle.setShine(-1f);
                }
            });
            a.start();
            mMain.postDelayed(this, SHINE_EVERY_MS);
        }
    };

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

        FrameLayout root = new FrameLayout(c);
        root.setBackgroundColor(0xFF0A0A0A);
        root.addView(new LoginFx.EmberView(c), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(c);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setFillViewport(true);
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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
        mCol = col;

        LoginFx.OrbitFrame mark = new LoginFx.OrbitFrame(c, PanelUi.dp(c, tv ? 96 : 84));
        int frame = mark.frameSize();
        col.addView(mark, new LinearLayout.LayoutParams(frame, frame));

        TitleTextView title = new TitleTextView(c);
        mTitle = title;
        title.setText(PanelConfig.APP_NAME);
        title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, tv ? 34 : 32);
        title.setTextColor(PanelUi.WHITE);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = PanelUi.dp(c, 4);
        col.addView(title, titleLp);

        TextView subtitle = PanelUi.text(c, "Inicia sesión con tu cuenta", 15, PanelUi.GRAY, false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = PanelUi.dp(c, 4);
        subLp.bottomMargin = PanelUi.dp(c, 24);
        col.addView(subtitle, subLp);

        mUser = field(c, "Usuario", false, LoginFx.GlyphDrawable.USER);
        mPass = field(c, "Contraseña", true, LoginFx.GlyphDrawable.LOCK);
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

        setContentView(root);
        mUser.requestFocus();
        animateEntry(mark, title, subtitle, mUser, mPass, mButton, footer);
    }

    /** Entrada escalonada: la marca crece con rebote y lo demás sube con desvanecido. */
    private void animateEntry(View... views) {
        float rise = PanelUi.dp(this, 28);
        for (int i = 0; i < views.length; i++) {
            View v = views[i];
            v.setAlpha(0f);
            if (i == 0) {
                v.setScaleX(0.5f);
                v.setScaleY(0.5f);
                v.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(60).setDuration(650)
                        .setInterpolator(new OvershootInterpolator(1.6f)).start();
            } else {
                v.setTranslationY(rise);
                v.animate().alpha(1f).translationY(0f).setStartDelay(160L + i * 90L).setDuration(520)
                        .setInterpolator(new DecelerateInterpolator(2f)).start();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTitle != null) {
            mMain.removeCallbacks(mShineLoop);
            mMain.postDelayed(mShineLoop, SHINE_FIRST_MS);
        }
    }

    @Override
    protected void onPause() {
        mMain.removeCallbacks(mShineLoop);
        super.onPause();
    }

    private EditText field(Context c, String hint, boolean password, int glyphKind) {
        final EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(PanelUi.GRAY);
        e.setTextColor(PanelUi.WHITE);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setBackground(LoginFx.fieldBackground(c));
        int h = PanelUi.dp(c, 16);
        e.setPadding(h, PanelUi.dp(c, 16), h, PanelUi.dp(c, 16));
        // icono a la izquierda: gris, y rojo mientras el campo tiene el foco
        final LoginFx.GlyphDrawable glyph = new LoginFx.GlyphDrawable(glyphKind, PanelUi.dp(c, 22), PanelUi.GRAY);
        e.setCompoundDrawablesRelativeWithIntrinsicBounds(glyph, null, null, null);
        e.setCompoundDrawablePadding(PanelUi.dp(c, 14));
        e.setOnFocusChangeListener((v, hasFocus) -> glyph.setColor(hasFocus ? PanelUi.RED : PanelUi.GRAY));
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
        shake();
    }

    /** Sacudida corta del formulario: el aviso de que algo no salió bien. */
    private void shake() {
        if (mCol == null) return;
        float d = PanelUi.dp(this, 10);
        ObjectAnimator.ofFloat(mCol, View.TRANSLATION_X, 0f, -d, d, -d * 0.8f, d * 0.8f, -d * 0.4f, 0f)
                .setDuration(380).start();
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
