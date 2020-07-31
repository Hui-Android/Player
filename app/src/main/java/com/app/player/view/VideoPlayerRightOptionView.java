package com.app.player.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.app.player.R;


public class VideoPlayerRightOptionView extends RelativeLayout implements View.OnClickListener {

    public static final String INTERFACE_TAG_DELETE = "delete";
    public static final String INTERFACE_TAG_DOWNLOAD = "download";
    public static final String INTERFACE_TAG_SHARE = "share";

    private LinearLayout mRightOptionView;
    private LinearLayout imgRightShare, imgRightDownload, imgRightDelete;

    private TextView tvImgRightVideoDown, tvRightShare;
    private ImageView ivImgRightVideoDown;

    private VideoPlayerRightOptionViewClickListener clickListener;

    public VideoPlayerRightOptionView(Context context) {
        this(context, null);
    }

    public VideoPlayerRightOptionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayerRightOptionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (clickListener == null) {
            return;
        }

        switch (id){
            case R.id.normal_right_img_copy_share: //分享
                clickListener.doMediaAction(INTERFACE_TAG_SHARE);
                break;
            case R.id.normal_right_img_download:  //下载
                clickListener.doMediaAction(INTERFACE_TAG_DOWNLOAD);
                break;
            case R.id.normal_right_img_delete:    //删除
                clickListener.doMediaAction(INTERFACE_TAG_DELETE);
                break;
             default:
                 break;
        }
    }

    private void initView(Context context){
        RelativeLayout rootView = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.ss_video_right_option_view, this);

        mRightOptionView = rootView.findViewById(R.id.layout_right_normal);

        //分享
        imgRightShare = rootView.findViewById(R.id.normal_right_img_copy_share);
        tvRightShare = rootView.findViewById(R.id.normal_right_tv_copy_share);
        // 下载
        imgRightDownload = rootView.findViewById(R.id.normal_right_img_download);
        tvImgRightVideoDown = rootView.findViewById(R.id.tv_ss_video_dwon);
        ivImgRightVideoDown = rootView.findViewById(R.id.iv_ss_video_dwon);
        // 删除
        imgRightDelete = rootView.findViewById(R.id.normal_right_img_delete);

        imgRightShare.setOnClickListener(this);
        imgRightDownload.setOnClickListener(this);
        imgRightDelete.setOnClickListener(this);
    }

    public void setClickListener(VideoPlayerRightOptionViewClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface VideoPlayerRightOptionViewClickListener {
        void doMediaAction(String tag);
    }
}
