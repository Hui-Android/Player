package com.app.player.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class AudioModel implements Parcelable {

    private int id;
    private String title;
    private String album;
    private String artist;
    private String path;
    private String displayName;
    private String mimeType;
    private long duration;
    private long size;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.title);
        dest.writeString(this.album);
        dest.writeString(this.artist);
        dest.writeString(this.path);
        dest.writeString(this.displayName);
        dest.writeString(this.mimeType);
        dest.writeLong(this.duration);
        dest.writeLong(this.size);
    }

    public AudioModel() {
    }

    public AudioModel(int id, String title, String album, String artist,
                 String path, String displayName, String mimeType, long duration,
                 long size) {
        super();
        this.id = id;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.path = path;
        this.displayName = displayName;
        this.mimeType = mimeType;
        this.duration = duration;
        this.size = size;
    }

    protected AudioModel(Parcel in) {
        this.id = in.readInt();
        this.title = in.readString();
        this.album = in.readString();
        this.artist = in.readString();
        this.path = in.readString();
        this.displayName = in.readString();
        this.mimeType = in.readString();
        this.duration = in.readLong();
        this.size = in.readLong();
    }

    public static final Parcelable.Creator<AudioModel> CREATOR = new Parcelable.Creator<AudioModel>() {
        @Override
        public AudioModel createFromParcel(Parcel source) {
            return new AudioModel(source);
        }

        @Override
        public AudioModel[] newArray(int size) {
            return new AudioModel[size];
        }
    };
}
