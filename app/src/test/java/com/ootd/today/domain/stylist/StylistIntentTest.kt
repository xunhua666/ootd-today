package com.ootd.today.domain.stylist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 追问意图识别测试。
 *
 * 这是「本地做意图识别」那一环。识别质量直接影响追问效果：
 * 猜错了品类，模型就会去改一个用户根本没提的地方。
 */
class StylistIntentTest {

    // ------------------------------------------------------------------
    // 换品类
    // ------------------------------------------------------------------

    @Test
    fun `swap bottom is recognised`() {
        val result = StylistIntent.normalize("换条裤子")
        assertTrue(result, result.contains("下装"))
    }

    @Test
    fun `swap shoes is recognised`() {
        assertTrue(StylistIntent.normalize("换双鞋").contains("鞋"))
    }

    @Test
    fun `dress beats bottom keyword`() {
        // 「连衣裙」里含「裙」字，若判定顺序不对会被归到不重要的一类。
        assertTrue(StylistIntent.normalize("换条连衣裙").contains("连体"))
    }

    @Test
    fun `remove intent is distinguished from swap`() {
        // 「不要外套」与「换外套」意图相反，必须分开。
        val remove = StylistIntent.normalize("不要外套")
        assertTrue(remove, remove.contains("不要"))
        assertTrue(remove, remove.contains("外套"))

        val swap = StylistIntent.normalize("换件外套")
        assertTrue(swap, swap.contains("更换"))
    }

    @Test
    fun `removing a category does not mention swapping`() {
        val result = StylistIntent.normalize("去掉配饰")
        assertTrue(result, result.contains("不要"))
        assertTrue(result, !result.contains("更换"))
    }

    // ------------------------------------------------------------------
    // 风格倾向
    // ------------------------------------------------------------------

    @Test
    fun `too plain is recognised`() {
        assertTrue(StylistIntent.normalize("太素了").contains("太素"))
    }

    @Test
    fun `too busy is recognised`() {
        assertTrue(StylistIntent.normalize("颜色太多了").contains("太花"))
    }

    @Test
    fun `formal and casual are distinguished`() {
        assertTrue(StylistIntent.normalize("正式一点").contains("正式"))
        assertTrue(StylistIntent.normalize("随便点就行").contains("休闲"))
    }

    @Test
    fun `warmer and cooler are distinguished`() {
        assertTrue(StylistIntent.normalize("有点冷，加件").contains("保暖"))
        assertTrue(StylistIntent.normalize("太热了").contains("凉快"))
    }

    // ------------------------------------------------------------------
    // 兜底
    // ------------------------------------------------------------------

    @Test
    fun `unrecognised text is passed through unchanged`() {
        // 认不出来时原样交给模型，比强行归类到某个错误意图上要好。
        val raw = "我想要那种很有故事感的感觉"
        assertEquals(raw, StylistIntent.normalize(raw))
    }

    @Test
    fun `blank input stays blank`() {
        assertEquals("", StylistIntent.normalize("   "))
    }
}