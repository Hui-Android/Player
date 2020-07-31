package com.app.player.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.app.player.R;

public class LoadingRingLayout extends LinearLayout {

    protected LoadingRingView ldrvLoading;
    protected TextView tvTips;

    public LoadingRingLayout(Context context) {
        this(context, null);
    }

    public LoadingRingLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingRingLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    protected void init(Context context) {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        View root = LayoutInflater.from(context).inflate(R.layout.loading_ring_layout, this);
        ldrvLoading = root.findViewById(R.id.ldrv_loading);
        tvTips = root.findViewById(R.id.tv_loading_ring);
    }

    public void setTips(CharSequence tips) {
        tvTips.setText(tips);
    }
    public void setTipsColor(int color) {
        tvTips.setTextColor(color);
    }

    public void setTipsTextSize(float size) {
        tvTips.setTextSize(size);
    }
    public void setLoadingBackgroudColor(int loadingBackgroudColor) {
        ldrvLoading.setLoadingBackgroudColor(loadingBackgroudColor);
    }

    public void setLoadingForegroudColor(int loadingForegroudColor) {
        ldrvLoading.setLoadingForegroudColor(loadingForegroudColor);
    }
    public void setLoadingDrawable(int resId) {
        ldrvLoading.setLoadingDrawable(resId);
    }


    @Override
    public void setVisibility(int visibility) {
        switch (visibility) {
            case VISIBLE:
                ldrvLoading.setVisibility(VISIBLE);
                break;
            case GONE:
                ldrvLoading.setVisibility(GONE);
                break;
            case INVISIBLE:
                ldrvLoading.setVisibility(INVISIBLE);
                break;
        }
        super.setVisibility(visibility);
    }
}
