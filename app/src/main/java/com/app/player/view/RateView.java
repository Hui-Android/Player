package com.app.player.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.app.player.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 视频分辨率选项
 */
public class RateView extends LinearLayout implements View.OnClickListener {

    //高清、标清、流畅
    private TextView heightRateTv, middleRateTv, lowRateTv;
    //当前视频码率
    private int currentState;
    private RateViewClickListener listener;

    public static final int EMPTY_LINK = 1;
    public static final int HEIGHT_RESOLUTION = 2;
    public static final int MIDDLE_RESOLUTION = 3;
    public static final int LOW_RESOLUTION = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({EMPTY_LINK, HEIGHT_RESOLUTION, MIDDLE_RESOLUTION, LOW_RESOLUTION})
    public @interface ResolutionEnum{

    }

    //视频url
    private String[] videoLink;
    private int[] rateIds = {R.string.ss_video_rate_h, R.string.ss_video_rate_m, R.string.ss_video_rate_l};

    public RateView(Context context) {
        this(context, null);
    }

    public RateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
        initEvent();
    }

    /**
     * 设置当前视频的码率
     */
    public void setVideoRate(@ResolutionEnum int state) {
        currentState = state;
        resetAllRate();
        switch (state) {
            case HEIGHT_RESOLUTION:
                heightRateTv.setSelected(true);
                break;
            case MIDDLE_RESOLUTION:
                middleRateTv.setSelected(true);
                break;
            case LOW_RESOLUTION:
                lowRateTv.setSelected(true);
                break;
            default:
                setVisibility(GONE);
                break;
        }
    }

    private void resetAllRate() {
        heightRateTv.setSelected(false);
        middleRateTv.setSelected(false);
        lowRateTv.setSelected(false);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        int temState;
        switch (id) {
            case R.id.option_rate_h:
                temState = HEIGHT_RESOLUTION;
                break;
            case R.id.option_rate_m:
                temState = MIDDLE_RESOLUTION;
                break;
            case R.id.option_rate_l:
                temState = LOW_RESOLUTION;
                break;
            default:
                temState = currentState;
                break;
        }
        if (temState != currentState && listener != null) {
            currentState = temState;
            setVideoRate(currentState);
            listener.onRateClick(true);
        } else if (listener != null) {
            listener.onRateClick(false);
        }
    }

    private void initView(Context context){
        LinearLayout rootView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.ss_video_change_rate, this);
        heightRateTv = rootView.findViewById(R.id.option_rate_h);
        middleRateTv = rootView.findViewById(R.id.option_rate_m);
        lowRateTv = rootView.findViewById(R.id.option_rate_l);
    }

    private void initEvent(){
        heightRateTv.setOnClickListener(this);
        middleRateTv.setOnClickListener(this);
        lowRateTv.setOnClickListener(this);
    }

    public int getCurrentState() {
        return currentState;
    }

    /**
     * 设置视频分辨路链接
     *
     * @param links 最多三个，链接为空时隐藏对应的分辨率
     */
    public void setRateLink(String[] links) {
        if (links == null || links.length == 0) {
            return;
        }
        videoLink = links;
        if (TextUtils.isEmpty(links[1])) {
            middleRateTv.setVisibility(View.GONE);
        } else {
            middleRateTv.setVisibility(View.VISIBLE);
        }
        if (TextUtils.isEmpty(links[0])) {
            heightRateTv.setVisibility(View.GONE);
        } else {
            heightRateTv.setVisibility(View.VISIBLE);
        }
        if (TextUtils.isEmpty(links[2])) {
            lowRateTv.setVisibility(View.GONE);
        } else {
            lowRateTv.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 获取当前码率名称
     */
    public int getCurrentRateNameId() {
        switch (currentState) {
            case HEIGHT_RESOLUTION:
                return rateIds[0];
            case MIDDLE_RESOLUTION:
                return rateIds[1];
            case LOW_RESOLUTION:
                return rateIds[2];
        }
        return rateIds[1];
    }

    // 获取当前的播放地址
    public String getCurrentVideoUrl() {
        switch (currentState) {
            case EMPTY_LINK:
                return "";
            case HEIGHT_RESOLUTION:
                return videoLink[0];
            case MIDDLE_RESOLUTION:
                return videoLink[1];
            case LOW_RESOLUTION:
                return videoLink[2];
        }
        return "";
    }

    interface RateViewClickListener{
        void onRateClick(boolean isChangeRate);
    }

    public void setListener(RateViewClickListener listener){
        this.listener = listener;
    }
}
