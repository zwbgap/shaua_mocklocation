package com.shaua.mocklocation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.shaua.mocklocation.utils.MapUtils;

import org.junit.Test;

import java.util.Random;

/**
 * MapUtils 纯逻辑单元测试：随机偏移、坐标转换往返
 */
public class MapUtilsTest {

    /* ================ randomOffset ================ */

    @Test
    public void randomOffset_withinBounds() {
        double lon = 117.027707;
        double lat = 36.667662;
        double maxLon = 10.0;
        double maxLat = 10.0;
        Random random = new Random(42);

        for (int i = 0; i < 1000; i++) {
            double[] result = MapUtils.randomOffset(lon, lat, maxLon, maxLat, random);
            double lonOffset = result[2];
            double latOffset = result[3];
            assertTrue("经度偏移超界: " + lonOffset, Math.abs(lonOffset) <= maxLon);
            assertTrue("纬度偏移超界: " + latOffset, Math.abs(latOffset) <= maxLat);

            // 偏移后坐标 = 原坐标 + 偏移量换算
            assertEquals(lon + lonOffset / 111320, result[0], 1e-12);
            assertEquals(lat + latOffset / 110574, result[1], 1e-12);
        }
    }

    @Test
    public void randomOffset_deterministicWithSameSeed() {
        double[] r1 = MapUtils.randomOffset(117.0, 36.6, 10, 10, new Random(7));
        double[] r2 = MapUtils.randomOffset(117.0, 36.6, 10, 10, new Random(7));
        assertEquals(r1[0], r2[0], 0);
        assertEquals(r1[1], r2[1], 0);
    }

    @Test
    public void randomOffset_nonZeroSpread() {
        // 固定种子下多次偏移应产生不同结果（覆盖随机性）
        Random random = new Random(1);
        double first = MapUtils.randomOffset(117.0, 36.6, 50, 50, random)[2];
        int sameCount = 0;
        for (int i = 0; i < 100; i++) {
            double v = MapUtils.randomOffset(117.0, 36.6, 50, 50, random)[2];
            if (v == first) {
                sameCount++;
            }
        }
        assertTrue("偏移量分布异常", sameCount < 100);
    }

    /* ================ 坐标转换往返 ================ */

    @Test
    public void bd09ToWgsToBd09_roundTrip() {
        // 济南附近 BD09 坐标
        double bdLon = 117.027707;
        double bdLat = 36.667662;

        double[] wgs = MapUtils.bd2wgs(bdLon, bdLat);
        double[] bdBack = MapUtils.wgs2bd09(wgs[0], wgs[1]);

        // gcj02towgs84 为近似逆变换，往返误差应远小于 1e-3°（约百米级）
        assertEquals(bdLon, bdBack[0], 1e-3);
        assertEquals(bdLat, bdBack[1], 1e-3);
    }
}
