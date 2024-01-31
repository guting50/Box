package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.ui.dialog.AboutDialog;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static CrashHandler mInstance;

    public static CrashHandler getInstance() {
        if (mInstance == null) {
            synchronized (CrashHandler.class) {
                if (null == mInstance) {
                    mInstance = new CrashHandler();
                }
            }
        }
        return mInstance;
    }

    public void init() {
        Thread.setDefaultUncaughtExceptionHandler(mInstance);
        new Handler(Looper.getMainLooper()).post(() -> {
            while (true) {
                try {
                    Looper.loop();
                } catch (Throwable e) {
                    uncaughtException(Thread.currentThread(), e);
                }
            }
        });
    }

    @Override
    public void uncaughtException(@NotNull Thread thread, @NotNull Throwable ex) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        //如果异常时在AsyncTask里面的后台线程抛出的
        //那么实际的异常仍然可以通过getCause获得
        Throwable cause = ex;
        while (null != cause) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        //stacktraceAsString就是获取的carsh堆栈信息
        final String stacktraceAsString = result.toString();
        printWriter.close();

        if (!TextUtils.isEmpty(stacktraceAsString)) {
            LogUtils.writeFile(stacktraceAsString);
            Log.e("MyExceptionHandler", stacktraceAsString);
            ToastUtils.showLong("很抱歉，程序出现异常，请联系管理员!\n" + stacktraceAsString);
            return;
        }
        Log.e("MyExceptionHandler", "很抱歉，程序出现异常，请联系管理员!");

        ToastUtils.showLong("很抱歉，程序出现异常，请联系管理员!");

        //干掉当前的程序
//        android.os.Process.killProcess(android.os.Process.myPid());
////        // 重启应用
//        ContextHelp.getContext().startActivity(
//                ContextHelp.getContext().getPackageManager()
//                        .getLaunchIntentForPackage(ContextHelp.getContext().getPackageName()));
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
    }
}
