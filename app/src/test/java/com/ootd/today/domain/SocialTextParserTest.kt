package com.ootd.today.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 社交文本解析器测试。
 *
 * 这个解析器被 @提及、#话题、动态流筛选三处共用，
 * 一旦识别错误会同时影响多个功能，因此边界情形必须覆盖到位。
 */
class SocialTextParserTest {

    // ------------------------------------------------------------------
    // 基础识别
    // ------------------------------------------------------------------

    @Test
    fun `plain text yields single plain segment`() {
        val segments = SocialTextParser.parse("今天天气不错")
        assertEquals(listOf(TextSegment.Plain("今天天气不错")), segments)
    }

    @Test
    fun `empty input yields no segments`() {
        assertTrue(SocialTextParser.parse("").isEmpty())
    }

    @Test
    fun `parses mention in the middle of text`() {
        val segments = SocialTextParser.parse("今天这套 @阿茶 觉得怎么样")
        assertEquals(
            listOf(
                TextSegment.Plain("今天这套 "),
                TextSegment.Mention("阿茶"),
                TextSegment.Plain(" 觉得怎么样")
            ),
            segments
        )
    }

    @Test
    fun `parses topic tag`() {
        val segments = SocialTextParser.parse("今日穿搭 #通勤穿搭 #ootd")
        assertEquals(
            listOf(
                TextSegment.Plain("今日穿搭 "),
                TextSegment.Topic("通勤穿搭"),
                TextSegment.Plain(" "),
                TextSegment.Topic("ootd")
            ),
            segments
        )
    }

    @Test
    fun `topic right after chinese text is still recognised`() {
        // 中文不把 # 嵌在词内，因此「今天#通勤」应识别出话题。
        val segments = SocialTextParser.parse("今天#通勤")
        assertEquals(
            listOf(TextSegment.Plain("今天"), TextSegment.Topic("通勤")),
            segments
        )
    }

    @Test
    fun `parses mixed mentions and topics`() {
        val segments = SocialTextParser.parse("@Kai #街头 这套太酷了")
        assertEquals(
            listOf(
                TextSegment.Mention("Kai"),
                TextSegment.Plain(" "),
                TextSegment.Topic("街头"),
                TextSegment.Plain(" 这套太酷了")
            ),
            segments
        )
    }

    // ------------------------------------------------------------------
    // 边界：不该被识别为提及/话题的情形
    // ------------------------------------------------------------------

    @Test
    fun `email address is not treated as mention`() {
        // @ 前面是词字符，属于邮箱的一部分。
        val segments = SocialTextParser.parse("联系我 a@b.com")
        assertEquals(listOf(TextSegment.Plain("联系我 a@b.com")), segments)
    }

    @Test
    fun `lone marker without word is plain text`() {
        // @ 后面没有词字符，不算提及。
        assertEquals(listOf(TextSegment.Plain("@ 你好")), SocialTextParser.parse("@ 你好"))
        assertEquals(listOf(TextSegment.Plain("#")), SocialTextParser.parse("#"))
    }

    @Test
    fun `marker followed by punctuation is plain text`() {
        assertEquals(listOf(TextSegment.Plain("@，你好")), SocialTextParser.parse("@，你好"))
    }

    @Test
    fun `topic stops at punctuation`() {
        val segments = SocialTextParser.parse("#通勤，然后去约会")
        assertEquals(
            listOf(
                TextSegment.Topic("通勤"),
                TextSegment.Plain("，然后去约会")
            ),
            segments
        )
    }

    @Test
    fun `mention stops at comma`() {
        val segments = SocialTextParser.parse("@阿茶，今天穿什么")
        assertEquals(
            listOf(
                TextSegment.Mention("阿茶"),
                TextSegment.Plain("，今天穿什么")
            ),
            segments
        )
    }

    // ------------------------------------------------------------------
    // 边界：允许的连接符
    // ------------------------------------------------------------------

    @Test
    fun `nickname with underscore and digits is captured fully`() {
        val segments = SocialTextParser.parse("@Kai_99 看这个")
        assertEquals(
            listOf(
                TextSegment.Mention("Kai_99"),
                TextSegment.Plain(" 看这个")
            ),
            segments
        )
    }

    @Test
    fun `nickname with dash is captured fully`() {
        val segments = SocialTextParser.parse("@小-满 你好")
        assertEquals(
            listOf(
                TextSegment.Mention("小-满"),
                TextSegment.Plain(" 你好")
            ),
            segments
        )
    }

    @Test
    fun `mention after chinese text is recognised`() {
        // 中文不把 @ 嵌在词内，因此「你好@阿茶」应识别出提及。
        val segments = SocialTextParser.parse("你好@阿茶")
        assertEquals(
            listOf(TextSegment.Plain("你好"), TextSegment.Mention("阿茶")),
            segments
        )
    }

    @Test
    fun `leading connector is not accepted`() {
        // `-` 不允许作为首字符。
        assertEquals(listOf(TextSegment.Plain("#-abc")), SocialTextParser.parse("#-abc"))
    }

    // ------------------------------------------------------------------
    // 提取辅助方法
    // ------------------------------------------------------------------

    @Test
    fun `extractTopics deduplicates while keeping order`() {
        val topics = SocialTextParser.extractTopics("#a 文字 #b 再来 #a")
        assertEquals(listOf("a", "b"), topics)
    }

    @Test
    fun `extractMentions deduplicates while keeping order`() {
        val mentions = SocialTextParser.extractMentions("@张三 @李四 还有 @张三")
        assertEquals(listOf("张三", "李四"), mentions)
    }

    @Test
    fun `two adjacent topics both parsed`() {
        // 设计权衡：# 不做「词内符号」检查（只有 @ 做，用于排除邮箱），
        // 因此 #a#b 识别为两个话题。
        val segments = SocialTextParser.parse("#a#b")
        assertEquals(
            listOf(TextSegment.Topic("a"), TextSegment.Topic("b")),
            segments
        )
    }

    @Test
    fun `known trade-off c sharp is parsed as topic`() {
        // 显式记录设计权衡：# 不做「词内符号」检查（只有 @ 做，用于排除邮箱），
        // 因此 `C#锐` 会被识别出话题「锐」。
        // 穿搭场景下不构成实际问题，此测试写明以防后人误以为是 bug。
        val segments = SocialTextParser.parse("我在学 C#锐")
        assertEquals(
            listOf(TextSegment.Plain("我在学 C"), TextSegment.Topic("锐")),
            segments
        )
    }

    @Test
    fun `lone hash followed by nothing is plain text`() {
        // `C#` 后面无词字符 → # 不是话题起始，整体作为普通文字。
        assertEquals(
            listOf(TextSegment.Plain("我在学 C#")),
            SocialTextParser.parse("我在学 C#")
        )
    }
}