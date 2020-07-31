package com.app.player;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.app.player.entity.UriModel;
import com.app.player.entity.VideoModel;
import com.app.player.util.LogUtil;
import com.app.player.util.ScreenUtil;
import com.app.player.util.ToastUtil;
import com.app.player.view.PlayerVideoControlView;
import com.app.player.view.VideoPlayerContract;

import java.io.IOException;

import static com.app.player.view.PlayerVideoControlView.VIDEO_RELOAD;
import static com.app.player.view.PlayerVideoControlView.VIDEO_SEEKING;

/**
 * 视频播放
 */
public class VideoPlayerActivity extends Activity implements View.OnClickListener,
        PlayerVideoControlView.OnPlayerViewControlListener, VideoPlayerContract.VideoPlayerViewer{

    private static final String TAG = "VideoPlayerActivity";
    private RelativeLayout rootLayout;
    private SurfaceView surfaceView;
    private VideoPlayerContract.VideoPlayerViewer viewer;
    private PlayerVideoControlView controlView;
    private SurfaceHolder surfaceHolder;
    private MediaPlayer mediaPlayer;

    // true : 在 onResume 的时候需要恢复播放状态
    // 用于记录app切换到后台时，app 的播放状态
    private boolean needToResumePlayingState = false;
    private boolean isSurfaceViewDestroy = false;

    private String currentVideoUrl;
    private boolean isBuffering = false;
    // 视频缓冲计时器
    private CountDownTimer bufferCountDownTimer;
    private Uri shareUri;
    private UriModel uriModel;

    private VideoCallBack videoCallBack;
    private boolean isLand = false; //是否为横屏

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen();
        resetConfigSetting();
        setContentView(R.layout.activity_video_player);

        initView();
    }

    private void initView(){

        videoCallBack =  new VideoCallBack();
        viewer = this;

        surfaceView = findViewById(R.id.surface_view);
        surfaceView.setOnClickListener(this);

        rootLayout = findViewById(R.id.root_layout);
        rootLayout.setOnClickListener(this);

        controlView = findViewById(R.id.player_view_control_view);
        controlView.setControlListener(this);

        //从手机本地选取视频播放
        uriModel = getIntent().getParcelableExtra("uriModel");
        if(uriModel != null){
            controlView.setTitleForNavite(uriModel.getVideoTitle());
            controlView.setVideoUri(uriModel.getVideoUri());
            shareUri = uriModel.getVideoUri();
        }

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(videoCallBack);

        showLoading();
        initBufferingAlarm();
    }

    private class VideoCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            LogUtil.i(TAG, "surfaceCreated");
            surfaceHolder = holder;
            initMediaPlayer(surfaceHolder);
            mediaPlayer.seekTo(controlView.getCurrentVideoTimestamp());
            if (isSurfaceViewDestroy && needToResumePlayingState) {
                isSurfaceViewDestroy = false;
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            LogUtil.i(TAG, "surfaceChanged");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            LogUtil.i(TAG, "surfaceDestroyed");
            isSurfaceViewDestroy = true;
        }
    }

    private void initMediaPlayer(SurfaceHolder surfaceHolder){
        LogUtil.i(TAG, "initMediaPlayer");
        if(mediaPlayer == null){
            mediaPlayer = new MediaPlayer();

            controlView.setMediaPlayer(mediaPlayer);

            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {

                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    LogUtil.i(TAG, "oninfo what:" + what + "---extra:" + extra);
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                        LogUtil.i(TAG, " buffer start");
                        if (viewer != null) {
                            viewer.onBufferingStart();
                        }
                        return true;
                    } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                        LogUtil.i(TAG, " buffer end");
                        if (viewer != null) {
                            viewer.onBufferingEnd();
                        }
                        return true;
                    }
                    return false;
                }
            });

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    LogUtil.i(TAG, "onCompletion");
                    if (viewer != null) {
                        viewer.onCompletion();
                    }
                }
            });

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    LogUtil.i(TAG, "setOnPreparedListener");
                    if (viewer != null) {
                        viewer.onPrepared(mp);
                    }
                }
            });

            mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {

                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int videoWidth, int videoHeight) {
                    LogUtil.i(TAG, "setOnVideoSizeChangedListener"+"====="+videoWidth+"====="+videoHeight);
                    if (videoWidth == 0 || videoHeight == 0) {
                        return;
                    }
                    calculateSurfaceViewSize(videoWidth, videoHeight);
                }
            });

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    LogUtil.i(TAG, "onerror what:" + what + "---extra:" + extra);
                    if (viewer != null) {
                        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED){
                            // 该问题下需要将播放器释放
                            viewer.restart();
                        }else {
                            viewer.onError(mp, what, extra);
                        }
                    }
                    return true;
                }
            });

            mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {

                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    if (viewer != null) {
                        viewer.onSeekComplete(mp);
                    }
                }
            });

            mediaPlayer.setDisplay(surfaceHolder);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            preparedMediaPlayer();
        }else{
            LogUtil.i(TAG,"mediaPlayer != null");
            mediaPlayer.setDisplay(surfaceHolder);
        }
    }

    private void preparedMediaPlayer(){
        LogUtil.i(TAG, "preparedMediaPlayer");

        if(mediaPlayer != null){
            try {
                mediaPlayer.reset();

                if(controlView.getVideoUrl() != null){  //网络或本地视频播放
                    mediaPlayer.setDataSource(this,controlView.getVideoUrl());
                }else{
                    mediaPlayer.setDataSource(this, Uri.parse("android.resource://" + getPackageName() + "/"+ R.raw.test_video));
                }
                mediaPlayer.prepareAsync();
                startBufferingCount();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void adapterOs() {
        // 这里一定要先调用一次
        // 否则在切换分辨率的时候，会出现（-38，0）的bug。这个因该是MediaPlayer的bug
        mediaPlayer.start();
        mediaPlayer.pause();
    }

    private void calculateSurfaceViewSize(int videoWidth, int videoHeight) {
        int width, height;
        if (videoWidth > videoHeight) {
            // 横屏
            width = ScreenUtil.getScreenWidth(this);
            height = width * videoWidth / videoHeight;
        } else {
            // 竖屏
            height = ScreenUtil.getScreenHeight(this);
            width = height * videoWidth / videoHeight;
        }
        if(!isLand){
            viewer.onVideoSizeChanged(width, height);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            LogUtil.i(TAG, String.valueOf(Configuration.ORIENTATION_LANDSCAPE));
            isLand = true;
        }
        else if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            LogUtil.i(TAG, String.valueOf(Configuration.ORIENTATION_PORTRAIT));
            isLand = false;

        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onGoBack() {
        controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_PAUSE);
        finishPage();
    }

    @Override
    public void onRefresh() {
        LogUtil.i(TAG, "onRefresh");
        preparedMediaPlayer();
    }

    @Override
    public void onUserSeek(int progress) {
        LogUtil.i(TAG, "onUserSeek : " + progress);
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(progress);
        }
    }

    @Override
    public void onChangePlayState(boolean playState) {
        LogUtil.i(TAG, "onChangePlayState");
        // 播放状态改变
        if (mediaPlayer != null) {
            if (playState) {
                LogUtil.i(TAG, "onChangePlayState start");
                mediaPlayer.start();
            } else {
                LogUtil.i(TAG, "onChangePlayState pause");
                mediaPlayer.pause();
            }
        }
    }

    @Override
    public void onChangeRateClick(String videoUrl) {
        currentVideoUrl = videoUrl;
//        controlView.setVideoPath(currentVideoUrl);
        controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_RATE_CHANGING);
        preparedMediaPlayer();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onTriggerRightOption(String tag) {
        ToastUtil.showDefaultToast(this, tag);
        if(tag.equals("share")){
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, shareUri);
            intent.setType("video/*");
            startActivity(Intent.createChooser(intent, "分享我的视频到..."));
        }else if(tag.equals("download")){

        }else if(tag.equals("delete")){
            finishPage();
        }
    }

    @Override
    public void showTips(String tips) {
        ToastUtil.showDefaultToast(this , tips);
    }

    @Override
    public void showTips(int tipsRes) {
        ToastUtil.showDefaultToast(this , tipsRes);
    }

    @Override
    public void finishPage() {
        release();
        controlView.post(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    @Override
    public Context getContext() {
        return VideoPlayerActivity.this;
    }

    @Override
    public void showLoading() {
        controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_START_LOADING);
    }

    @Override
    public void dismissLoading() {
        controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_LOADING_SUCCESS);
    }

    @Override
    public void preparedMediaFailed() {
        // 在指定时长内没有加载成功则会回调该方法。
        LogUtil.i(TAG, "preparedMediaFailed");
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            controlView.changeVideoControlState(VIDEO_RELOAD);
        }
    }

    /**
     * 获取服务端返回的Video实体
     * @param videoModel
     */
    @Override
    public void getCurrentMediaSuccess(VideoModel videoModel) {
        LogUtil.i(TAG, "getCurrentMediaSuccess ");

//        this.videoModel = videoModel;
//        controlView.setMediaBaseInfo(videoModel);
//        currentVideoUrl = getDefaultUrl(videoModel);
//        preparedMediaPlayer();
    }

    @Override
    public void getCurrentMediaFailed(int errorCode) {

    }

    @Override
    public void onBufferingStart() {
        LogUtil.i(TAG, "onBufferingStart");
        isBuffering = true;
        controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_BUFFERING_START);
    }

    @Override
    public void onBufferingEnd() {
        LogUtil.i(TAG, "onBufferingEnd");
        isBuffering = false;
        controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_SEEKING_SUCCESS);
    }

    @Override
    public void onCompletion() {
        LogUtil.i(TAG, "onCompletion");
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        cancelBufferingCount();
        dismissLoading();

        controlView.setSeekBarMax(mediaPlayer.getDuration());
        controlView.setVideoDuration(mediaPlayer.getDuration());
        // 设置当前的分辨率
        controlView.setCurrentRate();
        adapterOs();
        mediaPlayer.setLooping(true);

        // 如果视频当前的播放时间点大于0，说明之前已经播放过了。切换分辨率应该从原来的时间点继续
        if (controlView.getCurrentVideoTimestamp() > 0) {
            LogUtil.i(TAG, "onPrepared currentVideoTimestamp :  " + controlView.getCurrentVideoTimestamp());
            mediaPlayer.seekTo(controlView.getCurrentVideoTimestamp());
            if (needToResumePlayingState) {
                controlView.changeVideoControlState(VIDEO_SEEKING);
                needToResumePlayingState = false;
            } else {
                controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_RATE_CHANGE_SUCCESS);
            }
        } else {
            LogUtil.i(TAG, "onPrepared start from beginning");
            resumeVideoState();
        }
    }

    // 恢复视频原来的状态
    private void resumeVideoState() {
        LogUtil.i(TAG, "resumeVideoState");
        if (controlView.isCurrentPlayState() || needToResumePlayingState) {
            // 如果原来是播放状态，则应该保持播放状态，如果是暂停状态，则保持暂停状态
            LogUtil.i(TAG, "resumeVideoState start");
            if (needToResumePlayingState) {
                needToResumePlayingState = false;
            }
            controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_START);
        } else {
            LogUtil.i(TAG, "resumeVideoState pause");
            controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_PAUSE);
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        LogUtil.i(TAG, "onVideoSizeChanged");
    }

    @Override
    public void onVideoSizeChanged(int surfaceViewWidth, int surfaceViewHeight) {
        ViewGroup.LayoutParams surfaceLP = surfaceView.getLayoutParams();
        surfaceHolder.setFixedSize(surfaceViewWidth, surfaceViewHeight);
        surfaceLP.width = surfaceViewWidth;
        surfaceLP.height = surfaceViewHeight;
        surfaceView.setLayoutParams(surfaceLP);

    }

    @Override
    public void restart() {
        // 重新初始化播放器，并重试播放
        initMediaPlayer(surfaceHolder);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LogUtil.i(TAG, "onError : " + what);
        return true;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        LogUtil.i(TAG, "onSeekComplete");
        // 缓冲开始和缓冲结束必须配对。由于 缓冲开始的回调在 onSeekComplete 之前。为防止在 onSeekComplete
        // 方法中就隐藏了加载标志，这里需要进行判断是否缓冲开始
        if (!isBuffering) {
            controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_SEEKING_SUCCESS);
        }
        resumeVideoState();
    }

    /**
     * 获取默认播放视频连接
     */
    private String getDefaultUrl(VideoModel videoModel) {
        String normalUrl = videoModel.getPresentURL();
        String higherUrl = videoModel.getPresentHURL();
        String lowerUrl = videoModel.getPresentLURL();
        return TextUtils.isEmpty(normalUrl)
                ? (TextUtils.isEmpty(higherUrl) ? lowerUrl : higherUrl)
                : normalUrl;
    }

    private void setFullScreen() {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void resetConfigSetting() {
        Resources res = getResources();
        Configuration config = new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    private void release() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.i(TAG, "onResume");
        if (needToResumePlayingState && !isSurfaceViewDestroy) {
            controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_START);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtil.i(TAG, "onPause");
        cancelBufferingCount();
        if (controlView.isCurrentPlayState()) {
            needToResumePlayingState = true;
            onChangePlayState(false);
            controlView.onPause();
        } else {
            needToResumePlayingState = false;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.i(TAG, "onDestroy");
        if(videoCallBack != null){
            surfaceHolder.removeCallback(videoCallBack);
        }
        release();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.surface_view || id == R.id.root_layout) {
            // 更改播放控制控件的显示状态
            controlView.changeVideoControlState(PlayerVideoControlView.VIDEO_CONTROL_VISIBLE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                controlView.reduceVolume();
                return false;
            case KeyEvent.KEYCODE_VOLUME_UP:
                controlView.increaseVolume();
                return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 缓冲超过60秒则显示刷新
     */
    private void initBufferingAlarm() {
        bufferCountDownTimer = new CountDownTimer(60 * 1000, 60 * 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (viewer != null) {
                    viewer.preparedMediaFailed();
                }
            }
        };
    }

    public void startBufferingCount() {
        bufferCountDownTimer.cancel();
        bufferCountDownTimer.start();
    }

    public void cancelBufferingCount() {
        bufferCountDownTimer.cancel();
    }
}
