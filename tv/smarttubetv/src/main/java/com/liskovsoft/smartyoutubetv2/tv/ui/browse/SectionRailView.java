package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.PageRow;

import com.bumptech.glide.Glide;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * Tube OTTMTY: riel de iconos que queda a la izquierda cuando el menú está oculto. Solo muestra
 * las secciones y cuál está activa; no toma el foco (con izquierda en el contenido se despliega el menú).
 */
public class SectionRailView extends FrameLayout {
    private static final float UNSELECTED_ALPHA = 0.7f;

    private final LinearLayout mColumn;
    private final int mItemHeight;
    private final int mIconSize;
    private final int mGap;
    private int mSelected = -1;

    public SectionRailView(Context context) {
        super(context);

        float d = context.getResources().getDisplayMetrics().density;
        mItemHeight = (int) (46 * d);
        mIconSize = (int) (26 * d);
        mGap = (int) (18 * d);

        setFocusable(false);
        setClickable(false);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setClipChildren(true);

        mColumn = new LinearLayout(context);
        mColumn.setOrientation(LinearLayout.VERTICAL);
        mColumn.setGravity(Gravity.CENTER_HORIZONTAL);
        mColumn.setPadding(0, (int) (20 * d), 0, (int) (20 * d));
        addView(mColumn, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    /** Rebuilds the icons from the sections list and keeps the selection. */
    public void setSections(ObjectAdapter adapter, int selected) {
        mColumn.removeAllViews();

        for (int i = 0; i < adapter.size(); i++) {
            Object row = adapter.get(i);
            if (!(row instanceof PageRow)) {
                continue;
            }
            HeaderItem header = ((PageRow) row).getHeaderItem();
            int resId = 0;
            String iconUrl = null;
            if (header instanceof SectionHeaderItem) {
                resId = ((SectionHeaderItem) header).getResId();
                iconUrl = ((SectionHeaderItem) header).getIconUrl();
            }

            FrameLayout cell = new FrameLayout(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    (int) (getResources().getDisplayMetrics().density * 52), mItemHeight);
            if (header.getId() == MediaGroup.TYPE_SETTINGS) {
                lp.topMargin = mGap; // Ajustes va aparte, como en el menú desplegado
            }
            cell.setTag(i);

            ImageView icon = new ImageView(getContext());
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            cell.addView(icon, new LayoutParams(mIconSize, mIconSize, Gravity.CENTER));

            if (iconUrl != null) {
                Glide.with(getContext()).load(iconUrl).apply(ViewUtil.glideOptions()).into(icon);
            } else if (resId > 0) {
                icon.setImageDrawable(ContextCompat.getDrawable(getContext(), resId));
            } else {
                icon.setImageDrawable(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.lb_grey)));
            }

            mColumn.addView(cell, lp);
        }

        setSelected(selected);
    }

    public void setSelected(int index) {
        mSelected = index;

        for (int i = 0; i < mColumn.getChildCount(); i++) {
            FrameLayout cell = (FrameLayout) mColumn.getChildAt(i);
            boolean on = ((Integer) cell.getTag()) == index;
            cell.setAlpha(on ? 1f : UNSELECTED_ALPHA);
            if (on) {
                GradientDrawable pill = new GradientDrawable();
                pill.setColor(Color.parseColor("#33FFFFFF"));
                pill.setCornerRadius(mItemHeight / 2f);
                cell.setBackground(pill);
            } else {
                cell.setBackground(null);
            }
        }

        post(this::keepSelectedVisible);
    }

    /** More icons than screen height: slide the column so the active one stays in view. */
    private void keepSelectedVisible() {
        int h = getHeight();
        int colH = mColumn.getHeight();

        if (h <= 0 || colH <= h) {
            mColumn.setTranslationY(0);
            return;
        }

        View selected = null;
        for (int i = 0; i < mColumn.getChildCount(); i++) {
            if (((Integer) mColumn.getChildAt(i).getTag()) == mSelected) {
                selected = mColumn.getChildAt(i);
            }
        }

        if (selected == null) {
            mColumn.setTranslationY(0);
            return;
        }

        float center = selected.getTop() + selected.getHeight() / 2f;
        float ty = h / 2f - center;
        mColumn.setTranslationY(Math.max(h - colH, Math.min(0f, ty)));
    }
}
