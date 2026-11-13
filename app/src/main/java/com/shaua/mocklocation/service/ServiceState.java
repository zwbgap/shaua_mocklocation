package com.shaua.mocklocation.service;

/**
 * 模拟定位服务的纯状态判定逻辑。
 * <p>
 * 从 {@link MockLocationService} 抽出，便于 JVM 单元测试（不依赖 Android framework，
 * 因此纯 JUnit 测试可直接在本地 JVM 运行）：
 * 常驻通知文案的优先级、自动刷新节流、融合覆盖是否缺失。
 */
public final class ServiceState {

    /** Android 12（API 31）常量，避免依赖 android.os.Build 以便在 JVM 单测中运行 */
    public static final int SDK_S = 31;

    private ServiceState() {
    }

    /** 通知状态：注入异常 > 融合覆盖不可用 > 正常运行 */
    public enum NoticeStatus {
        INJECT_ERROR,
        FUSED_MISSING,
        NORMAL
    }

    /**
     * 计算常驻通知应显示的状态。
     *
     * @param injectErrorNotified 是否已触发连续注入异常
     * @param fusedRegistered     fused test provider 是否成功接管
     * @param sdkInt              当前系统 API level（通常传 {@link android.os.Build.VERSION#SDK_INT}）
     */
    public static NoticeStatus noticeStatus(boolean injectErrorNotified,
                                            boolean fusedRegistered,
                                            int sdkInt) {
        if (injectErrorNotified) {
            return NoticeStatus.INJECT_ERROR;
        }
        if (fusedCoverageMissing(fusedRegistered, sdkInt)) {
            return NoticeStatus.FUSED_MISSING;
        }
        return NoticeStatus.NORMAL;
    }

    /** Android 12（S）及以上、且未接管 fused Provider 时，融合覆盖不可用 */
    public static boolean fusedCoverageMissing(boolean fusedRegistered, int sdkInt) {
        return sdkInt >= SDK_S && !fusedRegistered;
    }

    /**
     * 自动刷新节流判定：距上次刷新不足 {@code throttleMs} 时应跳过。
     *
     * @param nowMs       当前时间戳（毫秒）
     * @param lastMs      上次刷新时间戳（毫秒），0 表示从未刷新
     * @param throttleMs  节流窗口（毫秒）
     * @return true 表示本次应执行刷新
     */
    public static boolean shouldAutoRefresh(long nowMs, long lastMs, long throttleMs) {
        if (lastMs <= 0L) {
            return true;
        }
        // 系统时钟回拨时允许刷新，避免被永久节流
        if (nowMs < lastMs) {
            return true;
        }
        return nowMs - lastMs >= throttleMs;
    }
}
