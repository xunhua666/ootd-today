package com.ootd.today.data.model

/**
 * 穿着场合。推荐算法的核心过滤维度之一。
 */
enum class Occasion(val label: String, val emoji: String) {
    COMMUTE("通勤", "💼"),
    DATE("约会", "🌹"),
    SPORT("运动", "🏃"),
    HOME("居家", "🏠"),
    FORMAL("正式", "🎩"),
    VACATION("度假", "🏖")
}

/**
 * 适用季节。与保暖度是两个独立维度：
 * 季节是用户的主观归类，保暖度才是算法做温度匹配的量化依据。
 */
enum class Season(val label: String, val emoji: String) {
    SPRING("春", "🌱"),
    SUMMER("夏", "☀️"),
    AUTUMN("秋", "🍂"),
    WINTER("冬", "❄️")
}

/**
 * 风格标签。用于同风格加权，让推荐出来的整套搭配调性统一。
 */
enum class StyleTag(val label: String) {
    STREET("街头"),
    PREPPY("学院"),
    RETRO("复古"),
    MINIMAL("极简"),
    SWEET("甜美"),
    TECHWEAR("机能")
}

/**
 * 单品状态。「已处理」的单品不参与推荐，但仍保留在衣橱里用于统计。
 */
enum class ItemStatus(val label: String) {
    ACTIVE("在用"),
    RETIRED("已处理")
}
