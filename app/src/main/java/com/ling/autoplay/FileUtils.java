package com.ling.autoplay;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.format.Formatter;
import android.util.Log;


import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class FileUtils {
    public static final String TAG = FileUtils.class.getSimpleName();
    private static List<String> s = new ArrayList<>();

    /**
     * 根据label获取外部存储路径(此方法适用于android7.0以上系统)
     * @param context
     * @param label 内部存储:Internal shared storage    SD卡:SD card    USB:USB drive(USB storage)
     */
    public static String getExternalPath(Context context) {
        String path = "";
        StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        //获取所有挂载的设备（内部sd卡、外部sd卡、挂载的U盘）
        List<StorageVolume> volumes = null;//此方法是android 7.0以上的
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            volumes = mStorageManager.getStorageVolumes();
        }
        try {
            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            //通过反射调用系统hide的方法
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
//       Method getUserLabel = storageVolumeClazz.getMethod("getUserLabel");//userLabel和description是一样的
            for (int i = 0; i < volumes.size(); i++) {
                StorageVolume storageVolume = volumes.get(i);//获取每个挂载的StorageVolume
                // 通过反射调用getPath、isRemovable、userLabel
                String storagePath = (String) getPath.invoke(storageVolume); //获取路径
                boolean isRemovableResult = (boolean) isRemovable.invoke(storageVolume);//是否可移除
                String description = null;//此方法是android 7.0以上的
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    description = storageVolume.getDescription(context);
                }
                Log.e("getExternalPath--", "summer i=" + i + " ,storagePath=" + storagePath +  " ,description=" + description);
                path = storagePath;
            }
        } catch (Exception e) {
            Log.e("getExternalPath--", "summer e:" + e);
        }
        return path;
    }

    public static String getStoragePath(Context context, boolean isUsb){
        String path="";
        StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Class<?> volumeInfoClazz;
        Class<?> diskInfoClaszz;
        try {
            volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo");
            diskInfoClaszz = Class.forName("android.os.storage.DiskInfo");
            Method StorageManager_getVolumes=Class.forName("android.os.storage.StorageManager").getMethod("getVolumes");
            Method VolumeInfo_GetDisk = volumeInfoClazz.getMethod("getDisk");
            Method VolumeInfo_GetPath = volumeInfoClazz.getMethod("getPath");
            Method DiskInfo_IsUsb = diskInfoClaszz.getMethod("isUsb");
            Method DiskInfo_IsSd = diskInfoClaszz.getMethod("isSd");
            List<Object> List_VolumeInfo = (List<Object>) StorageManager_getVolumes.invoke(mStorageManager);
            assert List_VolumeInfo != null;
            for(int i=0; i<List_VolumeInfo.size(); i++){
                Object volumeInfo = List_VolumeInfo.get(i);
                Object diskInfo = VolumeInfo_GetDisk.invoke(volumeInfo);
                if(diskInfo==null)continue;
                boolean sd= (boolean) DiskInfo_IsSd.invoke(diskInfo);
                boolean usb= (boolean) DiskInfo_IsUsb.invoke(diskInfo);
                File file= (File) VolumeInfo_GetPath.invoke(volumeInfo);
                if(isUsb == usb){//usb
                    assert file != null;
                    path=file.getAbsolutePath();
                }else if(!isUsb == sd){//sd
                    assert file != null;
                    path=file.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "[——————— ——————— Exception:"+e.getMessage()+"]");
            e.printStackTrace();
        }
        return path;
    }

    public static List<String> getFilesAllName(String path) {

        Log.e(TAG, "summer getFilesAllName="+path);
        File file = new File(path);
        Log.e(TAG, "summer ="+file + "");
        File[] files = file.listFiles();
        //没有权限files会变成null
        Log.e(TAG, files + " <<summer");
        if (files == null) {
            Log.e("error", "没有权限");
            return s;
        }
        for (int i = 0; i < files.length; i++) {
            if(files[i].isDirectory()){
                getFilesAllName(files[i].getAbsolutePath());
            }else if(files[i].getName().contains(".mp4")){
                s.add(files[i].getAbsolutePath());
                Log.e(TAG, files[i].getAbsolutePath() + " <<<<<<<summer");
            }
        }
        return s;
    }
    public static List<File> getUDiskVideoFiles(String path){
        File file = new File(path);
        File[] files = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String name = file.getName();
                Log.d(TAG,"summer name="+name);
                int i = name.indexOf('.');
                if (i != -1) {
                    name = name.substring(i);
                    if (name.equalsIgnoreCase(".mp4")) {
                        Log.i("tga", "summer getPath" + file.getPath());
                        Log.i("tga", "summer getAbsolutePath" + file.getAbsolutePath());
                        return true;
                    }
                }
                return false;
            }
        });
        if(files == null){
            return new ArrayList<>();
        }
        return Arrays.asList(files);
    }

    // 获取当前目录下所有的mp4文件
    public static Vector<String> GetVideoFileName(String fileAbsolutePath) {
        Vector<String> vecFile = new Vector<String>();
        File file = new File(fileAbsolutePath);
        File[] subFile = file.listFiles();

        for (int iFileLength = 0; iFileLength < subFile.length; iFileLength++) {
            // 判断是否为文件夹
            if (!subFile[iFileLength].isDirectory()) {
                String filename = subFile[iFileLength].getName();
                // 判断是否为MP4结尾
                if (filename.trim().toLowerCase().endsWith(".mp4")) {
                    Log.d(TAG,"summer filename="+filename);
                    vecFile.add(filename);
                }
            }
        }
        return vecFile;
    }

    /**
     * 获得SD卡总大小
     *
     * @return
     */
    private String getSDTotalSize(Context context) {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return Formatter.formatFileSize(context, blockSize * totalBlocks);
    }

    /**
     * 获得sd卡剩余容量，即可用大小
     *
     * @return
     */
    private String getSDAvailableSize(Context context) {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return Formatter.formatFileSize(context, blockSize * availableBlocks);
    }

    /**
     * 获取系统内存大小
     * @return
     */
    private String getSysteTotalMemorySize(Context context){
        //获得ActivityManager服务的对象
        ActivityManager mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        //获得MemoryInfo对象
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo() ;
        //获得系统可用内存，保存在MemoryInfo对象上
        mActivityManager.getMemoryInfo(memoryInfo) ;
        long memSize = memoryInfo.totalMem ;
        //字符类型转换
        String availMemStr = Formatter.formatFileSize(context,memSize);
        return availMemStr ;
    }

    /**
     * 获取系统可用的内存大小
     * @return
     */
    private String getSystemAvaialbeMemorySize(Context context){
        //获得ActivityManager服务的对象
        ActivityManager mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        //获得MemoryInfo对象
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo() ;
        //获得系统可用内存，保存在MemoryInfo对象上
        mActivityManager.getMemoryInfo(memoryInfo) ;
        long memSize = memoryInfo.availMem ;

        //字符类型转换
        String availMemStr = Formatter.formatFileSize(context,memSize);

        return availMemStr ;
    }
}
