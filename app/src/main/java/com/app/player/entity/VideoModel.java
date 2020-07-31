package com.app.player.entity;

public class VideoModel {

    /***
     * 文件名称
     */
    private String fileName;

    /***
     * 文件类型
     * 2：音乐类型
     * 3：视频类型
     */
    private int contentType;

    /***
     * 文件id
     */
    private String fileId;

    /***
     * 本地路径
     */
    private String localPath;

    /***
     * 下载路径(用于判断已下载)
     */
    private String downloadPath;

    /***
     * 内容高清播放格式的URL地址
     */
    private String presentHURL;

    /***
     * 内容流畅播放格式的URL地址
     */
    private String presentLURL;

    /***
     * 内容标准播放格式的URL地址
     */
    private String presentURL;

    /***
     * 文件大小
     */
    private long size;

    /***
     * 缩略图地址
     */
    private String thumbnailURL;

    private String fullPathName;

    /***
     *
     * @param size 文件大小
     */
    public void setSize(long size)
    {
        this.size = size;
    }

    /***
     *
     * @param thumbnailURL 缩略图地址
     */
    public void setThumbnailURL(String thumbnailURL)
    {
        this.thumbnailURL = thumbnailURL;
    }

    /***
     * 获取文件名称
     */
    public String getFileName()
    {
        return fileName;
    }

    /***
     * 设置文件名称
     */
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    /***
     * 获取本地路径
     * 若已经下载过则为下载保存路径
     * 若是直接从本机上传则为上传路径
     */
    public String getLocalPath()
    {
        return localPath;
    }

    /***
     * 设置本地路径
     */
    public void setLocalPath(String localPath)
    {
        this.localPath = localPath;
    }

    /***
     * 获取内容高清播放格式的URL地址
     */
    public String getPresentHURL()
    {
        return presentHURL;
    }

    /***
     * 设置内容高清播放格式的URL地址
     */
    public void setPresentHURL(String presentHURL)
    {
        this.presentHURL = presentHURL;
    }

    /***
     * 获取内容流畅播放格式的URL地址
     */
    public String getPresentLURL()
    {
        return presentLURL;
    }

    /***
     * 设置内容流畅播放格式的URL地址
     */
    public void setPresentLURL(String presentLURL)
    {
        this.presentLURL = presentLURL;
    }

    /***
     * 获取内容标准播放格式的URL地址
     */
    public String getPresentURL()
    {
        return presentURL;
    }

    /***
     * 设置内容标准播放格式的URL地址
     */
    public void setPresentURL(String presentURL)
    {
        this.presentURL = presentURL;
    }

    /***
     * 获取文件类型
     * 2：音乐类型
     * 3：视频类型
     */
    public int getContentType()
    {
        return contentType;
    }

    /***
     * 设置文件类型
     * 2：音乐类型
     * 3：视频类型
     */
    public void setContentType(int contentType)
    {
        this.contentType = contentType;
    }

    /***
     * 文件大小
     *
     */
    public long getSize()
    {
        return size;
    }

    /***
     * 文件的缩略图地址
     *
     */
    public String getThumbnailURL()
    {
        return thumbnailURL;
    }

    public String getFullPathName() {
        return fullPathName;
    }

    public void setFullPathName(String fullPathName) {
        this.fullPathName = fullPathName;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }

    @Override
    public String toString() {
        return "VideoModel [fileName=" + fileName + ", contentType="
                + contentType + ", fileId=" + fileId + ", localPath=" + localPath
                + ", presentHURL=" + presentHURL + ", presentLURL=" + presentLURL
                + ", presentURL=" + presentURL + ", size=" + size + ", thumbnailURL="
                + thumbnailURL + "]";
    }
}
