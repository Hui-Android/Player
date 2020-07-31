package com.app.player.entity;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class UriModel implements Parcelable {

    private Uri audioUri;
    private Uri videoUri;
    private String AudioTitle;
    private String VideoTitle;
    private String AudioSize;

    public Uri getAudioUri(){
        return audioUri;
    }

    public void setAudioUri(Uri uri){
        this.audioUri = uri;
    }

    public Uri getVideoUri() {
        return videoUri;
    }

    public void setVideoUri(Uri videoUri){
        this.videoUri = videoUri;
    }

    public String getAudioTitle() {
        return AudioTitle;
    }

    public void setAudioTitle(String audioTitle) {
        AudioTitle = audioTitle;
    }

    public String getVideoTitle() {
        return VideoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        VideoTitle = videoTitle;
    }

    public String getAudioSize() {
        return AudioSize;
    }

    public void setAudioSize(String audioSize) {
        AudioSize = audioSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.audioUri, flags);
        dest.writeParcelable(this.videoUri, flags);
        dest.writeString(this.AudioTitle);
        dest.writeString(this.VideoTitle);
        dest.writeString(this.AudioSize);
    }

    public UriModel() {
    }

    protected UriModel(Parcel in) {
        this.audioUri = in.readParcelable(Uri.class.getClassLoader());
        this.videoUri = in.readParcelable(Uri.class.getClassLoader());
        this.AudioTitle = in.readString();
        this.VideoTitle = in.readString();
        this.AudioSize = in.readString();
    }

    public static final Parcelable.Creator<UriModel> CREATOR = new Parcelable.Creator<UriModel>() {
        @Override
        public UriModel createFromParcel(Parcel source) {
            return new UriModel(source);
        }

        @Override
        public UriModel[] newArray(int size) {
            return new UriModel[size];
        }
    };
}
