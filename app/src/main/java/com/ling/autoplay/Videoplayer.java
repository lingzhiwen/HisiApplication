package com.ling.autoplay;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;

public class Videoplayer extends Activity {
    public static final String TAG = "Videoplayer";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 全屏显示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        MyVideoView vv = this.findViewById(R.id.videoView);
        String uri = "android.resource://" + getPackageName() + "/" + R.raw.output;
        vv.setVideoURI(Uri.parse(uri));
        vv.start();
        vv.setOnPreparedListener(mp -> {
            mp.start();
            mp.setLooping(true);
        });
//另一种方式
//        vv.setOnCompletionListener(mp -> {
//           vv.setVideoURI(Uri.parse(uri));
//            vv.start();
//        });
    }

    /**
     * 获得SD卡总大小
     *
     * @return
     */
    private String getSDTotalSize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return Formatter.formatFileSize(this, blockSize * totalBlocks);
    }

    /**
     * 获得sd卡剩余容量，即可用大小
     *
     * @return
     */
    private String getSDAvailableSize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return Formatter.formatFileSize(this, blockSize * availableBlocks);
    }

    /**
     * 获取系统内存大小
     * @return
     */
    private String getSysteTotalMemorySize(){
        //获得ActivityManager服务的对象
        ActivityManager mActivityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        //获得MemoryInfo对象
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo() ;
        //获得系统可用内存，保存在MemoryInfo对象上
        mActivityManager.getMemoryInfo(memoryInfo) ;
        long memSize = memoryInfo.totalMem ;
        //字符类型转换
        String availMemStr = Formatter.formatFileSize(this,memSize);
        return availMemStr ;
    }

    /**
     * 获取系统可用的内存大小
     * @return
     */
    private String getSystemAvaialbeMemorySize(){
        //获得ActivityManager服务的对象
        ActivityManager mActivityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        //获得MemoryInfo对象
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo() ;
        //获得系统可用内存，保存在MemoryInfo对象上
        mActivityManager.getMemoryInfo(memoryInfo) ;
        long memSize = memoryInfo.availMem ;

        //字符类型转换
        String availMemStr = Formatter.formatFileSize(this,memSize);

        return availMemStr ;
    }
}
