package com.shaua.mocklocation.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.location.Criteria;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.shaua.mocklocation.database.DataBaseHistorySearch;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GoUtils {

    /** 获取当前主题的强调色，用于对话框按钮等需随主题变色的场景 */
    public static int getThemeAccentColor(Context context) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorPrimary});
        int color = a.getColor(0, 0xFF006A61);
        a.recycle();
        return color;
    }

    /** 获取当前主题的深色主色，用于状态栏/系统导航栏 */
    public static int getThemePrimaryDarkColor(Context context) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorPrimaryContainer});
        int color = a.getColor(0, 0xFF00514A);
        a.recycle();
        return color;
    }

    /** 获取当前主题的主色，用于 Toolbar、底部导航选中态等 */
    public static int getThemePrimaryColor(Context context) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorPrimary});
        int color = a.getColor(0, 0xFF006A61);
        a.recycle();
        return color;
    }

    /** 获取当前主题的 surface 上的次要文字色，用于对话框次要按钮等 */
    public static int getThemeOnSurfaceVariantColor(Context context) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorOnSurfaceVariant});
        int color = a.getColor(0, 0xFF757575);
        a.recycle();
        return color;
    }

    /** 获取当前主题的主色上的文字色，用于 Toolbar 标题等 */
    public static int getThemeOnPrimaryColor(Context context) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorOnPrimary});
        int color = a.getColor(0, 0xFFFFFFFF);
        a.recycle();
        return color;
    }

    public static boolean isDeveloperOptionsEnabled(Context context) {
        return Settings.Global.getInt(
                context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
        ) == 1;
    }

    // WIFI是否可用
    public static boolean isWifiConnected(Context context) {
        // 从 API 29 开始，NetworkInfo 被标记为过时，这里更换新方法
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) {
            return false;
        }
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
    }

    public static boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    //MOBILE网络是否可用
    public static boolean isMobileConnected(Context context) {
        // 从 API 29 开始，NetworkInfo 被标记为过时，这里更换新方法
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) {
            return false;
        }
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    // 断是否有网络连接，但是如果该连接的网络无法上网，也会返回true
    public static boolean isNetworkConnected(Context context) {
        // 从 API 29 开始，NetworkInfo 被标记为过时，这里更换新方法
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    //网络是否可用
    public static boolean isNetworkAvailable(Context context) {
        return ((isWifiConnected(context) || isMobileConnected(context)) && isNetworkConnected(context));
    }

    //判断GPS是否打开
    public static  boolean isGpsOpened(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // 判断是否已在开发者选项中开启模拟位置权限（注意下面临时添加 @SuppressLint("wrongconstant") 以处理 addTestProvider 参数值的 lint 错误）
    @SuppressLint("wrongconstant")
    public static boolean isAllowMockLocation(Context context) {
        boolean canMockPosition = false;
        int index;

        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);//获得LocationManager引用

            List<String> list = locationManager.getAllProviders();
            for (index = 0; index < list.size(); index++) {
                if (list.get(index).equals(LocationManager.GPS_PROVIDER)) {
                    break;
                }
            }

            if (index < list.size()) {
                // 注意，由于 android api 问题，下面的参数会提示错误(以下参数是通过相关API获取的真实GPS参数，不是随便写的)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                            false, true, true, true, ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE);
                } else {
                    locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                            false, true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
                }
                canMockPosition = true;
            }

            // 模拟位置可用
            if (canMockPosition) {
                // remove test provider
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return canMockPosition;
    }

    /**
     * [获取应用程序版本名称]
     * @param context context
     * @return 当前应用的版本名称
     */
    public static synchronized String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();

            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);

            return packageInfo.versionName;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取App的名称
     * @param context 上下文
     * @return 名称
     */
    public static String getAppName(Context context) {
        PackageManager pm = context.getPackageManager();
        //获取包信息
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            int labelRes = applicationInfo.labelRes;
            return context.getResources().getString(labelRes);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static  String timeStamp2Date(String seconds) {
        if (seconds == null || seconds.isEmpty() || seconds.equals("null")) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        return sdf.format(new Date(Long.parseLong(seconds + "000")));
    }

    //提醒开启位置模拟的弹框
    public static  void showEnableMockLocationDialog(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("启用位置模拟")
                .setMessage("请在\"开发者选项→选择模拟位置信息应用\"中进行设置")
                .setPositiveButton("设置",(dialogInterface, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("取消",(dialogInterface, which) -> {
                })
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getThemeAccentColor(context));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getThemeOnSurfaceVariantColor(context));
    }

    //显示开启GPS的提示
    public static  void showEnableGpsDialog(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("启用定位服务")
                .setMessage("是否开启 GPS 定位服务?")
                .setPositiveButton("确定",(dialogInterface, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        context.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("取消",(dialogInterface, which) -> {

                })
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getThemeAccentColor(context));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getThemeOnSurfaceVariantColor(context));
    }

    // 提醒关闭 WIFI 的弹框
    public static  void showDisableWifiDialog(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("WIFI 提示")
                .setMessage("开启 WIFI 后（即使没有连接热点）可能导致定位闪回真实位置。\n建议关闭 WIFI，使用移动流量进行游戏！")
                .setPositiveButton("去关闭",(dialogInterface, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        context.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("忽略", (dialogInterface, which) -> dialogInterface.dismiss())
                .create();
        dialog.show();
        // 强制设置按钮文字颜色，避免主题导致按钮不可见
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getThemeAccentColor(context));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getThemeOnSurfaceVariantColor(context));
    }

    public static  void DisplayToast(Context context, String str) {
        Toast toast = Toast.makeText(context, str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 100);
        toast.show();
    }

    /* 计数器类 */
    public static class TimeCount extends CountDownTimer {
        private TimeCountListener mListener;

        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);//参数依次为总时长,和计时的时间间隔
        }

        @Override
        public void onFinish() {//计时完毕时触发
            mListener.onFinish();
        }

        @Override
        public void onTick(long millisUntilFinished) { //计时过程显示
            mListener.onTick(millisUntilFinished);
        }

        public void setListener(TimeCountListener mListener) {
            this.mListener = mListener;
        }

        public interface TimeCountListener {
            void onTick(long millisUntilFinished);
            void onFinish();
        }
    }

    /* ============================== 缓存清理 相关 ============================== */

    /**
     * 计算应用可清理缓存的总大小（字节）
     * 包括：内部缓存、外部缓存、日志文件、更新安装包
     */
    public static long getCacheSize(Context context) {
        long size = 0;
        size += getDirSize(context.getCacheDir());
        File externalCache = context.getExternalCacheDir();
        if (externalCache != null) size += getDirSize(externalCache);
        File logDir = context.getExternalFilesDir("Logs");
        if (logDir != null) size += getDirSize(logDir);
        File updateDir = context.getExternalFilesDir("Updates");
        if (updateDir != null) size += getDirSize(updateDir);
        return size;
    }

    /**
     * 清理应用缓存
     * 包括：内部缓存、外部缓存、日志文件、更新安装包、搜索历史
     * @return 实际清理的字节数
     */
    public static long clearCache(Context context) {
        long before = getCacheSize(context);

        // 清理内部缓存（含百度地图瓦片缓存）
        deleteDirContents(context.getCacheDir());
        // 清理外部缓存
        File externalCache = context.getExternalCacheDir();
        if (externalCache != null) deleteDirContents(externalCache);
        // 清理日志文件
        File logDir = context.getExternalFilesDir("Logs");
        if (logDir != null) deleteDirContents(logDir);
        // 清理更新安装包
        File updateDir = context.getExternalFilesDir("Updates");
        if (updateDir != null) deleteDirContents(updateDir);
        // 清理搜索历史
        try {
            DataBaseHistorySearch dbHelper = new DataBaseHistorySearch(context.getApplicationContext());
            DataBaseHistorySearch.deleteAll(dbHelper.getWritableDatabase());
            dbHelper.close();
        } catch (Exception e) {
            // 忽略
        }

        long after = getCacheSize(context);
        return before - after;
    }

    /**
     * 格式化字节数为可读字符串（如 1.2 MB）
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /** 递归计算目录大小 */
    private static long getDirSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += getDirSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    /** 删除目录下所有内容，保留目录本身 */
    private static void deleteDirContents(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirContents(file);
                    file.delete();
                } else {
                    file.delete();
                }
            }
        }
    }

    /* ============================== 应用图标切换 相关 ============================== */

    private static final String PKG = "com.shaua.mocklocation";
    private static final String[] ALIAS_COMPONENTS = {
            PKG + ".WelcomeActivityAliasDefault",
            PKG + ".WelcomeActivityAliasIcon1",
            PKG + ".WelcomeActivityAliasIcon2",
            PKG + ".WelcomeActivityAliasIcon3",
            PKG + ".WelcomeActivityAliasIcon4"
    };

    /**
     * 切换应用桌面图标
     * @param context 上下文
     * @param iconValue 图标标识：default / icon1 / icon2 / icon3 / icon4
     */
    public static void switchAppIcon(Context context, String iconValue) {
        int targetIndex;
        switch (iconValue) {
            case "icon1": targetIndex = 1; break;
            case "icon2": targetIndex = 2; break;
            case "icon3": targetIndex = 3; break;
            case "icon4": targetIndex = 4; break;
            default: targetIndex = 0; break;
        }
        PackageManager pm = context.getPackageManager();
        // 禁用所有别名入口
        for (int i = 0; i < ALIAS_COMPONENTS.length; i++) {
            pm.setComponentEnabledSetting(
                    new android.content.ComponentName(context, ALIAS_COMPONENTS[i]),
                    i == targetIndex
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    /**
     * 获取当前启用的图标标识（用于回显）
     */
    public static String getCurrentAppIcon(Context context) {
        PackageManager pm = context.getPackageManager();
        for (int i = 0; i < ALIAS_COMPONENTS.length; i++) {
            int state = pm.getComponentEnabledSetting(
                    new android.content.ComponentName(context, ALIAS_COMPONENTS[i]));
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                switch (i) {
                    case 1: return "icon1";
                    case 2: return "icon2";
                    case 3: return "icon3";
                    case 4: return "icon4";
                    default: return "default";
                }
            }
        }
        return "default";
    }
}