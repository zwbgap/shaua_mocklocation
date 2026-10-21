package com.shaua.mocklocation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shaua.mocklocation.utils.GoUtils;

import org.junit.Test;

/**
 * GoUtils 纯逻辑单元测试：语义化版本比较、历史过期阈值
 */
public class GoUtilsTest {

    /* ================ compareVersion ================ */

    @Test
    public void compareVersion_equal() {
        assertEquals(0, GoUtils.compareVersion("1.0.5", "1.0.5"));
        assertEquals(0, GoUtils.compareVersion("1.0", "1.0.0"));
    }

    @Test
    public void compareVersion_greater() {
        assertTrue(GoUtils.compareVersion("1.0.6", "1.0.5") > 0);
        // 修复 contains 误判的核心场景：1.0.10 > 1.0.9
        assertTrue(GoUtils.compareVersion("1.0.10", "1.0.9") > 0);
        assertTrue(GoUtils.compareVersion("1.1.0", "1.0.99") > 0);
        assertTrue(GoUtils.compareVersion("2.0", "1.9.9") > 0);
    }

    @Test
    public void compareVersion_less() {
        assertTrue(GoUtils.compareVersion("1.0.1", "1.0.10") < 0);
        assertTrue(GoUtils.compareVersion("1.0.5", "1.1.0") < 0);
        assertTrue(GoUtils.compareVersion("0.9", "1.0") < 0);
    }

    @Test
    public void compareVersion_malformedSegmentsTreatedAsZero() {
        assertTrue(GoUtils.compareVersion("1.0.x", "1.0.0") == 0);
        // "5-beta" 解析失败按 0 处理，因此 1.0.5-beta < 1.0.5
        assertTrue(GoUtils.compareVersion("1.0.5-beta", "1.0.5") < 0);
        assertEquals(0, GoUtils.compareVersion(null, ""));
    }

    /* ================ computeExpiredThresholdSeconds ================ */

    @Test
    public void expiredThreshold_30days() {
        // 2026-10-21 00:00:00 UTC 的毫秒时间戳
        long nowMillis = 1791744000000L;
        long expected = nowMillis / 1000 - 30L * 24 * 60 * 60;
        assertEquals(expected, GoUtils.computeExpiredThresholdSeconds(30, nowMillis));
    }

    @Test
    public void expiredThreshold_zeroDays() {
        long nowMillis = 1791744000000L;
        assertEquals(nowMillis / 1000, GoUtils.computeExpiredThresholdSeconds(0, nowMillis));
    }

    @Test
    public void expiredThreshold_isSecondsNotMillis() {
        long nowMillis = 1791744000000L;
        long threshold = GoUtils.computeExpiredThresholdSeconds(30, nowMillis);
        // 阈值必须是秒级（10 位），不能是毫秒级（13 位）
        assertTrue(threshold < 10_000_000_000L);
    }
}
