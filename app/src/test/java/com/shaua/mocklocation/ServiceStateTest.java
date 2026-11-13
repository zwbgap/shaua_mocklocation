package com.shaua.mocklocation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shaua.mocklocation.service.ServiceState;

import org.junit.Test;

/**
 * ServiceState 纯逻辑单元测试：常驻通知状态优先级、融合覆盖判定、自动刷新节流。
 * 该类不依赖 Android framework，可直接在本地 JVM 运行。
 */
public class ServiceStateTest {

    /* ================ noticeStatus 优先级 ================ */

    @Test
    public void noticeStatus_injectErrorHasTopPriority() {
        // 注入异常优先于融合覆盖不可用
        assertEquals(ServiceState.NoticeStatus.INJECT_ERROR,
                ServiceState.noticeStatus(true, false, ServiceState.SDK_S));
        assertEquals(ServiceState.NoticeStatus.INJECT_ERROR,
                ServiceState.noticeStatus(true, true, ServiceState.SDK_S));
    }

    @Test
    public void noticeStatus_fusedMissingOnAndroid12Plus() {
        assertEquals(ServiceState.NoticeStatus.FUSED_MISSING,
                ServiceState.noticeStatus(false, false, ServiceState.SDK_S));
        assertEquals(ServiceState.NoticeStatus.FUSED_MISSING,
                ServiceState.noticeStatus(false, false, ServiceState.SDK_S + 2));
    }

    @Test
    public void noticeStatus_normalWhenFusedRegisteredOrBelowAndroid12() {
        // Android 12 以下没有 fused Provider 覆盖需求，不算降级
        assertEquals(ServiceState.NoticeStatus.NORMAL,
                ServiceState.noticeStatus(false, false, ServiceState.SDK_S - 1));
        assertEquals(ServiceState.NoticeStatus.NORMAL,
                ServiceState.noticeStatus(false, true, ServiceState.SDK_S));
        assertEquals(ServiceState.NoticeStatus.NORMAL,
                ServiceState.noticeStatus(false, true, ServiceState.SDK_S - 1));
    }

    /* ================ fusedCoverageMissing ================ */

    @Test
    public void fusedCoverageMissing_onlyOnSApiAndWhenNotRegistered() {
        assertTrue(ServiceState.fusedCoverageMissing(false, ServiceState.SDK_S));
        assertFalse(ServiceState.fusedCoverageMissing(true, ServiceState.SDK_S));
        assertFalse(ServiceState.fusedCoverageMissing(false, ServiceState.SDK_S - 1));
        assertTrue(ServiceState.fusedCoverageMissing(false, 34));
    }

    /* ================ shouldAutoRefresh 节流 ================ */

    @Test
    public void shouldAutoRefresh_firstCallAlwaysAllowed() {
        assertTrue(ServiceState.shouldAutoRefresh(1000L, 0L, 1500L));
    }

    @Test
    public void shouldAutoRefresh_withinThrottleIsSkipped() {
        assertFalse(ServiceState.shouldAutoRefresh(1000L, 500L, 1500L));
        // 恰好等于窗口边界时允许
        assertTrue(ServiceState.shouldAutoRefresh(2000L, 500L, 1500L));
        assertTrue(ServiceState.shouldAutoRefresh(2001L, 500L, 1500L));
    }

    @Test
    public void shouldAutoRefresh_clockRollbackStillAllowed() {
        // 系统时钟回拨时不能永久节流
        assertTrue(ServiceState.shouldAutoRefresh(100L, 5000L, 1500L));
    }

    @Test
    public void shouldAutoRefresh_zeroThrottleAlwaysAllowed() {
        assertTrue(ServiceState.shouldAutoRefresh(1000L, 1000L, 0L));
        assertTrue(ServiceState.shouldAutoRefresh(1000L, 999L, 0L));
    }
}
