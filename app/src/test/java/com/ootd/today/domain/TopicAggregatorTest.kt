package com.ootd.today.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 热门话题聚合测试。
 *
 * 重点覆盖「忽略大小写」与「排序稳定性」——这两点出问题会直接
 * 导致话题榜出现重复项或每次刷新顺序乱跳。
 */
class TopicAggregatorTest {

    @Test
    fun `empty input gives empty ranking`() {
        assertTrue(TopicAggregator.rank(emptyList()).isEmpty())
    }

    @Test
    fun `rows without topics are ignored`() {
        assertTrue(TopicAggregator.rank(listOf("", "", "")).isEmpty())
    }

    @Test
    fun `counts occurrences across rows`() {
        val ranking = TopicAggregator.rank(
            listOf(
                "通勤穿搭,ootd",
                "通勤穿搭",
                "ootd,复古",
                "通勤穿搭"
            )
        )
        assertEquals(listOf("通勤穿搭" to 3, "ootd" to 2, "复古" to 1), ranking)
    }

    @Test
    fun `case insensitive merging keeps first seen form`() {
        // #OOTD 与 #ootd 应合并为同一话题，展示时保留首次出现的写法。
        val ranking = TopicAggregator.rank(listOf("OOTD", "ootd", "Ootd"))
        assertEquals(listOf("OOTD" to 3), ranking)
    }

    @Test
    fun `chinese tags are not lowercased incorrectly`() {
        val ranking = TopicAggregator.rank(listOf("通勤穿搭", "通勤穿搭"))
        assertEquals(listOf("通勤穿搭" to 2), ranking)
    }

    @Test
    fun `limit caps the result size`() {
        val ranking = TopicAggregator.rank(
            listOf("a,b,c,d,e,f,g,h,i,j"),
            limit = 3
        )
        assertEquals(3, ranking.size)
    }

    @Test
    fun `ties keep first seen order for stable output`() {
        // a、b、c 各出现一次，输出顺序应等于首次出现顺序，不能抖动。
        val ranking = TopicAggregator.rank(listOf("b,a,c"))
        assertEquals(listOf("b" to 1, "a" to 1, "c" to 1), ranking)
    }

    @Test
    fun `blank entries inside a row are skipped`() {
        // 形如 ",," 的脏数据不应产生空标签。
        val ranking = TopicAggregator.rank(listOf(",,通勤,,"))
        assertEquals(listOf("通勤" to 1), ranking)
    }

    @Test
    fun `same topic twice in one row counts twice`() {
        // 一条动态里重复写了同一个话题，按出现次数计。
        val ranking = TopicAggregator.rank(listOf("ootd,ootd"))
        assertEquals(listOf("ootd" to 2), ranking)
    }
}