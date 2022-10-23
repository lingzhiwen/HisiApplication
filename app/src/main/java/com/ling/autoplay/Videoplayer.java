package com.ling.autoplay;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;

import java.io.File;
import java.util.List;

public class Videoplayer extends Activity {
    public static final String TAG = "Videoplayer";
    private int position;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 全屏显示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        playVideo();

    }

    private void playVideo() {
        MyVideoView vv = this.findViewById(R.id.videoView);

        String udisk = FileUtils.getStoragePath(this,true);
        Log.d(TAG,"summer udiskpath="+udisk);
        if(TextUtils.isEmpty(udisk)){
            Toast.makeText(this,"u disk is not exist",Toast.LENGTH_SHORT).show();
            return;
        }
        List<String>  list = FileUtils.getFilesAllName(udisk);
        if(list ==null){
            Toast.makeText(this,"u disk video is not exist",Toast.LENGTH_SHORT).show();
            return;
        }
        String path = list.get(position);
        Log.d(TAG,"summer path="+path);
        vv.setVideoPath(path);
        vv.start(); //开始播放视频
        vv.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(position<list.size()-1) {
                    position = position + 1;
                }else{
                    position = 0;
                }
                Log.d(TAG,"summer onCompletion path="+list.get(position));
                vv.setVideoPath(list.get(position));
                vv.start();
            }
        });
//另一种方式
//        vv.setOnCompletionListener(mp -> {
//           vv.setVideoURI(Uri.parse(uri));
//            vv.start();
//        });
    }
}
