package com.ottmty.tube.panel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * Tube OTTMTY: texto de la marca con letra gruesa (Alfa Slab One), relleno en degradado de rojo a
 * negro (más marcado de abajo hacia arriba) y contorno blanco para que el negro no se pierda con el
 * fondo oscuro. Sin sombra. {@link #setWordmark(boolean)} lo apaga para usar el texto normal (por
 * ejemplo en los títulos de subpantallas).
 */
public class TitleTextView extends AppCompatTextView {
    private static final String FONT_ASSET = "fonts/alfa_slab_one.ttf";
    private static final int RED_TOP = 0xFFFF3B30;
    private static final int RED_MID = 0xFFC2171D;
    private static final int BLACK = 0xFF000000;
    private static final float OUTLINE_PER_SIZE = 0.14f;

    private static Typeface sFont;

    private final Typeface mPlainTypeface;
    private final Paint mDraw = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean mWordmark;

    public TitleTextView(Context context) {
        this(context, null);
    }

    public TitleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public TitleTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPlainTypeface = getTypeface();
        setWordmark(true);
    }

    public void setWordmark(boolean wordmark) {
        if (mWordmark == wordmark) {
            return;
        }
        mWordmark = wordmark;
        Typeface font = wordmark ? loadFont(getContext()) : null;
        setTypeface(font != null ? font : mPlainTypeface);
        if (wordmark && font == null) {
            mWordmark = false; // sin la letra incluida, se queda el texto normal
        }
        requestLayout();
        invalidate();
    }

    private static Typeface loadFont(Context c) {
        if (sFont == null) {
            try {
                sFont = Typeface.createFromAsset(c.getApplicationContext().getAssets(), FONT_ASSET);
            } catch (RuntimeException e) {
                return null;
            }
        }
        return sFont;
    }

    private float outline() {
        return getTextSize() * OUTLINE_PER_SIZE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mWordmark && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            int w = getMeasuredWidth() + (int) Math.ceil(outline());
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
                w = Math.min(w, MeasureSpec.getSize(widthMeasureSpec));
            }
            setMeasuredDimension(w, getMeasuredHeight());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mWordmark) {
            super.onDraw(canvas);
            return;
        }
        CharSequence text = getText();
        if (text == null || text.length() == 0) {
            return;
        }
        String s = text.toString();
        Paint p = mDraw;
        p.setTypeface(getTypeface());
        p.setTextSize(getTextSize());
        float textW = p.measureText(s);
        int left = getCompoundPaddingLeft();
        int right = getWidth() - getCompoundPaddingRight();
        int hg = getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
        float x;
        if (hg == Gravity.CENTER_HORIZONTAL) {
            x = left + (right - left - textW) / 2f;
        } else if (hg == Gravity.RIGHT || hg == Gravity.END) {
            x = right - textW;
        } else {
            x = left + outline() / 2f;
        }
        float base = getBaseline();
        Paint.FontMetrics fm = p.getFontMetrics();
        float top = base + fm.ascent;
        float bottom = base + fm.descent;

        // contorno blanco (debajo del relleno)
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(outline());
        p.setColor(0xFFFFFFFF);
        canvas.drawText(s, x, base, p);

        // relleno: degradado de abajo (negro) hacia arriba (rojo), con algo de diagonal
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(x + textW * 0.3f, top, x + textW * 0.7f, bottom,
                new int[]{RED_TOP, RED_MID, BLACK}, new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawText(s, x, base, p);
        p.setShader(null);
    }
}
