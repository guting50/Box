package com.github.tvbox.osc.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.jaredrummler.android.shell.Shell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 打印并保存日志
 * Created by Administrator on 2017/6/12.
 */

public class LogUtils {
    @SuppressLint("StaticFieldLeak")
    private static Context ct;

    public static void init(Context c) {
        ct = c;

        String sdStatus = Environment.getExternalStorageState();
        if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) {
            android.util.Log.e("Log-gt", "SD card is not avaiable/writeable icon_xingge_right now.");
            return;
        }
        startWriteLog();
    }

    public static void v(@Nullable String tag, @NonNull String msg) {
        android.util.Log.v(tag, msg);
        writeLog(tag, msg);
    }

    public static void d(@Nullable String tag, @NonNull String msg) {
        android.util.Log.d(tag, msg);
        writeLog(tag, msg);
    }

    public static void i(@Nullable String tag, @NonNull String msg) {
        android.util.Log.i(tag, msg);
        writeLog(tag, msg);
    }

    public static void w(@Nullable String tag, @NonNull String msg) {
        android.util.Log.w(tag, msg);
        writeLog(tag, msg);
    }

    public static void e(@Nullable String tag, @NonNull String msg) {
        android.util.Log.e(tag, msg);
        writeLog(tag, msg);
    }

    public static void wtf(@Nullable String tag, @NonNull String msg) {
        android.util.Log.wtf(tag, msg);
        writeLog(tag, msg);
    }

    public static void writeLog(@Nullable String tag, @NonNull String msg) {
        writeFile(("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] --->>> ").getBytes() +
                "[" + tag + ":" + msg + "]");
    }

    public static synchronized void writeFile(final String context) {
        new Thread(() -> {
            FileOutputStream fos = null;
            try {
                String GT_LOGS_DIR = ct.getExternalFilesDir(null) + File.separator + "logs";
                File path = new File(GT_LOGS_DIR);
                String str = "gt_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".txt";
                File file = new File(path.getPath() + File.separator + str);
                if (!path.exists()) {
                    path.mkdirs();
                }
                String title = "";
                if (!file.exists()) {
                    file.createNewFile();
                    title = getTitle();
                }
                fos = new FileOutputStream(file, true);
                if (!TextUtils.isEmpty(title)) {
                    fos.write(title.getBytes());
                    fos.write("\n".getBytes());
                }
                fos.write(context.getBytes());
                fos.write("\n".getBytes());
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("Log-gt", "Error on writeFilToSD.");
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }).start();
    }

    private static String getTitle() {
        String versionName = "";
        int versioncode = 0;
        try {
            PackageManager pm = ct.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ct.getPackageName(), 0);
            versionName = pi.versionName;
            versioncode = pi.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        final String version = versioncode + "；" + versionName;

        Map<String, String> param = new HashMap<>();
        param.put("device", "手机型号："
                + android.os.Build.MODEL + "；手机厂商：" + android.os.Build.MANUFACTURER + "；系统版本：" + android.os.Build.VERSION.SDK + "："
                + android.os.Build.VERSION.RELEASE);
        param.put("version", version);
        return new Gson().toJson(param);
    }

    /**
     * 启动写日志
     * 每天一个文件夹
     * 每小时一个文件
     * 每次启动的时候重新创建一个文件
     */
    @SuppressLint("SimpleDateFormat")
    public static void startWriteLog() {
        new Thread(() -> {
            String tag = new SimpleDateFormat("mmss").format(new Date());
            while (true) {
                delete();
                String dirName = new SimpleDateFormat("yyyyMMdd").format(new Date());
                String pathname = ct.getExternalFilesDir(null) + File.separator + "logs" + File.separator + dirName;
                File path = new File(pathname);
                if (!path.exists()) {
                    path.mkdirs();
                }
                String HH = new SimpleDateFormat("HH").format(new Date());
                File file = new File(path.getPath() + File.separator + "gt_" + HH + "_" + tag + ".txt");
                boolean isClear = !file.exists();

                Shell.SH.run("logcat CCodecBuffers:s vpu_api_legacy:s mpp:s chatty:s " +
                        " HidlServiceManagement:s ProvidersAccess:s BufferPoolAccessor2.0:s logd:s " +
                        " -f " + file.getPath() + " -d");
                if (isClear) {
                    Shell.SH.run("logcat -c");
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    startWriteLog();
                }
            }
        }).start();
    }

    private static void delete() {
        File logsDir = new File(ct.getExternalFilesDir(null) + File.separator + "logs");
        if (logsDir.exists()) {
            List<File> lists = new ArrayList(Arrays.asList(Objects.requireNonNull(logsDir.listFiles())));
            for (File file : lists) {
                //删除30天前的日志文件
                if (file.lastModified() < (new Date().getTime() - 30 * 24 * 60 * 60 * 1000L)) {
                    remove(file);
                    file.delete();
                }
            }
        }
    }

    private static void remove(File file) {
        File[] files = file.listFiles();//将file子目录及子文件放进文件数组
        if (files != null) {//如果包含文件进行删除操作
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {//删除子文件
                    files[i].delete();
                } else if (files[i].isDirectory()) {//通过递归方法删除子目录的文件
                    remove(files[i]);
                }
                files[i].delete();//删除子目录
            }
        }
    }
}