package com.shaua.mocklocation;

import android.app.Application;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;

import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.printer.ConsolePrinter;
import com.elvishew.xlog.printer.Printer;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;
import com.elvishew.xlog.printer.file.naming.ChangelessFileNameGenerator;

import com.google.android.material.color.DynamicColors;

import java.io.File;

public class GoApplication extends Application {
    public static final String APP_NAME = "迁境";
    public static final String LOG_FILE_NAME = APP_NAME + ".log";
    private static final long MAX_TIME = 1000 * 60 * 60 * 24 * 3; // 3 days

    @Override
    public void onCreate() {
        super.onCreate();

        // Material You 动态配色（Android 12+，将用户壁纸配色应用到应用）
        DynamicColors.applyToActivitiesIfAvailable(this);

        // 清理上一版本残留的更新安装包
        cleanUpdateApkFiles();

        initXlog();

        // 崩溃日志自收集（依赖 XLog 初始化后的日志目录）
        com.shaua.mocklocation.utils.CrashHandler.install(this);

        // 百度地图 7.5 开始，要求必须同意隐私政策，默认为false
        SDKInitializer.setAgreePrivacy(this, true);
        // 百度定位 7.5 开始，要求必须同意隐私政策，默认为false(官方说可以统一为以上接口，但实际测试并不行，定位还是需要单独设置)
        LocationClient.setAgreePrivacy(true);
        SDKInitializer.setApiKey(BuildConfig.MAPS_API_KEY);
        // 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
        SDKInitializer.initialize(this);

        SDKInitializer.setCoordType(CoordType.BD09LL);
    }

    /**
     * Initialize XLog.
     * Release 版本仅记录 WARN 及以上级别，且不输出线程信息/调用栈/边框以减小日志体积
     * Debug 版本记录全部级别并启用详细调试信息
     * 用户可在设置中关闭日志
     */
    private void initXlog() {
        File logPath = getExternalFilesDir("Logs");
        if (logPath == null) return;

        // 检查用户是否关闭了日志
        boolean logOff = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean("setting_log_off", false);

        LogConfiguration.Builder configBuilder = new LogConfiguration.Builder()
                .tag(APP_NAME);

        if (logOff) {
            configBuilder.logLevel(LogLevel.NONE);
        } else if (BuildConfig.DEBUG) {
            // Debug：全量日志 + 详细调试信息
            configBuilder.logLevel(LogLevel.ALL)
                    .enableThreadInfo()
                    .enableStackTrace(2)
                    .enableBorder();
        } else {
            // Release：仅警告及以上，减少日志体积
            configBuilder.logLevel(LogLevel.WARN);
        }

        LogConfiguration config = configBuilder.build();

        Printer consolePrinter = new ConsolePrinter();
        Printer filePrinter = new FilePrinter
                .Builder(logPath.getPath())
                .fileNameGenerator(new ChangelessFileNameGenerator(LOG_FILE_NAME))
                .backupStrategy(new NeverBackupStrategy())
                .cleanStrategy(new FileLastModifiedCleanStrategy(MAX_TIME))
                .build();
        XLog.init(config, consolePrinter, filePrinter);
    }

    /**
     * 清理更新目录中残留的 APK 安装包
     * 更新安装完成并启动新版本后，旧的下载文件不再需要
     */
    private void cleanUpdateApkFiles() {
        File updatesDir = getExternalFilesDir("Updates");
        if (updatesDir != null && updatesDir.exists()) {
            File[] files = updatesDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".apk")) {
                        file.delete();
                    }
                }
            }
        }
    }
}