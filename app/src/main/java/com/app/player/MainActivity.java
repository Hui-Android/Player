package com.app.player;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.app.player.entity.UriModel;
import com.app.player.util.ByteUtil;
import com.app.player.util.ToastUtil;

import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button mAudioNativeBtn, mVideoNativeBtn, mAudioBtn, mVideoBtn;
    private UriModel uriModel;
    private final int REQ_OPEN_FILE_AUDIO = 2332;
    private final int REQ_OPEN_FILE_VIDEO = 2333;
    public static final int PERMISSON_REQUESTCODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                PermissionGen.needPermission(this, PERMISSON_REQUESTCODE,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE});
            }
        }

        mAudioNativeBtn = findViewById(R.id.audio_native_btn);
        mAudioNativeBtn.setOnClickListener(this);
        mVideoNativeBtn = findViewById(R.id.video_native_btn);
        mVideoNativeBtn.setOnClickListener(this);
        mAudioBtn = findViewById(R.id.audio_btn);
        mAudioBtn.setOnClickListener(this);
        mVideoBtn = findViewById(R.id.video_btn);
        mVideoBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.audio_native_btn: //选择本地音频
                Intent audioIntent = new Intent(Intent.ACTION_GET_CONTENT); // 特殊种类的数据，比如相片或者录音
                audioIntent.setType("audio/*");
                startActivityForResult(audioIntent, REQ_OPEN_FILE_AUDIO);
                break;
            case R.id.video_native_btn: //选择本地视频
                Intent videoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                videoIntent.setType("video/*");
                startActivityForResult(videoIntent, REQ_OPEN_FILE_VIDEO);
                break;
            case R.id.audio_btn:
                startActivity(new Intent(this, AudioPlayerActivity.class));
                break;
            case R.id.video_btn:
                startActivity(new Intent(this, VideoPlayerActivity.class));
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null){
            Uri uri = data.getData();
            if (uri != null) {
                uriModel = new UriModel();
                if(requestCode == REQ_OPEN_FILE_AUDIO && resultCode == RESULT_OK){ //音频
                    Cursor cursor = getContentResolver().query(uri,null,null,null,null);
                    if(cursor != null && cursor.moveToNext()){
                        int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                        int sizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
                        String audioTitle = cursor.getString(titleColumn);
                        String audioSize = ByteUtil.formetFileSize(Long.parseLong(cursor.getString(sizeColumn)));
                        uriModel.setAudioTitle(audioTitle);
                        uriModel.setAudioSize(audioSize);
                        cursor.close();
                    }
                    uriModel.setAudioUri(uri);
                    Intent audioIntent = new Intent(this, AudioPlayerActivity.class);
                    audioIntent.putExtra("uriModel", uriModel);
                    startActivity(audioIntent);
                } else if(requestCode == REQ_OPEN_FILE_VIDEO && resultCode == RESULT_OK){   //视频
                    Cursor cursor = getContentResolver().query(uri,null,null,null,null);
                    if(cursor != null && cursor.moveToNext()){
                        int titleColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
                        String videoTitle = cursor.getString(titleColumn);
                        uriModel.setVideoTitle(videoTitle);
                        cursor.close();
                    }
                    uriModel.setVideoUri(uri);
                    Intent videoIntent = new Intent(this, VideoPlayerActivity.class);
                    videoIntent.putExtra("uriModel", uriModel);
                    startActivity(videoIntent);
                }
            }
        }else{
            ToastUtil.showDefaultToast(this,"data is null!");
        }
    }

    @PermissionSuccess(requestCode = PERMISSON_REQUESTCODE)
    public void success(){
        Toast.makeText(MainActivity.this, "授权成功", Toast.LENGTH_SHORT).show();
    }

    @PermissionFail(requestCode = PERMISSON_REQUESTCODE)
    public void fail() {
        Toast.makeText(MainActivity.this, "授权失败", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
}
