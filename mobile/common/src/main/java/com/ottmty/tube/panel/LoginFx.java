package com.ottmty.tube.panel;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import java.util.Random;

/**
 * Efectos del login "Brasas y brillo": fondo con resplandor rojo y brasas que suben, marca con un
 * punto que la orbita, y campos con icono y borde rojo al enfocar. Todo por código, sin recursos.
 * Las animaciones corren solo mientras la ventana está visible.
 */
final class LoginFx {
    /** Una vuelta completa de las animaciones de fondo; todas son periódicas en este tiempo. */
    private static final long LOOP_MS = 5000;

    private LoginFx() {
    }

    /** Fondo del campo: oscuro con borde gris; con el foco, borde rojo. */
    static Drawable fieldBackground(Context c) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, PanelUi.rounded(c, 0xFF161616, 22, PanelUi.RED, 2));
        s.addState(new int[]{}, PanelUi.rounded(c, 0xFF161616, 22, 0xFF4E4E4E, 1.5f));
        return s;
    }

    /** Reloj común: un animador lineal de 0 a 1 que se repite, atado a la visibilidad de la vista. */
    private abstract static class LoopView extends View {
        private ValueAnimator mAnimator;
        float mT;

        LoopView(Context c) {
            super(c);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            startLoop();
        }

        @Override
        protected void onDetachedFromWindow() {
            stopLoop();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            if (visibility == VISIBLE) {
                startLoop();
            } else {
                stopLoop();
            }
        }

        private void startLoop() {
            if (mAnimator != null) {
                return;
            }
            mAnimator = ValueAnimator.ofFloat(0f, 1f);
            mAnimator.setDuration(LOOP_MS);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mAnimator.addUpdateListener(a -> {
                mT = (float) a.getAnimatedValue();
                invalidate();
            });
            mAnimator.start();
        }

        private void stopLoop() {
            if (mAnimator != null) {
                mAnimator.cancel();
                mAnimator = null;
            }
        }
    }

    /** Fondo: resplandor rojo que respira arriba y brasas que suben con un vaivén suave. */
    static final class EmberView extends LoopView {
        private static final int COUNT = 54;
        private final float[] mX = new float[COUNT];
        private final float[] mY = new float[COUNT];
        private final float[] mPhase = new float[COUNT];
        private final float[] mSway = new float[COUNT];
        private final float[] mRad = new float[COUNT];
        private final boolean[] mWarm = new boolean[COUNT];
        private final Paint mGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mDot = new Paint(Paint.ANTI_ALIAS_FLAG);

        EmberView(Context c) {
            super(c);
            Random r = new Random(11);
            for (int i = 0; i < COUNT; i++) {
                mX[i] = r.nextFloat();
                mY[i] = 0.38f + r.nextFloat() * 0.60f;
                mPhase[i] = r.nextFloat();
                mSway[i] = r.nextFloat() * 6.28f;
                mRad[i] = 3f + r.nextFloat() * 5f;
                mWarm[i] = r.nextFloat() < 0.6f;
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mGlow.setShader(new RadialGradient(w * 0.5f, h * 0.22f, Math.max(w, 1) * 0.85f,
                    new int[]{0xFFD22823, 0x00D22823}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) {
                return;
            }
            // resplandor: respira una vez por vuelta
            double phi = 2 * Math.PI * mT;
            mGlow.setAlpha((int) (255 * (0.40 + 0.07 * Math.sin(phi))));
            canvas.drawRect(0, 0, w, h, mGlow);

            float scale = w / 1080f;
            for (int i = 0; i < COUNT; i++) {
                float u = (mT + mPhase[i]) % 1f;
                float a = (float) Math.pow(Math.sin(Math.PI * u), 1.3);
                float x = mX[i] * w + 34f * scale * (float) Math.sin(2 * Math.PI * u + mSway[i]);
                float y = mY[i] * h - 0.32f * h * u;
                float rad = mRad[i] * scale * 1.6f * (1f - 0.4f * u);
                int rgb = mWarm[i] ? 0xFF5A2D : 0xFFA555;
                mDot.setColor((rgb) | 0xFF000000);
                mDot.setAlpha((int) (255 * a * 0.22f));
                canvas.drawCircle(x, y, rad * 3f, mDot); // halo
                mDot.setAlpha((int) (255 * a));
                canvas.drawCircle(x, y, rad, mDot); // brasa
            }
        }
    }

    /** La marca (aro + disco + play) con un punto blanco, con estela, que la orbita. */
    static final class OrbitFrame extends FrameLayout {
        private final int mMark;
        private final float mRing;
        private final Paint mDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ValueAnimator mAnimator;
        private float mT;

        OrbitFrame(Context c, int markPx) {
            super(c);
            mMark = markPx;
            mRing = 32f / 108f * markPx;
            setWillNotDraw(false);
            setClipChildren(false);
            PanelUi.MarkView mark = new PanelUi.MarkView(c);
            LayoutParams lp = new LayoutParams(markPx, markPx);
            lp.gravity = android.view.Gravity.CENTER;
            addView(mark, lp);
        }

        int frameSize() {
            return Math.round(mMark * 1.5f);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int s = frameSize();
            super.onMeasure(MeasureSpec.makeMeasureSpec(s, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(s, MeasureSpec.EXACTLY));
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            start();
        }

        @Override
        protected void onDetachedFromWindow() {
            stop();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            if (visibility == VISIBLE) {
                start();
            } else {
                stop();
            }
        }

        private void start() {
            if (mAnimator != null) {
                return;
            }
            mAnimator = ValueAnimator.ofFloat(0f, 1f);
            mAnimator.setDuration(LOOP_MS);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mAnimator.addUpdateListener(a -> {
                mT = (float) a.getAnimatedValue();
                invalidate();
            });
            mAnimator.start();
        }

        private void stop() {
            if (mAnimator != null) {
                mAnimator.cancel();
                mAnimator = null;
            }
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float orbit = mRing + mMark * 0.11f;
            float dotR = mMark * 0.04f;
            for (int j = 9; j >= 0; j--) {
                double ang = 2 * Math.PI * 2 * mT - Math.PI / 2 - j * 0.10;
                float x = cx + orbit * (float) Math.cos(ang);
                float y = cy + orbit * (float) Math.sin(ang);
                mDot.setColor(j == 0 ? Color.WHITE : 0xFFFF7864);
                mDot.setAlpha((int) (255 * (1f - j * 0.1f) * (j == 0 ? 1f : 0.6f)));
                canvas.drawCircle(x, y, dotR * (1f - j * 0.08f), mDot);
            }
        }
    }

    /** Iconos de los campos: persona y candado, dibujados por código y teñibles. */
    static final class GlyphDrawable extends Drawable {
        static final int USER = 0;
        static final int LOCK = 1;
        private final int mKind;
        private final int mSize;
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF mRect = new RectF();

        GlyphDrawable(int kind, int sizePx, int color) {
            mKind = kind;
            mSize = sizePx;
            mPaint.setColor(color);
        }

        void setColor(int color) {
            mPaint.setColor(color);
            invalidateSelf();
        }

        @Override
        public int getIntrinsicWidth() {
            return mSize;
        }

        @Override
        public int getIntrinsicHeight() {
            return mSize;
        }

        @Override
        public void draw(Canvas canvas) {
            float s = mSize;
            float cx = s / 2f;
            float cy = s / 2f;
            if (mKind == USER) {
                mPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy - 0.20f * s, 0.20f * s, mPaint);
                mRect.set(cx - 0.42f * s, cy + 0.02f * s, cx + 0.42f * s, cy + 0.92f * s);
                canvas.drawArc(mRect, 180, 180, true, mPaint);
            } else {
                mPaint.setStyle(Paint.Style.FILL);
                mRect.set(cx - 0.36f * s, cy - 0.04f * s, cx + 0.36f * s, cy + 0.44f * s);
                canvas.drawRoundRect(mRect, 0.10f * s, 0.10f * s, mPaint);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(0.10f * s);
                mRect.set(cx - 0.22f * s, cy - 0.46f * s, cx + 0.22f * s, cy + 0.02f * s);
                canvas.drawArc(mRect, 180, 180, false, mPaint);
                canvas.drawLine(cx - 0.22f * s, cy - 0.22f * s, cx - 0.22f * s, cy - 0.02f * s, mPaint);
                canvas.drawLine(cx + 0.22f * s, cy - 0.22f * s, cx + 0.22f * s, cy - 0.02f * s, mPaint);
                mPaint.setStyle(Paint.Style.FILL);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
