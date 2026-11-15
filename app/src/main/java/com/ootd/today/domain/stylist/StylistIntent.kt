package com.ootd.today.domain.stylist

import com.ootd.today.data.model.ClothingCategory

/**
 * 追问意图识别（本地）。
 *
 * 用户对一套搭配的追问通常是口语化的短句：「换条裤子」「太素了」「正式点」。
 * 本地把它整理成一句明确的修改要求再发给模型，比直接丢原话更稳 ——
 * 用户说「这条裤子不行」时，模型并不知道指的是哪一条。
 *
 * 纯函数、可单测，不消耗任何网络调用。
 */
object StylistIntent {

    /**
     * 把用户的追问整理成给模型的明确指令。
     *
     * 识别不出来时原样返回，交给模型自己理解 —— 这比强行归类到某个
     * 错误意图上要好。
     */
    fun normalize(raw: String): String {
        val text = raw.trim()
        if (text.isEmpty()) return text

        val category = categoryOf(text)
        if (category != null) {
            // 「不要外套」和「换外套」的意图相反，需要区分。
            val wantsRemove = REMOVE_WORDS.any { text.contains(it) }
            return if (wantsRemove) {
                "不要${category.label}，请去掉后重新搭配"
            } else {
                "更换${category.label}，换一件别的"
            }
        }

        MATCHERS.firstOrNull { (words, _) -> words.any { text.contains(it) } }
            ?.let { return it.second }

        return text
    }

    /**
     * 从文本里识别出用户指的是哪个品类。
     *
     * 顺序有讲究：「连衣裙」必须排在「裤子」类之前判断，
     * 否则「换条连衣裙」会被识别成下装。
     */
    private fun categoryOf(text: String): ClothingCategory? =
        CATEGORY_KEYWORDS.firstOrNull { (_, words) -> words.any { text.contains(it) } }?.first

    private val CATEGORY_KEYWORDS: List<Pair<ClothingCategory, List<String>>> = listOf(
        ClothingCategory.ONE_PIECE to listOf("连衣裙", "连体裤", "连体装", "裙子", "裙装"),
        ClothingCategory.OUTERWEAR to listOf("外套", "大衣", "风衣", "夹克", "羽绒", "皮衣", "西装外套"),
        ClothingCategory.SHOES to listOf("鞋", "靴"),
        ClothingCategory.BAG to listOf("包"),
        ClothingCategory.ACCESSORY to listOf("配饰", "围巾", "帽子", "墨镜", "项链", "手表", "袜子"),
        ClothingCategory.BOTTOM to listOf("下装", "裤子", "裤", "半裙", "短裤"),
        ClothingCategory.TOP to listOf("上装", "上衣", "衬衫", "T恤", "卫衣", "毛衣", "针织", "背心")
    )

    /** 非品类类的意图，按优先级从上到下匹配。 */
    private val MATCHERS: List<Pair<List<String>, String>> = listOf(
        listOf("太素", "太单调", "太普通", "太闷", "没亮点") to
            "整体太素太单调，希望更有对比度或层次感",
        listOf("太花", "太乱", "太杂", "颜色太多", "太跳") to
            "整体太花太乱，希望收敛配色，减少颜色数量",
        listOf("正式", "严肃", "见客户", "面试") to
            "希望更正式一些，适合严肃场合",
        listOf("休闲", "随便", "舒服", "放松", "日常") to
            "希望更休闲舒适一些",
        listOf("冷", "保暖", "加厚", "加件") to
            "希望更保暖一些，可以加外套",
        listOf("热", "凉快", "薄一点", "少一件") to
            "希望更凉快一些，减少厚重单品"
    )

    private val REMOVE_WORDS = listOf("不要", "去掉", "不用", "别加")
}