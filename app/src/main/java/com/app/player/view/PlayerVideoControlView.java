package com.app.player.view;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.IntDef;

import com.app.player.R;
import com.app.player.VolumeControlPresenter;
import com.app.player.entity.VideoModel;
import com.app.player.util.DurationUtil;
import com.app.player.util.LogUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

public class PlayerVideoControlView extends RelativeLayout implements View.OnClickListener,
        View.OnTouchListener,RateView.RateViewClickListener, VolumeControlPresenter.VideoVolumeListener,
        VideoPlayerRightOptionView.VideoPlayerRightOptionViewClickListener{

    private static final String TAG = "PlayerVideoControlView";
    // 开始加载
    public static final int VIDEO_START_LOADING = 1;
    // 加载完成
    public static final int VIDEO_LOADING_SUCCESS = 2;
    // 重新加载
    public static final int VIDEO_RELOAD = 3;
    // 开始播放
    public static final int VIDEO_START = 4;
    // 暂停播放
    public static final int VIDEO_PAUSE = 5;
    // 更改音量
    public static final int VIDEO_VOLUME_CHANGE = 6;
    // 更改分辨率
    public static final int VIDEO_SHOW_RAGE_SELECTED = 7;
    // 显示/隐藏
    public static final int VIDEO_CONTROL_VISIBLE = 8;
    // 正在切换分辨率
    public static final int VIDEO_RATE_CHANGING = 9;
    // 视频切换分辨率成功
    public static final int VIDEO_RATE_CHANGE_SUCCESS = 10;
    // seek
    public static final int VIDEO_SEEKING = 11;
    // seek success
    public static final int VIDEO_SEEKING_SUCCESS = 12;
    // 视频丢失
    public static final int VIDEO_LOST = 13;
    // 视频播放错误（情况：1，有本地文件，但本地播放器加码失败，使用但网络播放链接置空；）
    public static final int VIDEO_DECODE_FAILED = 14;

    public static final int VIDEO_NET_INVALID = 15;
    // 正在缓冲
    public static final int VIDEO_BUFFERING_START = 16;

    private int currentState = VIDEO_START_LOADING;

    // 左边边屏幕滑动亮度显示
    private float startX = 0;//手指按下时的X坐标
    private float startY = 0;//手指按下时的Y坐标

    @IntDef({VIDEO_START_LOADING,
            VIDEO_LOADING_SUCCESS,
            VIDEO_RELOAD,
            VIDEO_START,
            VIDEO_PAUSE,
            VIDEO_VOLUME_CHANGE,
            VIDEO_SHOW_RAGE_SELECTED,
            VIDEO_CONTROL_VISIBLE,
            VIDEO_RATE_CHANGING,
            VIDEO_RATE_CHANGE_SUCCESS,
            VIDEO_SEEKING,
            VIDEO_SEEKING_SUCCESS,
            VIDEO_LOST,
            VIDEO_DECODE_FAILED,
            VIDEO_NET_INVALID,
            VIDEO_BUFFERING_START})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerVideoControlInterface {
    }

    private RelativeLayout rootLayout;
    // title 栏
    private LinearLayout titleLayout;
    private TextView videoTitleTv;
    private ImageView goBackIv;
    // 重新加载
    private RelativeLayout reloadLayout;
    private TextView reloadTipTv;
    private LinearLayout reloadBtnLayout;
    // 加载
    private RelativeLayout loadingLayout;
    // 底部控件栏
    private RelativeLayout bottomLayout;
    private ImageView playStateIv;
    private ImageView soundStateIv;
    private TextView changeRateTv;
    private SeekBar playScheduleSb;
    private TextView playTimeTv;
    private TextView videoDurationTv;
    // 分辨率选择
    private RateView rateView;
    // 右侧功能控件
    private VideoPlayerRightOptionView rightOptionView;
    // 音量增减
    private RelativeLayout volumeSeekBarLayout;
    private VolumeSeekBar soundSeekBar;
    // 暂停
    private ImageView centerPlayStateIv;
    // 底部提示
    private TextView bottomTipTv;
    // true : 播放；false：暂停
    private boolean currentPlayState = true;
    // 当前播放时间点
    private int currentVideoTimestamp = 0;
    // 音量调节实现累
    private VolumeControlPresenter volumeControlPresenter;
    // 控件点击回调接口
    private OnPlayerViewControlListener controlListener;
    private MediaPlayer mediaPlayer;

    private Uri currentVideoUrl;

    public PlayerVideoControlView(Context context) {
        this(context, null);
    }

    public PlayerVideoControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerVideoControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
        initData();
    }

    private void initData() {
        volumeControlPresenter = new VolumeControlPresenter(getContext(), soundSeekBar);
        volumeControlPresenter.setVolumeListener(this);
        onVolumeChange(volumeControlPresenter.getCurrentVolume());
    }

    private void initView() {
        View rootView = LayoutInflater.from(getContext())
                .inflate(R.layout.player_activity_control_layout, this);

        rootLayout = rootView.findViewById(R.id.root_layout);
        rootLayout.setOnClickListener(this);
        rootLayout.setOnTouchListener(this);

        titleLayout = rootView.findViewById(R.id.title_layout);
        videoTitleTv = titleLayout.findViewById(R.id.video_title_tv);
        videoTitleTv.setText("test_video");

        goBackIv = titleLayout.findViewById(R.id.go_back_iv);
        goBackIv.setOnClickListener(this);

        reloadLayout = rootView.findViewById(R.id.reload_layout);
        reloadTipTv = reloadLayout.findViewById(R.id.reload_tips);
        reloadBtnLayout = reloadLayout.findViewById(R.id.reload_btn);
        reloadBtnLayout.setOnClickListener(this);

        loadingLayout = rootView.findViewById(R.id.player_loading_layout);
        LoadingRingLayout loadingTipTv = loadingLayout.findViewById(R.id.loading_layout);
        loadingTipTv.setTips(getResources().getString(R.string.video_loading_buffer));

        bottomLayout = rootView.findViewById(R.id.layout_bottom);
        playStateIv = bottomLayout.findViewById(R.id.bottom_img_playctrl);
        playStateIv.setOnClickListener(this);
        soundStateIv = bottomLayout.findViewById(R.id.bottom_img_sound);
        soundStateIv.setOnClickListener(this);
        changeRateTv = bottomLayout.findViewById(R.id.bottom_rate);
        changeRateTv.setOnClickListener(this);
        playScheduleSb = bottomLayout.findViewById(R.id.bottom_seekbar_video);
        playTimeTv = bottomLayout.findViewById(R.id.bottom_tv_videoprogress);
        videoDurationTv = bottomLayout.findViewById(R.id.bottom_tv_video_duration);

        rateView = rootView.findViewById(R.id.rate_view);
        rateView.setListener(this);

        rightOptionView = rootView.findViewById(R.id.right_option_view);
        rightOptionView.setClickListener(this);

        volumeSeekBarLayout = rootView.findViewById(R.id.layout_adjust_sound);
        soundSeekBar = volumeSeekBarLayout.findViewById(R.id.adjust_sound_seekbar);

        centerPlayStateIv = rootView.findViewById(R.id.center_play);
        centerPlayStateIv.setOnClickListener(this);

        bottomTipTv = rootView.findViewById(R.id.bottom_tv_buffering);
        initSeekBarListener();

    }

    private void initSeekBarListener() {
        playScheduleSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 更新当前播放的时间点
                currentVideoTimestamp = progress;
                playTimeTv.setText(DurationUtil.durationToStrMs(progress, false));
                // 主要处理用户 seek 的事件
//                if (fromUser && controlListener != null) {
//                    changeVideoControlState(VIDEO_SEEKING);
//                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 主要处理用户 seek 的事件
                if (controlListener != null) {
                    changeVideoControlState(VIDEO_SEEKING);
                }

                // 控制视频跳转到目标位置
//                int progress = seekBar.getProgress();
//                int position = progress * mediaPlayer.getDuration() / 100;
//                mediaPlayer.seekTo(position);
            }
        });
    }

    public void setTitleForNavite(String title){
        videoTitleTv.setText(title);
    }

    public void setVideoPath(String path){
        setVideoUri(Uri.parse(path));
    }

    public void setVideoUri(Uri uri){
        this.currentVideoUrl = uri;
    }

    public Uri getVideoUrl(){
        return currentVideoUrl;
    }

    private Handler seekBarUpdateHandler = new Handler();
    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (mediaPlayer != null) {
                    playScheduleSb.setProgress(mediaPlayer.getCurrentPosition());
                    seekBarUpdateHandler.postDelayed(this, 450);
                }
                // TODO: 2018/9/7 这里有时候会触发非法状态的异常
            } catch (Exception e) {
                LogUtil.i(TAG, "updateSeekBarRunnable: " + e.getMessage());
            }
        }
    };

    // 用于倒计时两秒，以隐藏控件
    private CountDownTimer cdt = new CountDownTimer(2000, 2000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            changeVideoControlState(VIDEO_CONTROL_VISIBLE);
        }
    };

    public void onPause(){
        delayedStopUpdateSeekBar();
    }

    private void delayedStopUpdateSeekBar() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                seekBarUpdateHandler.removeCallbacks(updateSeekBarRunnable);
            }
        }, 500);
    }
    // 设置视频的基础信息
    public void setMediaBaseInfo(VideoModel videoModel) {
        setVideoTitle(videoModel.getFileName());
        setRateInfo(videoModel.getPresentHURL(), videoModel.getPresentURL(), videoModel.getPresentLURL());
    }

    // 设置视频标题
    private void setVideoTitle(String title) {
        String videoTitle = TextUtils.isEmpty(title) ? "NO TITLE" : title;
        videoTitleTv.setText(videoTitle);
    }

    // 设置视频分辨率信息
    private void setRateInfo(String heightUrl, String url, String lowUrl) {
        rateView.setVideoRate(getResolution(heightUrl, url, lowUrl));
        rateView.setRateLink(new String[]{heightUrl, url, lowUrl});
    }

    private int getResolution(String heightUrl, String url, String lowUrl) {
        if(!TextUtils.isEmpty(url)){
            return RateView.MIDDLE_RESOLUTION;
        }
        if(!TextUtils.isEmpty(heightUrl)){
            return RateView.HEIGHT_RESOLUTION;
        }
        if(!TextUtils.isEmpty(lowUrl)){
            return RateView.LOW_RESOLUTION;
        }
        return RateView.EMPTY_LINK;
    }

    // 设置当前的分辨率
    public void setCurrentRate() {
        changeRateTv.setText(rateView.getCurrentRateNameId());
    }

    // 设置视频总时长
    public void setVideoDuration(int duration) {
        videoDurationTv.setText(DurationUtil.durationToStrMs(duration, false));
    }

    public void setSeekBarMax(int duration) {
        playScheduleSb.setMax(duration);
    }

    // 更新当前播放进度
    public void updateVideoDuration(String currentVideoPlayTime) {
        playTimeTv.setText(currentVideoPlayTime);
    }

    // 获取播放状态
    public boolean isCurrentPlayState() {
        return currentPlayState;
    }

    public void setCurrentPlayState(boolean currentPlayState) {
        this.currentPlayState = currentPlayState;
    }

    //设置当前视频播放的时间点
    public void setCurrentVideoTimestamp(int progress){
        this.currentVideoTimestamp = progress;
    }

    // 获取当前视频播放的时间点
    public int getCurrentVideoTimestamp() {
        return currentVideoTimestamp;
    }

    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.root_layout){
            changeVideoControlState(VIDEO_CONTROL_VISIBLE);
        }else if(id == R.id.bottom_img_sound){
            changeVideoControlState(VIDEO_VOLUME_CHANGE);
        }else if(id == R.id.bottom_rate){
            changeVideoControlState(VIDEO_SHOW_RAGE_SELECTED);
        }else if(id == R.id.layout_bottom){
            // 防止滑动seekbar的时候导致触发控件层隐藏
            return;
        }

        // 需要回调给 activity 的触发事件
        if (controlListener == null) {
            return;
        }

        if (id == R.id.go_back_iv) {
            controlListener.onGoBack();
        }else if (id == R.id.reload_btn) {
            // 重新加载
            controlListener.onRefresh();
            changeVideoControlState(VIDEO_START_LOADING);
        }else if (id == R.id.bottom_img_playctrl || id == R.id.center_play) {
            // 播放
            if (currentPlayState) {
                changeVideoControlState(VIDEO_PAUSE);
            } else {
                changeVideoControlState(VIDEO_START);
            }
        }
    }

    //音量改变
    @Override
    public void onVolumeChange(int currentVolume) {
        LogUtil.i(TAG, "onVolumeChange :  " + currentVolume);
        if (currentVolume == 0) {
            // 静音
            volumeControlPresenter.startVibrator(getContext());
            soundStateIv.setImageResource(R.drawable.ss_video_bottom_sound_mute);
        } else {
            soundStateIv.setImageResource(R.drawable.ss_video_bottom_sound_not_mute);
        }
    }

    //分辨率切换
    @Override
    public void onRateClick(boolean isChangeRate) {
        if (isChangeRate && controlListener != null) {
            // 切换分辨率
//            controlListener.onChangeRateClick(rateView.getCurrentVideoUrl());
            controlListener.onChangeRateClick("");//测试，因没有Url，所以使用这项,否则会报错。
            rateView.setVisibility(View.GONE);
        }
    }

    //右侧菜单选项
    @Override
    public void doMediaAction(String tag) {
        if (controlListener != null) {
            controlListener.onTriggerRightOption(tag);
        }
    }

    // 两秒后自动隐藏;这里取巧，不在乎之前的状态是什么，只要 titleLayout 这个基准控件显示了，那么过两秒就隐藏
    // 整体的操作控件
    private void autoHideControlView() {
        if (titleLayout.getVisibility() == VISIBLE) {
            cdt.start();
        } else {
            cdt.cancel();
        }
    }

    // 增加音量
    public void increaseVolume() {
        if (volumeControlPresenter != null) {
            volumeControlPresenter.increaseVolume();
        }
    }

    // 降低音量
    public void reduceVolume() {
        if (volumeControlPresenter != null) {
            volumeControlPresenter.reduceVolume();
        }
    }

    public void setControlListener(OnPlayerViewControlListener controlListener) {
        this.controlListener = controlListener;
    }

    public interface OnPlayerViewControlListener {
        void onGoBack();

        void onRefresh();

        void onUserSeek(int progress);

        void onChangePlayState(boolean playState);

        void onChangeRateClick(String videoUrl);

        void onTriggerRightOption(String tag);
    }

    // 更改控件状态
    public void changeVideoControlState(@PlayerVideoControlInterface int state) {
//        LogUtil.i(TAG, "changeVideoControlState");
        switch (state) {
            case VIDEO_START_LOADING:
                changeToLoadingState();
                break;
            case VIDEO_LOADING_SUCCESS:
                changeToLoadingSuccess();
                break;
            case VIDEO_RELOAD:
                changeToReloadState();
                break;
            case VIDEO_START:
                changeToPlayState();
                break;
            case VIDEO_PAUSE:
                changeToPauseState();
                break;
            case VIDEO_VOLUME_CHANGE:
                changeToVolumeSelectedState();
                break;
            case VIDEO_SHOW_RAGE_SELECTED:
                changeToRateSelectedState();
                break;
            case VIDEO_CONTROL_VISIBLE:
                if (!changeToControlVisibleState()) {
                    // 如果更改状态失败，则直接返回，避免失败后，将 currentState 设置为 VIDEO_CONTROL_VISIBLE
                    return;
                }
                break;
            case VIDEO_RATE_CHANGE_SUCCESS:
                changeToSwitchRateSuccessState();
                break;
            case VIDEO_RATE_CHANGING:
                changeToSwitchRateState();
                break;
            case VIDEO_SEEKING:
                changeToVideoSeeking();
                break;
            case VIDEO_BUFFERING_START:
                changeToVideoBufferingStart();
                break;
            case VIDEO_SEEKING_SUCCESS:
                changeToVideoSeekSuccess();
                break;
            case VIDEO_LOST:
            case VIDEO_DECODE_FAILED:
                break;
            case VIDEO_NET_INVALID:
                break;
            default:
                LogUtil.i(TAG, "changeVideoControlState on default");
                break;
        }
        currentState = state;
        autoHideControlView();
    }

    private void resetControlView() {
        LogUtil.i(TAG, "resetControlView");
        titleLayout.setVisibility(GONE);
        loadingLayout.setVisibility(GONE);
        reloadLayout.setVisibility(GONE);
        bottomLayout.setVisibility(GONE);
        rateView.setVisibility(GONE);
        volumeSeekBarLayout.setVisibility(GONE);
        centerPlayStateIv.setVisibility(GONE);
        rightOptionView.setVisibility(GONE);
    }

    // 更改为loading状态
    private void changeToLoadingState() {
        LogUtil.i(TAG, "changeToLoadingState");
        resetControlView();
        titleLayout.setVisibility(VISIBLE);
        loadingLayout.setVisibility(VISIBLE);
    }

    private void changeToLoadingSuccess() {
        LogUtil.i(TAG, "changeToLoadingSuccess");
        resetControlView();
    }

    // 更改为reload 状态
    private void changeToReloadState() {
        LogUtil.i(TAG, "changeToReloadState");
        resetControlView();
        bottomTipTv.setVisibility(GONE);
        titleLayout.setVisibility(VISIBLE);
        reloadLayout.setVisibility(VISIBLE);
        reloadTipTv.setText(R.string.reload_touch_to_reload);
        reloadBtnLayout.setVisibility(VISIBLE);
    }

    // 选择视频分辨率; 注意。音量调节的显示和分辨率调节的显示是互斥的
    // 能切换到视频分辨率的选择状态，说明底部栏是在显示状态，所以这里无需重置控件的显示状态，
    // 直接显示分辨率选择控件即可
    private void changeToRateSelectedState() {
        LogUtil.i(TAG, "changeToRateSelectedState");
        if (rateView.getVisibility() == VISIBLE) {
            rateView.setVisibility(GONE);
        } else {
            volumeSeekBarLayout.setVisibility(GONE);
            rightOptionView.setVisibility(GONE);
            rateView.setVisibility(VISIBLE);
        }
    }

    // 音量调节; 注意。音量调节的显示和分辨率调节的显示是互斥的
    // 能切换到音量调节的选择状态，说明底部栏是在显示状态，所以这里无需重置控件的显示状态，
    // 直接显示音量调节控件即可
    private void changeToVolumeSelectedState() {
        LogUtil.i(TAG, "changeToVolumeSelectedState");
        if (volumeSeekBarLayout.getVisibility() == VISIBLE) {
            volumeSeekBarLayout.setVisibility(GONE);
            rightOptionView.setVisibility(VISIBLE);
        } else {
            volumeSeekBarLayout.setVisibility(VISIBLE);
            rightOptionView.setVisibility(GONE);
            rateView.setVisibility(GONE);
        }
    }

    // 更改为 播放状态
    private void changeToPlayState() {
        LogUtil.i(TAG, "changeToPlayState");
        centerPlayStateIv.setVisibility(GONE);
        playStateIv.setImageResource(R.drawable.ss_video_bottom_img_pause);
        seekBarUpdateHandler.post(updateSeekBarRunnable);
        notifyPlayState(true);
    }

    // 更改为 暂停状态
    private void changeToPauseState() {
        LogUtil.i(TAG, "changeToPauseState");
        playStateIv.setImageResource(R.drawable.ss_video_bottom_img_play);
        centerPlayStateIv.setVisibility(VISIBLE);
        // 这里需要延迟 500 ms 移除，避免立即移除，导致播放结束时 progress 走不满进度条
        delayedStopUpdateSeekBar();
        notifyPlayState(false);
    }

    // 拖动视频进度
    private void changeToVideoSeeking() {
        LogUtil.i(TAG, "changeToVideoSeeking");
        changeToVideoBufferingStart();
        if (controlListener != null) {
            controlListener.onUserSeek(currentVideoTimestamp);
        }
    }

    // 正在切换分辨率
    private void changeToSwitchRateState() {
        LogUtil.i(TAG, "changeToSwitchRateState");
        bottomTipTv.setVisibility(VISIBLE);
        bottomTipTv.setText(constructTips(rateView.getCurrentRateNameId(),
                R.string.video_changing_rate,
                true));
        // 切换分辨率的时候不再更新进度
        seekBarUpdateHandler.removeCallbacks(updateSeekBarRunnable);
    }

    // 切换分辨率成功
    private void changeToSwitchRateSuccessState() {
        LogUtil.i(TAG, "changeToSwitchRateSuccessState");
        bottomTipTv.setVisibility(VISIBLE);
        bottomTipTv.setText(constructTips(rateView.getCurrentRateNameId(),
                R.string.video_changed_rate,
                false));
        bottomTipTv.postDelayed(new Runnable() {
            @Override
            public void run() {
                bottomTipTv.setVisibility(GONE);
                bottomTipTv.setTag(VIDEO_SHOW_RAGE_SELECTED);
            }
        }, 2000);
        bottomTipTv.setTag(VIDEO_RATE_CHANGING);
    }

    // 开始缓冲
    private void changeToVideoBufferingStart() {
        LogUtil.i(TAG , "changeToVideoBufferingStart");
        bottomTipTv.setVisibility(VISIBLE);
        bottomTipTv.setText(getResources().getString(R.string.video_loading_buffer));
    }

    private void changeToVideoSeekSuccess() {
        LogUtil.i(TAG, "changeToVideoSeekSuccess");
        if (View.VISIBLE == bottomTipTv.getVisibility() && bottomTipTv.getTag() != null && VIDEO_RATE_CHANGING == (Integer) bottomTipTv.getTag()) {
            LogUtil.i(TAG, "changeToVideoSeekSuccess,but bottomTipTv doesn't need to be Gone");
            return;
        }
        bottomTipTv.setVisibility(GONE);
    }

    // 更改为显示
    // true: 更改显示成功；false: 更改显示失败
    private boolean changeToControlVisibleState() {
        LogUtil.i(TAG, "changeToControlVisibleState");
        if (currentState != VIDEO_START_LOADING &&
                currentState != VIDEO_RELOAD &&
                currentState != VIDEO_LOST &&
                currentState != VIDEO_DECODE_FAILED) {
            // 以标题栏为的显示为基准，更改显示的状态；例如：标题栏显示，调用改方法后更改状态为 隐藏
            if (titleLayout.getVisibility() != VISIBLE) {
                // 已经隐藏
                resetControlView();
                titleLayout.setVisibility(VISIBLE);
                bottomLayout.setVisibility(VISIBLE);
                rightOptionView.setVisibility(VISIBLE);
                if (!isCurrentPlayState()) {
                    // 暂停的时候，需要显示中间的暂停按钮
                    centerPlayStateIv.setVisibility(VISIBLE);
                }
            } else {
                resetControlView();
            }
            return true;
        }
        return false;
    }

    /**
     * @param playState 设置播放状态
     */
    private void notifyPlayState(boolean playState) {
        if (controlListener != null) {
            controlListener.onChangePlayState(playState);
            this.currentPlayState = playState;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float distanceX = event.getX() - startX;
                float distanceY = event.getY() - startY;
                if(Math.abs(distanceY) < 50 && distanceX > 100){
                    //快进
                    setVideo(true);
                }else if(Math.abs(distanceY) < 50 && distanceX < -100){
                    //快退
                    setVideo(false);
                }
                break;
        }
        return false;
    }

    private void setVideo(boolean flag){
        int currentPosition = getCurrentVideoTimestamp();
        if (flag) {
            setCurrentVideoTimestamp(currentPosition + 300);
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(currentPosition + 300);
            }
//            changeVideoControlState(VIDEO_SEEKING);
        } else {
            setCurrentVideoTimestamp(currentPosition - 300);
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(currentPosition - 300);
            }
//            changeVideoControlState(VIDEO_SEEKING);
        }
    }

    private static String durationToStrMs(int duration, boolean includeMs) {
        int ms = duration % 1000;
        int s = duration / 1000;
        int m = 0;
        if (s > 60) {
            m = s / 60;
            s = s % 60;
        }
        if (includeMs) {
            return String.format(Locale.CHINA, "%02d:%02d.%03d", m, s, ms);
        }
        return String.format(Locale.CHINA, "%02d:%02d", m, s);
    }

    // 构造提示语
    private SpannableStringBuilder constructTips(int currentRateNameId,
                                                 int baseTipsId,
                                                 boolean isChanging) {
        String currentRateName = getResources().getString(currentRateNameId);
        String baseTips = String.format(getResources().getString(baseTipsId),
                currentRateName);
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(baseTips);
        int color = getResources().getColor(R.color.video_rate_text_color);
        int index = isChanging ? baseTips.indexOf("，") : baseTips.length();
        stringBuilder.setSpan(new ForegroundColorSpan(color), index - 2, index,
                Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        return stringBuilder;
    }
}
