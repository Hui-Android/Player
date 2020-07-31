package com.app.player.view;

import android.content.Context;
import android.media.MediaPlayer;
import android.text.SpannableStringBuilder;
import android.view.SurfaceHolder;

import com.app.player.entity.VideoModel;

public interface VideoPlayerContract {
    interface VideoPlayerViewer {
        void showTips(String tips);

        void showTips(int tipsRes);

        void finishPage();

        Context getContext();

        void showLoading();

        void dismissLoading();

        void preparedMediaFailed();

        void getCurrentMediaSuccess(VideoModel videoModel);

        void getCurrentMediaFailed(int errorCode);

        void onBufferingStart();

        void onBufferingEnd();

        void onCompletion();

        void onPrepared(MediaPlayer mediaPlayer);

        void onVideoSizeChanged(MediaPlayer mp, int width, int height);

        void onVideoSizeChanged(int surfaceViewWidth, int surfaceViewHeight);

        void restart();

        boolean onError(MediaPlayer mp, int what, int extra);

        void onSeekComplete(MediaPlayer mp);
    }
}
