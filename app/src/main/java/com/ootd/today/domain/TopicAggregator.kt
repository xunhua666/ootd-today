package com.ootd.today.domain

/**
 * 热门话题聚合。
 *
 * 从「每行一条动态的话题串」（逗号分隔）统计出标签热度排行。
 *
 * 抽成独立对象的理由：
 * 1. 聚合规则（忽略大小写、保留原始写法、按热度排序）容易出错，值得单测；
 * 2. 仓库实现里只剩一行调用，逻辑集中在此处，接真后端时可整块替换。
 */
object TopicAggregator {

    /**
     * @param topicStrings 每条动态的话题串，形如 `"通勤穿搭,ootd"`；空串表示无话题。
     * @param limit 最多返回多少个标签。
     * @return 标签 -> 出现次数，按次数降序；次数相同时按首次出现顺序排列，
     *   保证结果稳定（不随 Map 迭代顺序变化而抖动）。
     */
    fun rank(topicStrings: List<String>, limit: Int = 8): List<Pair<String, Int>> {
        // LinkedHashMap 保证插入顺序，从而让「同热度」的排序稳定。
        val counts = linkedMapOf<String, Int>()
        val displayForms = mutableMapOf<String, String>()

        topicStrings.forEach { raw ->
            raw.split(",")
                .filter { it.isNotBlank() }
                .forEach { tag ->
                    val key = tag.lowercase()
                    counts[key] = (counts[key] ?: 0) + 1
                    // 保留首次出现的写法，避免把用户输入的样式弄丢（#OOTD vs #ootd）。
                    displayForms.putIfAbsent(key, tag)
                }
        }

        return counts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (key, count) -> (displayForms[key] ?: key) to count }
    }
}