package com.shaua.mocklocation.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.elvishew.xlog.XLog;
import com.shaua.mocklocation.GoApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * 崩溃日志自收集
 * <p>
 * 捕获未处理异常后，将崩溃报告写入独立文件（保留最近 5 份），
 * 同时追加到主日志文件，使日志查看页与分享自动包含崩溃信息。
 * 处理完毕后交还系统默认处理器结束进程。
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final int MAX_CRASH_FILES = 5;

    private final Context mContext;
    private final Thread.UncaughtExceptionHandler mDefaultHandler;
    private final File mLogDir;

    private CrashHandler(Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        mLogDir = context.getExternalFilesDir("Logs");
    }

    /** 在 Application.onCreate 中安装，须在 XLog 初始化之后调用 */
    public static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }

    /** 获取最近一次崩溃报告文件，不存在时返回 null */
    public static File getLatestCrashFile(Context context) {
        File[] files = listCrashFiles(context);
        return (files == null || files.length == 0) ? null : files[0];
    }

    private static File[] listCrashFiles(Context context) {
        File logDir = context.getExternalFilesDir("Logs");
        if (logDir == null || !logDir.exists()) {
            return new File[0];
        }
        File[] files = logDir.listFiles((dir, name) -> name.startsWith("crash_") && name.endsWith(".txt"));
        if (files == null) {
            return new File[0];
        }
        // 按文件名（含时间戳）倒序，最新的在前
        Arrays.sort(files, Comparator.comparing(File::getName).reversed());
        return files;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            String report = buildReport(thread, throwable);
            writeCrashFile(report);
            appendToMainLog(report);
        } catch (Exception e) {
            // 收集失败不能影响系统默认崩溃流程
            XLog.e("CrashHandler: collect failed - " + e.getMessage());
        }
        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, throwable);
        }
    }

    private String buildReport(Thread thread, Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));

        StringBuilder sb = new StringBuilder();
        sb.append("===== 崩溃报告 =====\n");
        sb.append("时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append('\n');
        sb.append("应用版本: ").append(getVersionName()).append(" (").append(getVersionCode()).append(")\n");
        sb.append("系统版本: Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("设备型号: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        sb.append("崩溃线程: ").append(thread.getName()).append('\n');
        sb.append("\n---- 堆栈信息 ----\n");
        sb.append(sw);
        return sb.toString();
    }

    private void writeCrashFile(String report) throws Exception {
        if (mLogDir == null) {
            return;
        }
        String fileName = "crash_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".txt";
        try (FileWriter writer = new FileWriter(new File(mLogDir, fileName))) {
            writer.write(report);
        }
        cleanOldCrashFiles();
    }

    /** 崩溃信息追加到主日志，日志查看页与分享即可包含崩溃现场 */
    private void appendToMainLog(String report) {
        if (mLogDir == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(new File(mLogDir, GoApplication.LOG_FILE_NAME), true)) {
            writer.write("\n" + report + "\n");
        } catch (Exception ignored) {
        }
    }

    private void cleanOldCrashFiles() {
        File[] files = listCrashFiles(mContext);
        for (int i = MAX_CRASH_FILES; i < files.length; i++) {
            files[i].delete();
        }
    }

    private String getVersionName() {
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

    private long getVersionCode() {
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }
}
