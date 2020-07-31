package com.app.player;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.player.entity.AudioModel;
import com.app.player.entity.UriModel;
import com.app.player.util.ByteUtil;
import com.app.player.util.DurationUtil;
import com.app.player.util.LogUtil;
import com.app.player.util.NetWorkUtil;
import com.app.player.util.ToastUtil;
import com.app.player.view.BottomPopDialog;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;

/**
 * 音频播放
 */
public class AudioPlayerActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "AudioPlayerActivity";
    private TextView view_tv_title,view_tv_current_time,view_tv_max_time,view_tv_center_name,view_tv_buffering;
    private TextView view_share, view_delete, view_download;
    private ImageView view_img_play,view_img_next,view_img_prev,view_center_icon,ivMore;
    private SeekBar seekBar;
    private MediaPlayer mediaPlayer;
    private int duration = 0;
    private int currentProgress = 0;

    private String audio_title;
    private String audio_size;
    private boolean isLocal = false;
    private UriModel uriModel;
    private String audioPath;
    private Uri audioUri;
    private BroadcastReceiver noisyCastReceiver;

    private boolean isPrevious = false;
    private boolean isNext = false;
    private boolean isSingle = false;   //单曲循环
    private boolean isList = true;     //列表循环，默认播放模式
    private boolean isRandom = false;   //随机播放
    private int music_index = -1;
    private List<AudioModel> musicList; //本地媒体库音频
    public static final int PERMISSON_REQUESTCODE = 100;

    /*
     * 音量控制
     */
    private AudioManager mAudioManager = null;
    private int maxVolume = 0;
    private int currentVolume = 0;

    private int countResume = 0;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = null;

    private int currentAim = -1;

    private static final int CHECK_AIM_NO = -1;
    private static final int CHECK_AIM_STARTPLAYER = 0;
    private static final int CHECK_AIM_STARTPLAYER_SEEKTO = 4;
    private static final int CHECK_AIM_NEXT = 1;
    private static final int CHECK_AIM_PREV = 2;
    private static final int CHECK_AIM_CURRENT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ActivityCompat.checkSelfPermission(AudioPlayerActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                PermissionGen.needPermission(this, PERMISSON_REQUESTCODE,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE});
            }else{
                initMusicList();
            }
        }

        initView();
        initData();
        addListener();
    }

    private void initView(){
        uriModel = getIntent().getParcelableExtra("uriModel");
        if(uriModel != null){
            audio_title = uriModel.getAudioTitle();
            audio_size = uriModel.getAudioSize();
            setAudioUri(uriModel.getAudioUri());
            isLocal = true;
        }else{
            music_index = new Random().nextInt(musicList.size());
            Uri randomUri = queryUri(musicList.get(music_index).getDisplayName());
            if(randomUri != null){
                audioUri = randomUri;
                audio_title = musicList.get(music_index).getDisplayName();
                audio_size = ByteUtil.formetFileSize(musicList.get(music_index).getSize());
            }
        }

        view_tv_title = findViewById(R.id.common_title_tv);

        view_tv_current_time = findViewById(R.id.bottom_ctrl_current_time);
        view_tv_buffering = findViewById(R.id.bottom_ctrl_buffering);
        view_tv_max_time = findViewById(R.id.bottom_ctrl_max_time);
        view_tv_center_name = findViewById(R.id.playact_center_name);

        view_img_play = findViewById(R.id.play_ctrl_play);
        view_img_play.setSelected(false);
        view_img_play.setOnClickListener(this);
        view_img_next = findViewById(R.id.play_ctrl_next);
        view_img_next.setSelected(true);
        view_img_next.setOnClickListener(this);
        view_img_prev = findViewById(R.id.play_ctrl_prev);
        view_img_prev.setSelected(true);
        view_img_prev.setOnClickListener(this);

        seekBar = findViewById(R.id.audio_ctrl_seekbar);
        seekBar.setOnSeekBarChangeListener(new AudioBarBarChangeListener());

        findViewById(R.id.title_back).setOnClickListener(this);
        view_center_icon = findViewById(R.id.playact_center_icon);
        ivMore = findViewById(R.id.ss_audio_more);
        ivMore.setOnClickListener(this);

        view_share = findViewById(R.id.share);
        view_share.setOnClickListener(this);
        view_download = findViewById(R.id.download);
        view_download.setOnClickListener(this);
        view_delete = findViewById(R.id.delete);
        view_delete.setOnClickListener(this);
    }

    private void initData(){
        noisyCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                    // 拔掉耳机时候进行相应的操作
                    pausePlayer();
                }
            }
        };
        registerReceiver(noisyCastReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        final TelephonyManager tManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void addListener(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1){
            mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    if(focusChange == AudioManager.AUDIOFOCUS_LOSS){
                        //失去焦点之后的操作
                        pausePlayer();
                    }else if(focusChange == AudioManager.AUDIOFOCUS_GAIN){
                        //获得焦点之后的操作
                    }
                }
            };
        }
    }

    private void initMusicList(){
        AudioProvider audioProvider = new AudioProvider(this);
        musicList = (List<AudioModel>) audioProvider.getList();
        if(musicList != null && musicList.size() > 0){
            LogUtil.i(TAG,musicList.size()+"");
        }
    }

    private void setAudioPath(String path){
        setAudioUri(Uri.parse(path));
    }

    private void setAudioUri(Uri uri){
        this.audioUri = uri;
    }

    private Uri getAudioUri(){
        return audioUri;
    }

    class AudioBarBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private boolean needResume = false;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            currentProgress = progress;
            if (fromUser) {
                controlHandler.sendEmptyMessage(MSG_WHAT_PROGRESSCHANGED);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            needResume = dealCurrentState(-1, false) == STATE_PLAYING || dealWaitPlayMedia(false, false);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (needResume) {
                needResume = false;
                checkState(CHECK_AIM_STARTPLAYER_SEEKTO);
            }
        }
    }

    private void requestAudioFocus() {
        if(mAudioManager != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 大于Android 8.0+
                AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setOnAudioFocusChangeListener(mAudioFocusChangeListener).build();
                audioFocusRequest.acceptsDelayedFocusGain();
                mAudioManager.requestAudioFocus(audioFocusRequest);
            } else {
                // 小于Android 8.0
                int result = mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

                }
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (uriModel != null || musicList != null && musicList.size() > 0) {
            if (countResume == 0) {
                countResume++;
                checkState(CHECK_AIM_NO);
            }
        }else{
            ToastUtil.showDefaultToast(this, getString(R.string.no_music));
        }
        initVolume();
    }

    /**
     * 初始化音频控制数据
     */
    private void initVolume() {
        if (null == mAudioManager) {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    //试图开始播放、上一首、下一首时调用
    private void checkState(final int aim) {
        currentAim = aim;
        boolean networkState = NetWorkUtil.isConnected(this);
        if (aim == CHECK_AIM_CURRENT || aim == CHECK_AIM_NEXT || aim == CHECK_AIM_PREV || aim == CHECK_AIM_STARTPLAYER) {
            if (dealCurrentState(-1, false) == STATE_GETING || dealCurrentState(-1, false) == STATE_PREPARING) {
                return;
            }
        }

        if (aim == CHECK_AIM_NO) {
            /**
             * 第一次进入
             */
            callGetCurrentMedia();
        }else if (aim == CHECK_AIM_STARTPLAYER || aim == CHECK_AIM_NEXT || aim == CHECK_AIM_PREV || aim == CHECK_AIM_CURRENT
                || aim == CHECK_AIM_STARTPLAYER_SEEKTO ) {
            if(networkState){
                callByAim(aim);
            }else{
                if (aim == CHECK_AIM_STARTPLAYER
                        && (dealCurrentState(-1, false) == STATE_PREPARED || dealCurrentState(-1, false) == STATE_PAUSE
                        || dealCurrentState(-1, false) == STATE_COMPLETE || dealCurrentState(
                        -1, false) == STATE_PLAYING) || aim == CHECK_AIM_STARTPLAYER_SEEKTO ) {
                    callByAim(aim);
                } else {
                    Message.obtain(controlHandler, MSG_WHAT_TOAST, Toast.LENGTH_SHORT, 0, "网络不可用，请检查你的网络设置！").sendToTarget();
                    controlHandler.sendEmptyMessage(MSG_WHAT_LOADFILE_ERROR);
                }
            }
        }
    }

    private void callByAim(int aim) {
        if (aim == CHECK_AIM_NO) {

        } else if (aim == CHECK_AIM_STARTPLAYER) {
            if (currentState != STATE_COMPLETE && currentState != STATE_PAUSE && currentState != STATE_PREPARED){
                return;
            }
            startPlayer();
        } else if (aim == CHECK_AIM_NEXT) {
            callGetNextMedia();
        } else if (aim == CHECK_AIM_PREV) {
            callGetPrevMedia();
        } else if (aim == CHECK_AIM_CURRENT) {
            callGetCurrentMedia();
        }else if (aim == CHECK_AIM_STARTPLAYER_SEEKTO){
            startPlayer();
        }
    }

    private void resetParma() {
        duration = 0;
        currentProgress = 0;
        controlHandler.sendEmptyMessage(MSG_WHAT_UPDATE_TIMEVIEW);
    }

    private void actFinish(){
        pausePlayer();
        Thread thread = new Thread(new Runnable() {
            public void run() {
                releaseMediaPlayer();
            }
        }, "releaseThread");
        thread.start();
        this.finish();
    }

    @Override
    public boolean isFinishing(){
        return super.isFinishing();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(noisyCastReceiver);

        if (mAudioManager != null && mAudioFocusChangeListener != null) {
            mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
        }
    }

    private void initMediaPlayer(){
        dealWaitPlayMedia(true, true);
        prepareMedia();
    }

    /**
     * @Title: releaseMediaPlayer
     * @Description: 释放播放器
     */
    synchronized private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (this.currentState > STATE_NO_PREPARE) {
            this.currentState = STATE_NO_PREPARE;
        }
    }

    private static long lastClickTime;
    public synchronized static boolean isFastClick(){
        long time = System.currentTimeMillis();
        if (time - lastClickTime < 1000){
            return true;
        }
        lastClickTime = time;
        return false;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.play_ctrl_next) {
            if (isFastClick()){
                return;
            }
            if (!v.isSelected()) {
                isNext = true;
                checkState(CHECK_AIM_NEXT);
            }else{
                ToastUtil.showDefaultToast(this,getString(R.string.is_the_last_music));
            }

        }else if(id == R.id.play_ctrl_play){
            if (audioUri == null){
                return;
            }

            if (!v.isSelected()) {
                //如果当前还没有加载 就加载当前的
                if (currentState == STATE_NO_MEDIA){
                    callGetCurrentMedia();
                    return;
                }
                if (currentState != STATE_PAUSE && currentState != STATE_PREPARED && currentState != STATE_COMPLETE) {
                    initMediaPlayer();
                    return;
                }
                checkState(CHECK_AIM_STARTPLAYER);
            } else {
                pausePlayer();
            }
        }else if(id == R.id.play_ctrl_prev){
            if (isFastClick()){
                return;
            }

            if(isLocal){
                return;
            }

            if (!v.isSelected()) {
                isPrevious = true;
                checkState(CHECK_AIM_PREV);
            }else{
                ToastUtil.showDefaultToast(this,getString(R.string.is_the_first_music));
            }
        }else if(id == R.id.title_back){
            actFinish();
        }else if(id == R.id.ss_audio_more) {
            if (isFastClick()) {
                return;
            }
            showPopupWindow(v);
        }else if(id == R.id.dialog_dismiss){
            if (musicSelectDialog != null && musicSelectDialog.isShowing()) {
                musicSelectDialog.dismiss();
                musicSelectDialog = null;
            }
        }else if(id == R.id.share){
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, audioUri);
            intent.setType("audio/*");
            startActivity(Intent.createChooser(intent, "分享我的音频到..."));
        }else if(id == R.id.download){
            ToastUtil.showDefaultToast(this,"download");
        }else if(id == R.id.delete){
            ToastUtil.showDefaultToast(this,"delete");
        }
    }

    private void prepareMedia() {
        if (dealCurrentState(-1, false) == STATE_PREPARING) {
            return;
        }
        dealCurrentState(STATE_PREPARING, true);
        Thread createThread = new Thread(new Runnable() {
            public void run() {
                try {
                    releaseMediaPlayer();
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {

                        @Override
                        public boolean onInfo(MediaPlayer mp, int what, int extra) {
                            if (what == 701) {
                                Message.obtain(controlHandler, MSG_WHAT_BUFFERING, 1, 0).sendToTarget();
                                return true;
                            } else if (what == 702) {
                                Message.obtain(controlHandler, MSG_WHAT_BUFFERING, 0, 0).sendToTarget();
                                return true;
                            }
                            return false;
                        }
                    });
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            if (dealCurrentState(-1, false) < STATE_PREPARED) {
                                return;
                            }
                            dealCurrentState(STATE_COMPLETE, true);
                            pausePlayer();
                            currentProgress = 0;
                            controlHandler.sendEmptyMessage(MSG_WHAT_PLAYER_COMPLTION);

                            if(isLocal){
                                isLocal = false;
                            }

                            if(!isSingle){ //非单曲循环
                                if(isList){ //列表循环
                                    if(music_index == musicList.size() - 1){ //最后一首重置播放下标
                                        music_index = -1;
                                    }
                                }else if(isRandom){ //随机播放
                                    music_index = new Random().nextInt(musicList.size());
                                }
                            }
                            callGetNextMedia();
                        }
                    });
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            dealCurrentState(STATE_PREPARED, true);
                            controlHandler.sendEmptyMessage(MSG_WHAT_PLAYER_PREPARE);
                        }
                    });
                    mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {// 该问题下需要将播放器释放
                                mediaPlayer.release();
                                mediaPlayer = null;
                            } else {
                                pausePlayer();
                                mediaPlayer.release();
                                mediaPlayer = null;
                            }
                            dealCurrentState(STATE_NO_PREPARE, true);
                            LogUtil.d(TAG,getString(R.string.loading_failed));
                            return true;
                        }
                    });
                    mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {

                        @Override
                        public void onSeekComplete(MediaPlayer mp) {
                            mp.start();
                        }
                    });

                    if (getAudioUri() == null){
                        LogUtil.i(TAG,getString(R.string.audio_play_get_resource_error));
                        finish();
                        return;
                    }

                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.setDataSource(AudioPlayerActivity.this, audioUri);
                    mediaPlayer.prepare();
                } catch (Exception e) {
                    dealCurrentState(STATE_NO_PREPARE, true);
                    e.printStackTrace();
                    controlHandler.sendEmptyMessage(MSG_WHAT_LOADFILE_ERROR);
                }
            }
        }, "createThread");
        createThread.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            actFinish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /*-------------------------------------Handler start ---------------------------------------*/
    private static final int MSG_WHAT_LOADFILE_ERROR = 2;
    private static final int MSG_WHAT_UPDATE_TIMEVIEW = 3;
    private static final int MSG_WHAT_PLAYER_COMPLTION = 4;
    private static final int MSG_WHAT_UPDATE_PROGRESS = 5;
    private static final int MSG_WHAT_PROGRESSCHANGED = 6;
    private static final int MSG_WHAT_PLAYER_PREPARE = 7;
    private static final int MSG_WHAT_RESET = 8;
    private static final int MSG_WHAT_PLAYBTN_STATE = 9;
    /**
     * arg1=0:缓冲完成<br/>
     * arg1=1:开始缓冲
     */
    private static final int MSG_WHAT_BUFFERING = 11;
    private static final int MSG_WHAT_SET_CENTER_NAME = 12;
    private static final int MSG_WHAT_TOAST = 13;

    private static final int PROGRESS_UPDATE_DELAY = 1000;

    static class ControlHandler extends Handler {
        private WeakReference<AudioPlayerActivity> ref;

        ControlHandler(AudioPlayerActivity act) {
            ref = new WeakReference<>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final AudioPlayerActivity act = ref.get();
            if (act == null) {
                return;
            }
            switch (msg.what) {
                case MSG_WHAT_LOADFILE_ERROR:
                    act.view_tv_title.setVisibility(View.INVISIBLE);
                    act.view_tv_center_name.setVisibility(View.INVISIBLE);
                    ToastUtil.showDefaultToast(act, act.getString(R.string.loading_failed));
                    break;
                case MSG_WHAT_UPDATE_TIMEVIEW:
                    act.seekBar.setMax(act.duration);
                    act.seekBar.setProgress(act.currentProgress);
                    act.view_tv_max_time.setText("-" + DurationUtil.durationToStrMs(act.duration, false));
                    act.view_tv_current_time.setText(DurationUtil.durationToStrMs(act.currentProgress, false));
                    break;
                case MSG_WHAT_PLAYER_COMPLTION:
                    act.seekBar.setProgress(0);
                    act.view_tv_max_time.setText("-" + DurationUtil.durationToStrMs(act.duration, false));
                    act.view_tv_current_time.setText(DurationUtil.durationToStrMs(0, false));
                    break;
                case MSG_WHAT_UPDATE_PROGRESS:
                    try {
                        if (act.mediaPlayer != null && act.mediaPlayer.isPlaying()) {
                            LogUtil.i(TAG,"MSG_WHAT_UPDATE_PROGRESS=====");
                            act.seekBar.setProgress(act.mediaPlayer.getCurrentPosition());
                            int mpTime = act.mediaPlayer.getCurrentPosition();
                            act.view_tv_max_time.setText("-" + DurationUtil.durationToStrMs(act.duration - mpTime, false));
                            act.view_tv_current_time.setText(DurationUtil.durationToStrMs(mpTime, false));
                            act.controlHandler.sendEmptyMessageDelayed(MSG_WHAT_UPDATE_PROGRESS, PROGRESS_UPDATE_DELAY);
                        } else {
                            LogUtil.i(TAG,"MSG_WHAT_UPDATE_PROGRESS remove");
                            act.controlHandler.removeMessages(MSG_WHAT_UPDATE_PROGRESS);
                        }
                    }catch (Exception e){

                    }
                    break;
                case MSG_WHAT_PROGRESSCHANGED:
                    int seekTime = act.seekBar.getProgress();
                    act.view_tv_max_time.setText("-" + DurationUtil.durationToStrMs(act.duration - seekTime, false));
                    act.view_tv_current_time.setText(DurationUtil.durationToStrMs(seekTime, false));
                    break;
                case MSG_WHAT_PLAYER_PREPARE:
                    if (act.mediaPlayer != null){
                        act.duration = act.mediaPlayer.getDuration();
                        act.controlHandler.sendEmptyMessage(MSG_WHAT_UPDATE_TIMEVIEW);
                        act.startPlayer();
                    }
                    break;
                case MSG_WHAT_RESET:
                    act.currentProgress = 0;

                    act.view_tv_max_time.setText("-" + DurationUtil.durationToStrMs(act.duration - act.currentProgress, false));
                    act.view_tv_current_time.setText(DurationUtil.durationToStrMs(act.currentProgress, false));
                    act.view_tv_title.setText(act.audio_title);
                    act.view_tv_center_name.setText(act.audio_title);
                    if(act.audio_size != null){
                        act.view_download.setText(act.getString(R.string.download) + "(" + act.audio_size + ")");
                    }
                    act.view_center_icon.setImageResource(R.drawable.music_musicpreview_icon);

                    if(act.music_index <= 0){
                        act.view_img_prev.setAlpha((float) 0.5);
                        act.view_img_prev.setSelected(true);
                    }else{
                        act.view_img_prev.setAlpha((float) 1.0);
                        act.view_img_prev.setSelected(false);
                    }

                    if(act.music_index < act.musicList.size() - 1){
                        act.view_img_next.setAlpha((float) 1.0);
                        act.view_img_next.setSelected(false);
                    }else{
                        act.view_img_next.setAlpha((float) 0.5);
                        act.view_img_next.setSelected(true);
                    }
                break;
                case MSG_WHAT_PLAYBTN_STATE:
                    if (msg.arg1 == 0) {
                        act.view_img_play.setSelected(false);
                    } else {
                        act.view_img_play.setSelected(true);
                    }
                    break;
                case MSG_WHAT_BUFFERING:
                    if (msg.arg1 == 1) {
                        act.view_tv_max_time.setVisibility(View.INVISIBLE);
                        act.view_tv_current_time.setVisibility(View.INVISIBLE);

                        act.view_tv_buffering.setVisibility(View.VISIBLE);
                    } else {
                        act.view_tv_max_time.setVisibility(View.VISIBLE);
                        act.view_tv_current_time.setVisibility(View.VISIBLE);
                        act.view_tv_buffering.setVisibility(View.INVISIBLE);
                    }
                    break;
                case MSG_WHAT_SET_CENTER_NAME:
                    if (msg.obj == null) {
                        act.view_tv_center_name.setText(act.audio_title);
                    } else {
                        act.view_tv_center_name.setText((String) msg.obj);
                    }
                    break;
            }
        }
    }
    private ControlHandler controlHandler = new ControlHandler(this);
    /*-------------------------------------Handler end ---------------------------------------*/

    private void resetView(){
        if (getAudioUri() != null){
            controlHandler.sendEmptyMessage(MSG_WHAT_RESET);
            resetParma();
        }
    }

    private void callGetCurrentMedia() {
        pausePlayer();
        resetView();
        initMediaPlayer();
    }

    private void callGetNextMedia() {
        if (dealCurrentState(-1, false) == STATE_GETING || dealCurrentState(-1, false) == STATE_PREPARING) {
            return;
        }

        pausePlayer();
        if(!isSingle || isNext){  //播放模式为非单曲循环或点击播放下一首
            if(isList || !isRandom || isNext){ //播放模式为列表循环或随机播放
                music_index = music_index + 1;
                if(isNext){
                    isNext = false;
                }
            }
        }
        Uri queryUri = queryUri(musicList.get(music_index).getDisplayName());
        if(queryUri != null){
            audioUri = queryUri;
            audio_title = musicList.get(music_index).getDisplayName();
            audio_size = ByteUtil.formetFileSize(musicList.get(music_index).getSize());
        }
        resetView();
        initMediaPlayer();
    }

    private void callGetPrevMedia() {
        if (dealCurrentState(-1, false) == STATE_GETING || dealCurrentState(-1, false) == STATE_PREPARING) {
            return;
        }
        pausePlayer();
        if(!isSingle || isPrevious){    //播放模式为非单曲循环或点击播放上一首
            if(isList || !isRandom || isPrevious){    //播放模式为列表循环或随机播放
                music_index = music_index - 1;
                if(isPrevious){
                    isPrevious = false;
                }
            }
        }
        Uri queryUri = queryUri(musicList.get(music_index).getDisplayName());
        if(queryUri != null){
            audioUri = queryUri;
            audio_title = musicList.get(music_index).getDisplayName();
            audio_size = ByteUtil.formetFileSize(musicList.get(music_index).getSize());
        }
        resetView();
        initMediaPlayer();
    }

    PhoneStateListener phoneStateListener = new PhoneStateListener() {
        boolean isringpause = false;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:// 无任何状态
                    LogUtil.i(TAG, "call 无状态");
                    if (isringpause) {
                        startPlayer();
                        isringpause = false;
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:// 接听状态
                    LogUtil.i(TAG, "call 接听");
                    if (dealCurrentState(-1, false) == STATE_PLAYING || dealWaitPlayMedia(false, false)) {
                        isringpause = true;
                    }
                    pausePlayer();
                    break;
                case TelephonyManager.CALL_STATE_RINGING:// 来电状态
                    LogUtil.i(TAG, "call 来电");
                    if (dealCurrentState(-1, false) == STATE_PLAYING || dealWaitPlayMedia(false, false)) {
                        isringpause = true;
                    }
                    pausePlayer();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 状态：没有媒体资源
     */
    private static final int STATE_NO_MEDIA = 1;
    /**
     * 状态：媒体资源获取中
     */
    private static final int STATE_GETING = 2;
    /**
     * 状态：未准备
     */
    private static final int STATE_NO_PREPARE = 3;
    /**
     * 状态：准备中
     */
    private static final int STATE_PREPARING = 4;
    /**
     * 状态：准备完成
     */
    private static final int STATE_PREPARED = 5;
    /**
     * 状态：播放
     */
    private static final int STATE_PLAYING = 6;
    /**
     * 状态：暂停
     */
    private static final int STATE_PAUSE = 7;
    /**
     * 状态：播放完成
     */
    private static final int STATE_COMPLETE = 8;

    private int currentState = STATE_NO_MEDIA;

    synchronized private int dealCurrentState(int state, boolean isSet) {
        if (isSet) {
            this.currentState = state;
            if (currentState == STATE_PREPARING) {
                Message.obtain(controlHandler, MSG_WHAT_SET_CENTER_NAME, "加载中···").sendToTarget();
            } else if (currentState == STATE_PREPARED) {
                Message.obtain(controlHandler, MSG_WHAT_SET_CENTER_NAME, null).sendToTarget();
            }
            return -1;
        } else {
            return this.currentState;
        }
    }

    /**
     * 等待播放
     */
    private boolean waitPlayMedia = false;

    synchronized private boolean dealWaitPlayMedia(boolean waitPlayMedia, boolean isSet) {
        if (isSet) {
            this.waitPlayMedia = waitPlayMedia;
            return false;
        } else {
            return this.waitPlayMedia;
        }
    }

    private void pausePlayer() {
        if (dealCurrentState(-1, false) < STATE_PREPARED) {
            dealWaitPlayMedia(false, true);
        }
        Message.obtain(controlHandler, MSG_WHAT_PLAYBTN_STATE, 0, 0).sendToTarget();
        Thread pauseThread = new Thread(new Runnable() { // anr
            public void run() {
                if (dealCurrentState(-1, false) == STATE_PLAYING) {
                    dealCurrentState(STATE_PAUSE, true);
                    try {
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                            currentProgress = seekBar.getProgress();
                        }
                    } catch (Exception e) {
                        // onError中调用可能报异常
                        e.printStackTrace();
                    }
                }
                controlHandler.removeMessages(MSG_WHAT_UPDATE_PROGRESS);
            }

        }, "pauseThread");
        pauseThread.start();
    }

    private void startPlayer() {
        requestAudioFocus();
        Thread startThread = new Thread(new Runnable() {

            @Override
            public void run() {
                if (dealCurrentState(-1, false) == STATE_PREPARED || dealCurrentState(-1, false) == STATE_PAUSE
                        || dealCurrentState(-1, false) == STATE_COMPLETE || dealCurrentState(-1, false) == STATE_PLAYING) {
                    Message.obtain(controlHandler, MSG_WHAT_SET_CENTER_NAME, null).sendToTarget();
                } else {
                    Message.obtain(controlHandler, MSG_WHAT_SET_CENTER_NAME, "加载中···").sendToTarget();
                }
                if (dealCurrentState(-1, false) == STATE_NO_MEDIA) {
                    dealWaitPlayMedia(true, true);
                    callGetCurrentMedia();
                } else if (dealCurrentState(-1, false) == STATE_GETING) {
                    dealWaitPlayMedia(true, true);
                } else if (dealCurrentState(-1, false) == STATE_PREPARING) {
                    dealWaitPlayMedia(true, true);
                } else if (dealCurrentState(-1, false) == STATE_NO_PREPARE) {
                    initMediaPlayer();
                } else if (dealCurrentState(-1, false) == STATE_PREPARED || dealCurrentState(-1, false) == STATE_PAUSE || dealCurrentState(-1, false) == STATE_COMPLETE
                        || dealCurrentState(-1, false) == STATE_PLAYING) {
                    dealWaitPlayMedia(false, true);
                    dealCurrentState(STATE_PLAYING, true);
                    Message.obtain(controlHandler, MSG_WHAT_PLAYBTN_STATE, 1, 0).sendToTarget();
                    if (currentAim == CHECK_AIM_STARTPLAYER_SEEKTO){
                        mediaPlayer.seekTo(currentProgress);
//                        return;
                    }
                    if (currentProgress == 0){
                        mediaPlayer.start();
                    }else {
                        mediaPlayer.seekTo(currentProgress);
                    }
                    controlHandler.sendEmptyMessageDelayed(MSG_WHAT_UPDATE_PROGRESS, PROGRESS_UPDATE_DELAY);
                }
            }
        }, "startThread");
        startThread.start();
    }

    @PermissionSuccess(requestCode = PERMISSON_REQUESTCODE)
    public void success(){
        Toast.makeText(AudioPlayerActivity.this, "授权成功", Toast.LENGTH_SHORT).show();
        initMusicList();
    }

    @PermissionFail(requestCode = PERMISSON_REQUESTCODE)
    public void fail() {
        Toast.makeText(AudioPlayerActivity.this, "授权失败", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private PopupWindow mPopupWindow = null;
    private void showPopupWindow(View parent){
        int height = (int) getResources().getDimension(R.dimen.qb_px_75);
        int widoff = (int) getResources().getDimension(R.dimen.qb_px_10);

        hidePopupWindow();

        if(mPopupWindow == null) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View mPopView = inflater.inflate(R.layout.pop_layout, null);
            mPopupWindow = new PopupWindow(mPopView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            TextView play_mode_tv = mPopView.findViewById(R.id.play_mode);
            TextView music_list_tv = mPopView.findViewById(R.id.music_list);

            play_mode_tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hidePopupWindow();
                    showModeSelectDiaolog();
                }
            });

            music_list_tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hidePopupWindow();
                    showMusicSelectDiaolog();
                }
            });

            mPopupWindow.setFocusable(true);
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
            mPopupWindow.showAtLocation(parent, Gravity.TOP | Gravity.RIGHT, widoff, height + 30);
        }
    }

    private void hidePopupWindow(){
        if(mPopupWindow != null && mPopupWindow.isShowing()){
            mPopupWindow.dismiss();
        }
        mPopupWindow = null;
    }

    private Dialog musicSelectDialog = null;
    private void showMusicSelectDiaolog(){

        if (musicSelectDialog != null && musicSelectDialog.isShowing()) {
            musicSelectDialog.dismiss();
            musicSelectDialog = null;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.music_list_layout, null);
        ImageView backIv = view.findViewById(R.id.dialog_dismiss);
        backIv.setOnClickListener(this);
        RecyclerView recyclerView = view.findViewById(R.id.music_recyclerview);
        musicSelectDialog = BottomPopDialog.getDialog(AudioPlayerActivity.this, view, musicSelectDialog);
        musicSelectDialog.show();

        LinearLayoutManager musicLayoutManager = new LinearLayoutManager(this);
        musicLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(musicLayoutManager);
        MusicAdapter musicAdapter = new MusicAdapter(AudioPlayerActivity.this);
        musicAdapter.setData(musicList);
        recyclerView.setAdapter(musicAdapter);
        musicAdapter.setOnItemClickListener(new MusicAdapter.MusicOnItemClick() {
            @Override
            public void musicOnItemClick(int position, String path) {
                music_index = position;
                musicSelectDialog.dismiss();
                if(ActivityCompat.checkSelfPermission(AudioPlayerActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED){
                    Uri queryUri = queryUri(musicList.get(position).getDisplayName());
                    if(queryUri != null){
                        audioUri = queryUri;
                        audio_title = musicList.get(position).getDisplayName();
                        audio_size = ByteUtil.formetFileSize(musicList.get(position).getSize());
                        callGetCurrentMedia();

                        if(isLocal){
                            isLocal = false;
                        }
                    }
                }else{
                    LogUtil.d(TAG,"请先申请权限");
                }
            }
        });
    }

    private Dialog modeSelectDialog = null;
    private void showModeSelectDiaolog(){
        if(modeSelectDialog != null && modeSelectDialog.isShowing()){
            modeSelectDialog.dismiss();
            modeSelectDialog = null;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.mode_select_layout, null);
        Button singleBtn = view.findViewById(R.id.single);
        Button listBtn = view.findViewById(R.id.list);
        Button randomBtn = view.findViewById(R.id.random);

        singleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (modeSelectDialog != null) {
                    modeSelectDialog.dismiss();
                }
                isSingle = true;
                isList = false;
                isRandom = false;
                ToastUtil.showDefaultToast(AudioPlayerActivity.this,"当前播放模式为单曲循环");
            }
        });

        listBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (modeSelectDialog != null) {
                    modeSelectDialog.dismiss();
                }
                isSingle = false;
                isList = true;
                isRandom = false;
                ToastUtil.showDefaultToast(AudioPlayerActivity.this,"当前播放模式为列表循环");
            }
        });

        randomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (modeSelectDialog != null) {
                    modeSelectDialog.dismiss();
                }
                isSingle = false;
                isList = false;
                isRandom = true;
                ToastUtil.showDefaultToast(AudioPlayerActivity.this,"当前播放模式为随机播放");
            }
        });

        modeSelectDialog = BottomPopDialog.getDialog(AudioPlayerActivity.this, view, modeSelectDialog);
        modeSelectDialog.show();
    }

    public final Uri queryUri(String displayName) {
        Uri external = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Images.Media.DISPLAY_NAME + "=?";
        String[] args = new String[]{displayName};
        String[] projection = new String[]{MediaStore.Images.Media._ID};
        Cursor cursor = getContentResolver().query(external, projection, selection, args, null);
        if (cursor != null && cursor.moveToNext()) {
            Uri queryUri = ContentUris.withAppendedId(external, cursor.getLong(0));
            LogUtil.d(TAG, "查询成功，Uri路径：" + queryUri);
            cursor.close();
            return queryUri;
        } else {
            LogUtil.d(TAG, "Uri is null");
            return null;
        }
    }
}
