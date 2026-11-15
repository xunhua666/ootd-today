package com.ootd.today.domain.stylist

import com.ootd.today.data.model.WardrobeItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * 「沈砚」返回结果的解析。
 *
 * 纯函数、无网络依赖，可以直接单测。
 *
 * 对模型输出采取**宽容策略**：大模型不一定严格守格式，
 * 常见的有包了 markdown 代码块、前后带解释文字、字段缺失。
 * 这些都不该让功能直接失败，能救则救。
 */
object StylistAdviceParser {

    /**
     * @param raw 模型返回的原始文本。
     * @param pool 本次请求提供给模型的全部单品，用于把 id 还原成单品。
     * @return 解析成功且选出的单品能构成一套合法搭配时返回结果，否则 null。
     */
    fun parse(raw: String, pool: List<WardrobeItem>): StylistAdvice? {
        val json = extractJsonObject(raw) ?: return null

        val ids = json.optJSONArray("itemIds") ?: return null
        val byId = pool.associateBy { it.id }
        val items = buildList {
            for (i in 0 until ids.length()) {
                val id = ids.optLong(i, -1L)
                // 过滤掉模型编造出来的 id —— 这是最常见的越界行为。
                byId[id]?.let { add(it) }
            }
        }.distinctBy { it.id }

        if (items.size < MIN_ITEMS) return null

        val headline = json.optString("headline").trim()
        val reason = json.optString("reason").trim()
        val tip = json.optString("tip").trim().ifBlank { null }

        return StylistAdvice(
            items = items,
            headline = headline.ifBlank { "今日搭配" },
            reason = reason,
            tip = tip
        )
    }

    /**
     * 从可能夹带杂物的文本里抠出 JSON 对象。
     *
     * 先尝试整体解析；失败则退而求其次，取第一个 `{` 到最后一个 `}` 之间的内容。
     * 这能覆盖「模型在 JSON 前后加了说明文字」以及
     * 「包在 ```json 代码块里」两种常见情况。
     */
    private fun extractJsonObject(raw: String): JSONObject? {
        val trimmed = raw.trim()
        runCatching { return JSONObject(trimmed) }

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start < 0 || end <= start) return null

        return runCatching { JSONObject(trimmed.substring(start, end + 1)) }.getOrNull()
    }

    /** 少于这个件数就不算一套搭配。 */
    private const val MIN_ITEMS = 2
}

/**
 * 「沈砚」给出的建议。
 *
 * @property items 从用户衣橱里选出的单品，必定都属于用户。
 * @property headline 一句话定调。
 * @property reason 为什么这么搭。
 * @property tip 附带的穿着建议，可为空。
 */
data class StylistAdvice(
    val items: List<WardrobeItem>,
    val headline: String,
    val reason: String,
    val tip: String?
) {
    /** 拼成 UI 上直接展示的一段话。 */
    fun toDisplayText(): String = buildString {
        append(reason)
        tip?.let { append(" ").append(it) }
    }
}

/** 让 JSONArray 支持逐项读取时的默认值写法。 */
private fun JSONArray.optString(index: Int): String = optString(index, "")