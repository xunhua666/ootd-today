package com.ootd.today.domain.stylist

import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.model.Weather

/**
 * 一次「问沈砚」请求的内容。
 *
 * @property systemPrompt 人设与输出格式约束。
 * @property userText 本次的天气、场合与可选单品清单。
 * @property imagePaths 需要一起发给模型看的照片路径（用户选择上传时才有）。
 * @property followUp 追问的修改要求，首次请求时为 null。
 */
data class StylistRequest(
    val systemPrompt: String,
    val userText: String,
    val imagePaths: List<String>,
    val followUp: String? = null
)

/**
 * 构造发给大模型的请求内容。
 *
 * 纯函数、可单测。把「怎么描述衣橱」这件事集中在这里，
 * 提示词调整不需要碰网络层和 UI。
 */
object StylistPromptBuilder {

    /**
     * 单品清单的上限。
     *
     * 超过这个数量就截断 —— 一方面控制 token 开销，
     * 另一方面过长的清单反而会让模型抓不住重点。
     * 截断时优先保留**场合匹配**的单品，因为不匹配的本来就不会被选。
     */
    private const val MAX_ITEMS = 80

    fun build(
        items: List<WardrobeItem>,
        weather: Weather,
        occasion: Occasion,
        includePhotos: Boolean,
        followUp: String? = null
    ): StylistRequest {
        val usable = items.filter { it.occasions.contains(occasion) }
        val pool = if (usable.isEmpty()) items else usable
        val shortlist = pool.take(MAX_ITEMS)

        // 只带出被选入清单的单品照片，避免把整个衣橱的照片都传出去。
        val photos = if (includePhotos) {
            shortlist.mapNotNull { it.imagePath }.filter { it.isNotBlank() }.take(MAX_PHOTOS)
        } else {
            emptyList()
        }

        return StylistRequest(
            systemPrompt = StylistPersona.SYSTEM_PROMPT,
            userText = buildUserText(weather, occasion, shortlist, truncated = pool.size > MAX_ITEMS),
            imagePaths = photos,
            followUp = followUp
        )
    }

    private fun buildUserText(
        weather: Weather,
        occasion: Occasion,
        items: List<WardrobeItem>,
        truncated: Boolean
    ): String = buildString {
        appendLine("今天的条件：")
        appendLine("- 地点：${weather.city}")
        appendLine("- 天气：${weather.condition.label}，${weather.lowC}~${weather.highC}℃，当前 ${weather.temperatureC}℃")
        appendLine()
        appendLine("场合：${occasion.label}")
        appendLine()
        appendLine("用户衣橱里可选的单品（每行一件，格式：id | 名称 | 品类 | 主色 | 保暖度1-5 | 适用季节 | 适用场合 | 风格 | 已穿次数）：")

        items.forEach { item ->
            appendLine(
                listOf(
                    item.id.toString(),
                    item.name,
                    "${item.category.label}${if (item.subCategory.isNotBlank()) "/${item.subCategory}" else ""}",
                    "${item.colorName}(${hexOf(item.colorArgb)})",
                    item.warmthLevel.toString(),
                    item.seasons.joinToString("/") { it.label }.ifBlank { "未标注" },
                    item.occasions.joinToString("/") { it.label }.ifBlank { "未标注" },
                    item.style?.label ?: "未标注",
                    item.wearCount.toString(),
                    "已确认版型:${item.shape.summary}"
                ).joinToString(" | ")
            )
        }

        if (truncated) {
            appendLine()
            appendLine("（清单已截断，只列出了最相关的一部分单品）")
        }

        appendLine()
        append("请从上面这些单品中挑出今天最合适的一套，按约定的 JSON 格式返回。")
    }

    /** 把 ARGB 值转成 #RRGGBB，给模型一个可参考的具体色值。 */
    private fun hexOf(argb: Long): String = "#%06X".format(argb and 0xFFFFFF)

    /** 一次最多带几张照片，避免请求体过大。 */
    private const val MAX_PHOTOS = 6
}
