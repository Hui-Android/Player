package com.app.player;

import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;

import com.app.player.util.LogUtil;
import com.app.player.view.VolumeSeekBar;

import static android.content.Context.VIBRATOR_SERVICE;

public class VolumeControlPresenter implements VolumeSeekBar.OnSeekBarChangeListener {

    private static final String TAG = "VolumeControlPresenter";
    private VolumeSeekBar seekBar;
    private AudioManager mAudioManager = null;

    // currentVolume 等于 0 一定是静音，但静音不一定 currentVolume 等于 0
    private int currentVolume = 0;
    private boolean isMute = false;
    private int maxVolume;
    private boolean isInTracking = false;
    private VideoVolumeListener volumeListener;

    public VolumeControlPresenter(Context context, VolumeSeekBar seekBar) {
        initVolumeSeekBar(context, seekBar);
    }

    private void initVolumeSeekBar(Context context, VolumeSeekBar seekBar) {
        this.seekBar = seekBar;
        if (null == mAudioManager) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        this.seekBar.setMax(maxVolume);
        this.seekBar.setOnSeekBarChangeListener(this);
        this.seekBar.setProgress(currentVolume);
    }

    public int getCurrentVolume() {
        return currentVolume;
    }

    public void reduceVolume() {
        if (null != mAudioManager) {
            updateVolume(--currentVolume);
        }
        if (null != seekBar) {
            float scale = ((float) currentVolume / maxVolume);
            seekBar.setProgress((int) (scale * seekBar.getMax()));
            seekBar.onProgressRefresh(scale, true);
        }
    }

    public void increaseVolume() {
        if (null != mAudioManager) {
            updateVolume(++currentVolume);
        }
        if (null != seekBar) {
            float scale = ((float) currentVolume / maxVolume);
            seekBar.setProgress((int) (scale * seekBar.getMax()));
            seekBar.onProgressRefresh(scale, true);
        }
    }

    /**
     * 改变音量大小和静音操作
     *
     * @param index
     */
    public void updateVolume(int index) {
        if (index < 0) {
            index = 0;
        } else if (index > maxVolume) {
            index = maxVolume;
        }
        if (mAudioManager != null) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
            setMute(index == 0);
            currentVolume = index;
        }
        if (volumeListener != null) {
            volumeListener.onVolumeChange(index);
        }
    }

    // 恢复声音
    public void resumeSound() {
        updateVolume(currentVolume == 0 ? maxVolume / 2 : currentVolume);
    }

    // 设置静音
    public void setSoundMute() {
        if (mAudioManager != null) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            setMute(true);
        }
    }

    public boolean isMute() {
        return isMute;
    }

    public void setMute(boolean mute) {
        this.isMute = mute;
    }

    public void release() {
        mAudioManager = null;
    }

    // 静音/恢复 切换
    public void switchMute(Context context) {
        startVibrator(context);
        if (isMute()) {
            resumeSound();
        } else {
            setSoundMute();
        }
    }

    // 开始震动
    public void startVibrator(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        long[] pattern = {0, 200}; // wait/ON/OFF/ON...
        vibrator.vibrate(pattern, -1);
    }

    @Override
    public void onProgressChanged(VolumeSeekBar seekBar, int progress, boolean fromUser) {
        if (isInTracking) {
            updateVolume(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(VolumeSeekBar seekBar) {
        isInTracking = true;
    }

    @Override
    public void onStopTrackingTouch(VolumeSeekBar seekBar) {
        isInTracking = false;
    }

    public void setVolumeListener(VideoVolumeListener volumeListener) {
        this.volumeListener = volumeListener;
    }

    public interface VideoVolumeListener {
        void onVolumeChange(int currentVolume);
    }
}
