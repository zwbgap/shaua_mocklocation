package com.shaua.mocklocation.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.elvishew.xlog.XLog;
import com.shaua.mocklocation.MainActivity;
import com.shaua.mocklocation.R;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * 模拟定位前台服务
 * <p>
 * 通过 {@link LocationManager#addTestProvider} 同时向 GPS、NETWORK、FUSED 三个 Provider
 * 注入伪造的 Location，使系统及其他应用读取到模拟位置。
 * <p>
 * 对抗检测能力：
 * <ul>
 *   <li>反射隐藏 Location 的 mock 标记，规避 isFromMockProvider()/isMock() 检测；</li>
 *   <li>GPS 信号拟真：精度 4~8m 微波动、坐标随机微动（±2m 内）、卫星数 6~12 随机；</li>
 *   <li>海拔 ±1m / 航向 ±3° 微波动；</li>
 *   <li>Android 12+ 同时注入 FUSED Provider，覆盖融合定位读取路径。</li>
 * </ul>
 * <p>
 * 能力边界：
 * <ul>
 *   <li>仅影响走系统 LocationManager API 路径的应用；</li>
 *   <li>若目标应用使用基站/WiFi 扫描、IP 地理、传感器等多源校验，仍可能“闪回”真实位置。</li>
 * </ul>
 * <p>
 * 首帧与刷新：服务启动时同步注入首帧，消除启动空窗；通知栏「刷新定位」Action 会重新注册
 * test provider 并立即重注入一帧，用于目标应用首次定位读到真实位置时快速刷新，
 * 无需回到应用手动切换地图图层。
 */
public class MockLocationService extends Service {

    public static final double DEFAULT_LAT = 36.667662;
    public static final double DEFAULT_LNG = 117.027707;
    public static final double DEFAULT_ALT = 55.0D;
    public static final float DEFAULT_BEA = 0.0F;
    public static final float DEFAULT_SPEED = 0.0F;

    /** 通知栏「刷新定位」Action：重新注册 test provider 并立即注入一帧 */
    public static final String ACTION_REFRESH_LOCATION =
            "com.shaua.mocklocation.action.REFRESH_LOCATION";

    /** P0-4：bind Intent 携带坐标，消除 bindService / startForegroundService 之间的时序窗口 */
    public static final String EXTRA_BIND_COORDS = "EXTRA_BIND_COORDS";

    /** P0-3：目标坐标持久化文件（服务被系统杀掉后重启时恢复） */
    private static final String PREFS_MOCK_STATE = "mock_state";
    private static final String KEY_MOCK_LAT = "mock_lat";
    private static final String KEY_MOCK_LNG = "mock_lng";
    private static final String KEY_MOCK_ALT = "mock_alt";
    private static final String KEY_MOCK_SAVED = "mock_saved";

    private static final int HANDLER_MSG_ID = 0;
    private static final String SERVICE_HANDLER_NAME = "MockLocationThread";
    private static final int SERVICE_NOTE_ID = 1;
    private static final String SERVICE_NOTE_CHANNEL_ID = "MOCK_LOCATION_NOTE";
    private static final String SERVICE_NOTE_CHANNEL_NAME = "MOCK_LOCATION_NOTE";
    /** 融合定位 Provider 名称（Android 12+ 原生存在） */
    private static final String FUSED_PROVIDER_NAME = "fused";
    /** 刷新提示文案的显示时长（毫秒），到时恢复常驻状态文案 */
    private static final long REFRESH_TIPS_DURATION_MS = 2000L;

    /* P0-2：坐标与运行状态跨线程可见（主线程写、HandlerThread 读）；
       写入统一经 applyCoordinates 在 mInjectLock 内完成，读端在 injectOneFrame 内加锁。 */
    private volatile double mCurLat = DEFAULT_LAT;
    private volatile double mCurLng = DEFAULT_LNG;
    private volatile double mCurAlt = DEFAULT_ALT;
    private float mCurBea = DEFAULT_BEA;

    /* GPS 拟真参数：精度波动、坐标随机游走偏移 */
    private final Random mRandom = new Random();
    private float mCurAccuracy = 12.0F;
    private double mJitterLat = 0D;
    private double mJitterLng = 0D;
    private int mJitterTick = 0;
    /* 海拔/航向微波动：避免恒定值特征 */
    private float mAltJitter = 0F;
    private float mBeaJitter = 0F;

    /* 每帧微动：保证每帧坐标/精度都不同，避免被客户端判定为"位置未变化" */
    private double mFrameJitterLat = 0D;
    private double mFrameJitterLng = 0D;
    private float mFrameAccuracy = 12.0F;

    /* 速度拟真：按基准坐标位移计算，静止时给少量噪声 */
    private float mCurSpeed = DEFAULT_SPEED;
    private double mLastBaseLat = DEFAULT_LAT;
    private double mLastBaseLng = DEFAULT_LNG;
    private long mLastSpeedNanos = 0L;

    /* 运行状态可观测性：融合覆盖是否接管、连续注入失败次数 */
    private volatile boolean mFusedRegistered = false;
    private int mInjectFailCount = 0;
    private boolean mInjectErrorNotified = false;
    /** 连续注入失败达到该次数（约 2 秒）后提示异常 */
    private static final int INJECT_FAIL_THRESHOLD = 20;

    /* 反隐藏标记：缓存解析出的隐藏 setter，避免每次注入都走反射查找 */
    private Method mHiddenMockSetter;
    private boolean mMockSetterResolved = false;

    private LocationManager mLocManager;
    private HandlerThread mLocHandlerThread;
    private Handler mLocHandler;
    /** 主线程 Handler：用于刷新提示文案延时恢复 */
    private Handler mMainHandler;
    /** P0-2：onDestroy 写、HandlerThread 读，必须 volatile */
    private volatile boolean isStop = false;

    /**
     * 是否已收到目标坐标。避免 HandlerThread 首帧在 onStartCommand 之前
     * 用 DEFAULT_LAT/LNG 注入一帧错误坐标；也用于循环判断是否允许注入。
     */
    private volatile boolean mStarted = false;

    /**
     * P0-1：串行化「provider 重注册 + 坐标写入 + 注入」整个临界区。
     * 通知刷新、启动首帧、Binder.setPosition、HandlerThread 循环均可能并发进入，
     * 若重注册与注入不互斥，会出现 provider 被 remove 期间 setTestProviderLocation 抛异常丢帧。
     */
    private final Object mInjectLock = new Object();

    private final MockLocationBinder mBinder = new MockLocationBinder();

    @Override
    public IBinder onBind(Intent intent) {
        // P0-4：bindService(BIND_AUTO_CREATE) 会先触发 onCreate，此时若 bind Intent 已带坐标，
        // 立即应用并注入首帧，消除「服务已运行但尚无目标坐标」的窗口（旧实现依赖随后
        // startForegroundService 传坐标，两次调用间存在时序窗口）。
        if (intent != null && intent.getBooleanExtra(EXTRA_BIND_COORDS, false)) {
            double lng = intent.getDoubleExtra(MainActivity.LNG_MSG_ID, DEFAULT_LNG);
            double lat = intent.getDoubleExtra(MainActivity.LAT_MSG_ID, DEFAULT_LAT);
            double alt = intent.getDoubleExtra(MainActivity.ALT_MSG_ID, DEFAULT_ALT);
            applyCoordinates(lng, lat, alt, true);
        }
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        XLog.i("MockLocationService: onCreate");

        mLocManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mMainHandler = new Handler(Looper.getMainLooper());

        synchronized (mInjectLock) {
            removeTestProviderNetwork();
            addTestProviderNetwork();
            removeTestProviderGPS();
            addTestProviderGPS();
            removeTestProviderFused();
            addTestProviderFused();
        }

        initGoLocation();
        initNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_REFRESH_LOCATION.equals(intent.getAction())) {
            // B：通知栏「刷新定位」——重新注册可能被系统回收的 test provider，并立即注入一帧
            refreshFromNotification();
            return START_STICKY;
        }
        if (intent != null) {
            double lng = intent.getDoubleExtra(MainActivity.LNG_MSG_ID, DEFAULT_LNG);
            double lat = intent.getDoubleExtra(MainActivity.LAT_MSG_ID, DEFAULT_LAT);
            double alt = intent.getDoubleExtra(MainActivity.ALT_MSG_ID, DEFAULT_ALT);
            XLog.i("MockLocationService: start with lat=" + lat + ", lng=" + lng + ", alt=" + alt);
            // C：首帧同步注入，消除启动空窗（原循环首帧需等 100ms，且可能注入默认坐标）
            applyCoordinates(lng, lat, alt, true);
        } else {
            // P0-3：START_STICKY 被系统重启时 intent 为 null，恢复上次保存的模拟坐标并立即注入，
            // 避免坐标退回 DEFAULT_LAT/LNG（济南）且 mStarted=false 导致静默丢失模拟位置。
            if (restoreSavedCoordinates()) {
                XLog.i("MockLocationService: restored saved coordinates after restart");
                injectOneFrame();
            } else {
                XLog.w("MockLocationService: restart with null intent and no saved coordinates");
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        XLog.i("MockLocationService: onDestroy");
        isStop = true;
        mLocHandler.removeMessages(HANDLER_MSG_ID);
        mLocHandlerThread.quit();

        synchronized (mInjectLock) {
            removeTestProviderNetwork();
            removeTestProviderGPS();
            removeTestProviderFused();
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    /**
     * P0-2/P0-1：统一写入坐标并立即注入首帧，整个临界区在 mInjectLock 内完成。
     * 坐标写入前已持久化（P0-3），服务重启可恢复。
     */
    private void applyCoordinates(double lng, double lat, double alt, boolean injectNow) {
        synchronized (mInjectLock) {
            mCurLng = lng;
            mCurLat = lat;
            mCurAlt = alt;
            mStarted = true;
            mLastSpeedNanos = 0L;
            saveCoordinates(lng, lat, alt);
            if (injectNow) {
                injectFrameLocked();
            }
        }
    }

    /** P0-3：持久化目标坐标，供 START_STICKY 重启后恢复 */
    private void saveCoordinates(double lng, double lat, double alt) {
        SharedPreferences sp = getSharedPreferences(PREFS_MOCK_STATE, MODE_PRIVATE);
        sp.edit()
                .putString(KEY_MOCK_LAT, Double.toString(lat))
                .putString(KEY_MOCK_LNG, Double.toString(lng))
                .putString(KEY_MOCK_ALT, Double.toString(alt))
                .putBoolean(KEY_MOCK_SAVED, true)
                .apply();
    }

    /** P0-3：恢复上次保存的坐标；无有效记录时返回 false */
    private boolean restoreSavedCoordinates() {
        SharedPreferences sp = getSharedPreferences(PREFS_MOCK_STATE, MODE_PRIVATE);
        if (!sp.getBoolean(KEY_MOCK_SAVED, false)) {
            return false;
        }
        try {
            double lat = Double.parseDouble(sp.getString(KEY_MOCK_LAT, ""));
            double lng = Double.parseDouble(sp.getString(KEY_MOCK_LNG, ""));
            double alt = Double.parseDouble(sp.getString(KEY_MOCK_ALT, Double.toString(DEFAULT_ALT)));
            if (Double.isNaN(lat) || Double.isNaN(lng) || Double.isNaN(alt)) {
                return false;
            }
            applyCoordinates(lng, lat, alt, false);
            return true;
        } catch (Exception e) {
            XLog.w("MockLocationService: restoreSavedCoordinates failed - " + e.getMessage());
            return false;
        }
    }

    /**
     * 用户主动停止模拟时清除持久化状态，避免服务被系统重启后误恢复一个已停止的位置。
     * 由 MainActivity.stopGoLocation() 调用。
     */
    public static void clearSavedCoordinates(Context context) {
        context.getSharedPreferences(PREFS_MOCK_STATE, MODE_PRIVATE)
                .edit().remove(KEY_MOCK_SAVED).apply();
    }

    private void initNotification() {
        NotificationChannel mChannel = new NotificationChannel(
                SERVICE_NOTE_CHANNEL_ID,
                SERVICE_NOTE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(mChannel);
        }

        // D：初始文案即反映融合覆盖状态（fused 已在 onCreate 中尝试注册完毕）
        Notification notification = buildNotification(buildStatusText());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(SERVICE_NOTE_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(SERVICE_NOTE_ID, notification);
        }
    }

    private Notification buildNotification(String text) {
        Intent clickIntent = new Intent(this, MainActivity.class);
        PendingIntent clickPI = PendingIntent.getActivity(
                this, 1, clickIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SERVICE_NOTE_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(text)
                .setContentIntent(clickPI)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true);

        // B：常驻「刷新定位」Action，进目标应用前点一下即可重新注入，无需回到应用切图层
        Intent refreshIntent = new Intent(this, MockLocationService.class)
                .setAction(ACTION_REFRESH_LOCATION);
        PendingIntent refreshPI = PendingIntent.getForegroundService(
                this, 2, refreshIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ic_position,
                getResources().getString(R.string.app_action_refresh), refreshPI);

        // D：融合定位未接管时常驻「定位设置」Action，引导关闭「提高精确度」（Wi-Fi/蓝牙扫描）
        if (fusedCoverageMissing()) {
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            if (settingsIntent.resolveActivity(getPackageManager()) != null) {
                PendingIntent settingsPI = PendingIntent.getActivity(
                        this, 3, settingsIntent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(R.drawable.ic_map,
                        getResources().getString(R.string.app_action_location_settings), settingsPI);
            }
        }
        return builder.build();
    }

    /** 更新前台通知文案，用于暴露融合覆盖/注入异常状态 */
    private void updateServiceNotification(String text) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(SERVICE_NOTE_ID, buildNotification(text));
        }
    }

    /** D：常驻状态文案按优先级：注入异常 > 融合覆盖不可用 > 普通运行提示（判定逻辑抽到 ServiceState 便于单测） */
    private String buildStatusText() {
        ServiceState.NoticeStatus status = ServiceState.noticeStatus(
                mInjectErrorNotified, mFusedRegistered, Build.VERSION.SDK_INT);
        switch (status) {
            case INJECT_ERROR:
                return getResources().getString(R.string.app_service_tips_error);
            case FUSED_MISSING:
                return getResources().getString(R.string.app_fused_unavailable_service);
            case NORMAL:
            default:
                return getResources().getString(R.string.app_service_tips);
        }
    }

    /** Android 12+ 未成功接管 fused Provider 时返回 true（融合覆盖不可用） */
    private boolean fusedCoverageMissing() {
        return ServiceState.fusedCoverageMissing(mFusedRegistered, Build.VERSION.SDK_INT);
    }

    /**
     * B：通知栏「刷新定位」Action 的处理逻辑。
     * 部分 ROM / 系统会在应用退到后台后回收 test provider，这里重新注册并立即注入一帧，
     * 抢占系统 getLastKnownLocation() 缓存，等价于「切图层重读系统位置」且无需回到应用。
     */
    private void refreshFromNotification() {
        if (!mStarted) {
            XLog.w("MockLocationService: refresh ignored - service not started yet");
            return;
        }
        // P0-1：重注册 provider 与注入必须落在同一把锁内，否则 HandlerThread 循环帧可能在
        // provider 被 remove 的瞬间调用 setTestProviderLocation 而抛异常丢帧。
        synchronized (mInjectLock) {
            reRegisterTestProviders();
            injectFrameLocked();
        }
        updateServiceNotification(getResources().getString(R.string.app_service_tips_refreshed));
        XLog.i("MockLocationService: location refreshed from notification action");
        mMainHandler.postDelayed(() -> {
            if (!isStop) {
                updateServiceNotification(buildStatusText());
            }
        }, REFRESH_TIPS_DURATION_MS);
    }

    /** 重新注册三个 test provider，修复被系统回收后注入失效的问题。调用方须持有 mInjectLock */
    private void reRegisterTestProviders() {
        removeTestProviderNetwork();
        addTestProviderNetwork();
        removeTestProviderGPS();
        addTestProviderGPS();
        removeTestProviderFused();
        addTestProviderFused();
    }

    private void initGoLocation() {
        mLocHandlerThread = new HandlerThread(SERVICE_HANDLER_NAME, Process.THREAD_PRIORITY_FOREGROUND);
        mLocHandlerThread.start();
        mLocHandler = new Handler(mLocHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                try {
                    Thread.sleep(100);
                    if (!isStop) {
                        // C：未收到目标坐标前不注入，避免用 DEFAULT_LAT/LNG 打出一帧错误位置
                        if (mStarted) {
                            injectOneFrame();
                        }
                        sendEmptyMessage(HANDLER_MSG_ID);
                    }
                } catch (InterruptedException e) {
                    XLog.e("MockLocationService: ERROR - handleMessage");
                    Thread.currentThread().interrupt();
                }
            }
        };
        mLocHandler.sendEmptyMessage(HANDLER_MSG_ID);
    }

    /**
     * C：同步注入一帧（GPS + NETWORK + FUSED）。
     * 启动 / 通知刷新时立即调用，不等 100ms 循环延迟，让系统位置缓存尽快落到模拟坐标；
     * 循环帧也复用此方法。mInjectLock 串行化，避免主线程首帧、通知刷新与循环并发写拟真状态。
     */
    private void injectOneFrame() {
        if (isStop) {
            return;
        }
        synchronized (mInjectLock) {
            injectFrameLocked();
        }
    }

    /** 实际注入一帧；调用方须持有 mInjectLock */
    private void injectFrameLocked() {
        if (isStop) {
            return;
        }
        updateJitter();
        updateFrameJitter();
        setLocationNetwork();
        setLocationGPS();
        setLocationFused();
    }

    /* ============================== GPS Provider ============================== */

    private void removeTestProviderGPS() {
        try {
            if (mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                mLocManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        } catch (Exception e) {
            XLog.e("MockLocationService: ERROR - removeTestProviderGPS");
        }
    }

    @SuppressLint("wrongconstant")
    private void addTestProviderGPS() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mLocManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true,
                        ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE);
            } else {
                mLocManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true,
                        Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
            }
            if (!mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            }
        } catch (Exception e) {
            XLog.e("MockLocationService: ERROR - addTestProviderGPS");
        }
    }

    private void setLocationGPS() {
        try {
            Location loc = new Location(LocationManager.GPS_PROVIDER);
            loc.setAccuracy(mFrameAccuracy);
            loc.setAltitude(baseAltitude());
            loc.setBearing(baseBearing());
            loc.setLatitude(mCurLat + mJitterLat + mFrameJitterLat);
            loc.setLongitude(mCurLng + mJitterLng + mFrameJitterLng);
            loc.setTime(System.currentTimeMillis());
            loc.setSpeed(mCurSpeed);
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                loc.setVerticalAccuracyMeters(mFrameAccuracy * 0.8F);
                loc.setBearingAccuracyDegrees(10F + mRandom.nextFloat() * 20F);
                loc.setSpeedAccuracyMetersPerSecond(1F + mRandom.nextFloat());
            }
            Bundle bundle = new Bundle();
            bundle.putInt("satellites", 6 + mRandom.nextInt(7));
            loc.setExtras(bundle);
            hideMockFlag(loc);

            mLocManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc);
            markInjectResult(true);
        } catch (Exception e) {
            XLog.e("MockLocationService: ERROR - setLocationGPS");
            markInjectResult(false);
        }
    }

    /* ============================== NETWORK Provider ============================== */

    private void removeTestProviderNetwork() {
        try {
            if (mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false);
                mLocManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            XLog.e("MockLocationService: ERROR - removeTestProviderNetwork");
        }
    }

    @SuppressLint("wrongconstant")
    private void addTestProviderNetwork() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mLocManager.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false,
                        true, true, true, true,
                        true, ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_COARSE);
            } else {
                mLocManager.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false,
                        true, true, true, true,
                        true, Criteria.POWER_LOW, Criteria.ACCURACY_COARSE);
            }
            if (!mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
            }
        } catch (SecurityException e) {
            XLog.e("MockLocationService: ERROR - addTestProviderNetwork");
        }
    }

    private void setLocationNetwork() {
        try {
            Location loc = new Location(LocationManager.NETWORK_PROVIDER);
            loc.setAccuracy(30F + mRandom.nextFloat() * 50F);
            loc.setAltitude(baseAltitude());
            loc.setBearing(baseBearing());
            // 与 GPS 帧保持同一坐标，避免多源输入不一致导致融合引擎弃用模拟位置
            loc.setLatitude(mCurLat + mJitterLat + mFrameJitterLat);
            loc.setLongitude(mCurLng + mJitterLng + mFrameJitterLng);
            loc.setTime(System.currentTimeMillis());
            loc.setSpeed(mCurSpeed);
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            hideMockFlag(loc);

            mLocManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, loc);
        } catch (Exception e) {
            XLog.e("MockLocationService: ERROR - setLocationNetwork");
        }
    }

    /* ============================== FUSED Provider ============================== */

    private void removeTestProviderFused() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && mLocManager.isProviderEnabled(FUSED_PROVIDER_NAME)) {
                mLocManager.setTestProviderEnabled(FUSED_PROVIDER_NAME, false);
                mLocManager.removeTestProvider(FUSED_PROVIDER_NAME);
            }
        } catch (Exception e) {
            XLog.w("MockLocationService: removeTestProviderFused skipped - " + e.getMessage());
        }
    }

    @SuppressLint("wrongconstant")
    private void addTestProviderFused() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && mLocManager.getProvider(FUSED_PROVIDER_NAME) != null) {
                mLocManager.addTestProvider(FUSED_PROVIDER_NAME, true, true, true, false,
                        true, true, true,
                        ProviderProperties.POWER_USAGE_MEDIUM, ProviderProperties.ACCURACY_FINE);
                if (!mLocManager.isProviderEnabled(FUSED_PROVIDER_NAME)) {
                    mLocManager.setTestProviderEnabled(FUSED_PROVIDER_NAME, true);
                }
                mFusedRegistered = true;
                XLog.i("MockLocationService: fused test provider registered");
            }
        } catch (Exception e) {
            // 部分 ROM 禁止对 fused 注册 TestProvider，失败可安全忽略，但需暴露给界面
            mFusedRegistered = false;
            XLog.w("MockLocationService: addTestProviderFused skipped - " + e.getMessage());
        }
    }

    private void setLocationFused() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !mFusedRegistered) {
                return;
            }
            Location loc = new Location(FUSED_PROVIDER_NAME);
            // 与 GPS 帧共用同一组位置/海拔/航向/速度，仅精度略大，避免多源输入冲突
            loc.setAccuracy(mFrameAccuracy + 2F);
            loc.setAltitude(baseAltitude());
            loc.setBearing(baseBearing());
            loc.setLatitude(mCurLat + mJitterLat + mFrameJitterLat);
            loc.setLongitude(mCurLng + mJitterLng + mFrameJitterLng);
            loc.setTime(System.currentTimeMillis());
            loc.setSpeed(mCurSpeed);
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            hideMockFlag(loc);

            mLocManager.setTestProviderLocation(FUSED_PROVIDER_NAME, loc);
        } catch (Exception e) {
            XLog.w("MockLocationService: setLocationFused skipped - " + e.getMessage());
        }
    }

    /* ============================== 拟真与反检测 ============================== */

    /**
     * GPS 信号拟真：每约 2 秒更新一次精度与坐标随机游走，
     * 模拟真实 GPS 的漂移特性，避免“精度恒为 1m、坐标纹丝不动”的异常特征。
     */
    private void updateJitter() {
        if (mJitterTick++ % 20 != 0) {
            return;
        }
        // 定点停留场景：慢漂移收敛到 ±0.00001°（约 ±1m），坐标始终落在目标点附近
        mJitterLat += (mRandom.nextDouble() - 0.5D) * 0.000004D;
        mJitterLng += (mRandom.nextDouble() - 0.5D) * 0.000004D;
        mJitterLat = Math.max(-0.00001D, Math.min(0.00001D, mJitterLat));
        mJitterLng = Math.max(-0.00001D, Math.min(0.00001D, mJitterLng));
        // GPS 精度 4~8m：提高在融合定位中的权重，促使目标应用优先采纳模拟 GPS
        mCurAccuracy = 4F + mRandom.nextFloat() * 4F;
        // 海拔在基准值 ±1m 内、航向在 ±3° 内缓慢漂移
        mAltJitter += (mRandom.nextFloat() - 0.5F) * 0.4F;
        mAltJitter = Math.max(-1F, Math.min(1F, mAltJitter));
        mBeaJitter += (mRandom.nextFloat() - 0.5F) * 2F;
        mBeaJitter = Math.max(-3F, Math.min(3F, mBeaJitter));
    }

    /**
     * 每帧微动与速度拟真：让每一帧的坐标和精度都有轻微变化，
     * 避免客户端与融合引擎因"连续多帧位置完全相同"而判定为静止、丢弃更新。
     */
    private void updateFrameJitter() {
        // 每帧随机游走（单帧约 0.1m），限制在 ±0.7m 内，保持刷新新鲜度又不偏离目标点
        mFrameJitterLat += (mRandom.nextDouble() - 0.5D) * 0.000002D;
        mFrameJitterLng += (mRandom.nextDouble() - 0.5D) * 0.000002D;
        mFrameJitterLat = Math.max(-0.000006D, Math.min(0.000006D, mFrameJitterLat));
        mFrameJitterLng = Math.max(-0.000006D, Math.min(0.000006D, mFrameJitterLng));
        // 精度每帧小幅波动；慢变基准值由 updateJitter 每 2 秒更新
        mFrameAccuracy = Math.max(3F, mCurAccuracy + (mRandom.nextFloat() - 0.5F) * 2F);
        updateSpeed();
    }

    /**
     * 按基准坐标的位移计算速度，使速度与位置变化保持一致；
     * 静止模拟时给 0~0.6 m/s 噪声，瞬移后重置基准避免出现异常速度。
     */
    private void updateSpeed() {
        long now = SystemClock.elapsedRealtimeNanos();
        if (mLastSpeedNanos != 0L) {
            double meters = distanceMeters(mLastBaseLat, mLastBaseLng, mCurLat, mCurLng);
            double seconds = (now - mLastSpeedNanos) / 1_000_000_000D;
            mCurSpeed = seconds > 0D ? (float) (meters / seconds) : 0F;
        } else {
            mCurSpeed = 0F;
        }
        if (mCurSpeed < 1F) {
            mCurSpeed = mRandom.nextFloat() * 0.6F;
        }
        // 收敛到合理上限，避免融合引擎判定物理不可能
        mCurSpeed = Math.min(mCurSpeed, 120F);
        mLastBaseLat = mCurLat;
        mLastBaseLng = mCurLng;
        mLastSpeedNanos = now;
    }

    /** 计算两点间距离（米），经度按纬度做余弦校正 */
    private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = lat2 - lat1;
        double dLng = (lng2 - lng1) * Math.cos(Math.toRadians((lat1 + lat2) / 2D));
        return Math.sqrt(dLat * dLat + dLng * dLng) * 111000D;
    }

    /** 三个 Provider 共用的海拔值，带缓慢抖动 */
    private double baseAltitude() {
        return mCurAlt + mAltJitter;
    }

    /** 三个 Provider 共用的航向值，带缓慢抖动 */
    private float baseBearing() {
        return (mCurBea + mBeaJitter + 360F) % 360F;
    }

    /**
     * 记录注入结果：连续失败达到阈值时把前台通知改为异常文案，
     * 避免 provider 被系统回收后界面仍显示"运行中"却实际未注入。
     */
    private void markInjectResult(boolean ok) {
        if (ok) {
            mInjectFailCount = 0;
            if (mInjectErrorNotified) {
                mInjectErrorNotified = false;
                updateServiceNotification(buildStatusText());
            }
            return;
        }
        if (++mInjectFailCount >= INJECT_FAIL_THRESHOLD && !mInjectErrorNotified) {
            mInjectErrorNotified = true;
            updateServiceNotification(buildStatusText());
            XLog.e("MockLocationService: 连续注入失败 " + mInjectFailCount + " 次，已更新通知提示");
        }
    }

    /**
     * 反射隐藏 Location 的模拟标记，规避目标应用通过
     * {@link Location#isFromMockProvider()}（API&lt;31）或 isMock()（API31+）的检测。
     * 解析结果缓存复用，均失败时记录日志并放行（不影响注入主流程）。
     */
    private void hideMockFlag(Location loc) {
        if (!mMockSetterResolved) {
            mHiddenMockSetter = resolveMockSetter();
            mMockSetterResolved = true;
            if (mHiddenMockSetter != null) {
                XLog.i("MockLocationService: mock flag setter resolved - " + mHiddenMockSetter.getName());
            } else {
                XLog.w("MockLocationService: mock flag setter resolve failed");
            }
        }
        if (mHiddenMockSetter != null) {
            try {
                mHiddenMockSetter.invoke(loc, false);
            } catch (Throwable t) {
                XLog.w("MockLocationService: hideMockFlag invoke failed - " + t.getMessage());
            }
        }
    }

    /**
     * 优先解析当前 API 对应的隐藏 setter：API 31+ 为 setMock，低版本为 setIsFromMockProvider。
     */
    private Method resolveMockSetter() {
        String[] methodNames = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? new String[]{"setMock", "setIsFromMockProvider"}
                : new String[]{"setIsFromMockProvider", "setMock"};
        for (String name : methodNames) {
            Method method = reflectiveGetDeclaredMethod(name);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    /**
     * 通过直接反射或元反射获取隐藏方法，绕过 Android 9+ 的隐藏 API 访问限制。
     */
    @SuppressLint({"PrivateApi", "SoonBlockedPrivateApi"})
    private Method reflectiveGetDeclaredMethod(String methodName) {
        try {
            return Location.class.getDeclaredMethod(methodName, boolean.class);
        } catch (Throwable direct) {
            try {
                // 元反射：借 Class.getDeclaredMethod 自身绕过隐藏 API 拦截
                Method getDeclaredMethod = Class.class.getDeclaredMethod(
                        "getDeclaredMethod", String.class, Class[].class);
                return (Method) getDeclaredMethod.invoke(
                        Location.class, methodName, new Class[]{boolean.class});
            } catch (Throwable meta) {
                return null;
            }
        }
    }

    /* ============================== Binder ============================== */

    public class MockLocationBinder extends Binder {
        /**
         * 运行中更新模拟位置
         *
         * @param lng 经度 (WGS84)
         * @param lat 纬度 (WGS84)
         * @param alt 海拔 (米)
         */
        public void setPosition(double lng, double lat, double alt) {
            mLocHandler.removeMessages(HANDLER_MSG_ID);
            // P0-2/P0-3：坐标写入、持久化与首帧注入统一走 applyCoordinates（持有 mInjectLock）
            applyCoordinates(lng, lat, alt, true);
            mLocHandler.sendEmptyMessage(HANDLER_MSG_ID);
            XLog.i("MockLocationService: update position lat=" + lat + ", lng=" + lng);
        }

        /** Android 12+ 未成功接管 fused Provider 时返回 true，表示融合 coverage 不可用 */
        public boolean isFusedCoverageMissing() {
            return fusedCoverageMissing();
        }
    }
}
