package com.ootd.today.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 颜色工具测试。
 *
 * 重点在**跨 0°** 的情形：色环是环形的，350° 与 10° 只差 20°，
 * 用普通减法会算成 340°。这类错误会直接让「同色系搭配」被判成灾难。
 */
class ColorMathTest {

    @Test
    fun `pure red is chromatic`() {
        assertTrue(ColorMath.isChromatic(0xFFFF0000.toInt()))
    }

    @Test
    fun `mid gray is neutral`() {
        assertFalse(ColorMath.isChromatic(0xFF808080.toInt()))
        assertFalse(ColorMath.isChromatic(0xFFFFFFFF.toInt()))
        assertFalse(ColorMath.isChromatic(0xFF000000.toInt()))
    }

    @Test
    fun `earth tones count as neutral`() {
        // 驼色、燕麦色在实际穿搭里就是中性色。若当成彩色，
        // 「蓝色系 + 驼色围巾」会被误判成撞色。
        assertFalse("驼色 0xFFB8A88A", ColorMath.isChromatic(0xFFB8A88A.toInt()))
        assertFalse("燕麦 0xFFD9C7A7", ColorMath.isChromatic(0xFFD9C7A7.toInt()))
    }

    @Test
    fun `muted but real colors stay chromatic`() {
        // 当前刚需：雾霾蓝这类低饱和但确实是彩色的颜色不能被误归为中性。
        assertTrue("雾霾蓝 0xFF7C9CBF", ColorMath.isChromatic(0xFF7C9CBF.toInt()))
        assertTrue("中蓝 0xFF3A5F8A", ColorMath.isChromatic(0xFF3A5F8A.toInt()))
        assertTrue("藕粉 0xFFE8A0B4", ColorMath.isChromatic(0xFFE8A0B4.toInt()))
    }

    @Test
    fun `spread of evenly spaced hues is the expected arc`() {
        // 0°/120°/240° 等分色环，最短弧段是 240°。
        assertEquals(240f, ColorMath.circularSpread(listOf(0f, 120f, 240f)), 0.01f)
    }

    @Test
    fun `spread across zero degree boundary is small`() {
        // 关键用例：350° 与 10° 相差 20°，不是 340°。
        assertEquals(20f, ColorMath.circularSpread(listOf(350f, 10f)), 0.01f)
    }

    @Test
    fun `spread of a single hue is zero`() {
        assertEquals(0f, ColorMath.circularSpread(listOf(120f)), 0.01f)
    }

    @Test
    fun `clustering merges close hues`() {
        assertEquals(1, ColorMath.clusterHues(listOf(3f, 8f, 15f)).size)
    }

    @Test
    fun `clustering separates distant hues`() {
        assertEquals(3, ColorMath.clusterHues(listOf(0f, 120f, 240f)).size)
    }

    @Test
    fun `clustering merges across zero degree boundary`() {
        // 355° 与 5° 属于同一个色系，应合成一簇。
        assertEquals(1, ColorMath.clusterHues(listOf(355f, 5f)).size)
    }

    @Test
    fun `clustering of empty input is empty`() {
        assertTrue(ColorMath.clusterHues(emptyList()).isEmpty())
    }
}