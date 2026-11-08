package com.ottmty.tube.panel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/** Piezas de interfaz del login y de la cuenta: tema oscuro con acento rojo, hechas por código. */
final class PanelUi {
    static final int BG = 0xFF0F0F0F;
    static final int CARD = 0xFF1C1C1E;
    static final int SURFACE = 0xFF272727;
    static final int RED = 0xFFE53935;
    static final int RED_DARK = 0xFFC4161C;
    static final int WHITE = 0xFFFFFFFF;
    static final int GRAY = 0xFFAAAAAA;
    static final int ERROR = 0xFFFF6B6B;

    private PanelUi() {
    }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static GradientDrawable rounded(Context c, int color, float radiusDp, int strokeColor, float strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) d.setStroke(dp(c, strokeDp), strokeColor);
        return d;
    }

    /** Campo de texto: gris oscuro; con el foco (control remoto) se marca con un borde blanco. */
    static Drawable fieldBackground(Context c) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, rounded(c, SURFACE, 12, WHITE, 2));
        s.addState(new int[]{}, rounded(c, SURFACE, 12, Color.TRANSPARENT, 0));
        return s;
    }

    /** Botón principal rojo. */
    static Drawable buttonBackground(Context c) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{-android.R.attr.state_enabled}, rounded(c, 0xFF5A2A2A, 26, Color.TRANSPARENT, 0));
        s.addState(new int[]{android.R.attr.state_pressed}, rounded(c, RED_DARK, 26, Color.TRANSPARENT, 0));
        s.addState(new int[]{android.R.attr.state_focused}, rounded(c, RED, 26, WHITE, 3));
        s.addState(new int[]{}, rounded(c, RED, 26, Color.TRANSPARENT, 0));
        return s;
    }

    /** Botón secundario (contorno gris). */
    static Drawable secondaryBackground(Context c) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, rounded(c, SURFACE, 26, GRAY, 1));
        s.addState(new int[]{android.R.attr.state_focused}, rounded(c, SURFACE, 26, WHITE, 3));
        s.addState(new int[]{}, rounded(c, Color.TRANSPARENT, 26, GRAY, 1));
        return s;
    }

    static TextView text(Context c, String value, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    static Button button(Context c, String label, boolean primary) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setTextColor(WHITE);
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        b.setBackground(primary ? buttonBackground(c) : secondaryBackground(c));
        b.setMinHeight(dp(c, 52));
        b.setMinimumHeight(dp(c, 52));
        b.setFocusable(true);
        return b;
    }

    /** Marca de Tube OTTMTY: anillo, disco rojo y triángulo de reproducción. */
    static final class MarkView extends View {
        private final Paint mRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mDisc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mTriFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mTriStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mTriangle = new Path();

        MarkView(Context c) {
            this(c, null);
        }

        MarkView(Context c, AttributeSet a) {
            super(c, a);
            mRing.setStyle(Paint.Style.STROKE);
            mRing.setStrokeWidth(2.4f);
            mRing.setColor(WHITE);
            mDisc.setStyle(Paint.Style.FILL);
            mDisc.setColor(RED);
            mTriFill.setStyle(Paint.Style.FILL);
            mTriFill.setColor(WHITE);
            mTriStroke.setStyle(Paint.Style.STROKE);
            mTriStroke.setStrokeWidth(3.4f);
            mTriStroke.setStrokeJoin(Paint.Join.ROUND);
            mTriStroke.setColor(WHITE);
            mTriangle.moveTo(47.5f, 42.5f);
            mTriangle.lineTo(47.5f, 65.5f);
            mTriangle.lineTo(67f, 54f);
            mTriangle.close();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float s = Math.min(getWidth(), getHeight()) / 108f;
            canvas.save();
            canvas.translate((getWidth() - 108f * s) / 2f, (getHeight() - 108f * s) / 2f);
            canvas.scale(s, s);
            canvas.drawCircle(54f, 54f, 32f, mRing);
            canvas.drawCircle(54f, 54f, 25.5f, mDisc);
            canvas.drawPath(mTriangle, mTriFill);
            canvas.drawPath(mTriangle, mTriStroke);
            canvas.restore();
        }
    }
}
