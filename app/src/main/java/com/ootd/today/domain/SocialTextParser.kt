package com.ootd.today.domain

/**
 * 富文本片段。
 *
 * 把一段用户输入的纯文本切成「普通文字 / @提及 / #话题」三类，
 * UI 层据此对提及与话题施加不同样式并响应点击。
 */
sealed interface TextSegment {
    /** 普通文字。 */
    data class Plain(val text: String) : TextSegment

    /** @提及的昵称（不含 @ 符号）。 */
    data class Mention(val nickname: String) : TextSegment

    /** #话题标签（不含 # 符号）。 */
    data class Topic(val tag: String) : TextSegment
}

/**
 * 社交文本解析器：识别 @提及 与 #话题。
 *
 * 纯 Kotlin、无 Android 依赖，因此可直接跑 JVM 单测。
 *
 * ## 识别规则
 *
 * - `@` / `#` 后紧跟至少一个「词字符」才算命中。词字符 =
 *   字母、数字、下划线（`Character.isLetterOrDigit` 天然涵盖中日韩汉字）。
 * - `@` / `#` 前面**不能**是词字符，避免把邮箱（`a@b.com`）误判成提及。
 * - 话题在遇到空白或标点时结束，这样 `@阿茶，今天穿什么` 能正确切出
 *   昵称「阿茶」而不把逗号吃进去。
 * - 允许的连接符：昵称里的 `-`、`_`、`.` 会一并纳入，
 *   因为真实昵称常含这些字符（如 `Kai_99`）。
 */
object SocialTextParser {

    private const val MENTION = '@'
    private const val TOPIC = '#'

    /**
     * 把 [raw] 切成片段序列。
     *
     * 空字符串返回空列表（UI 层据此跳过渲染）。
     */
    fun parse(raw: String): List<TextSegment> {
        if (raw.isEmpty()) return emptyList()

        val segments = mutableListOf<TextSegment>()
        val plain = StringBuilder()
        var index = 0

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                segments += TextSegment.Plain(plain.toString())
                plain.clear()
            }
        }

        while (index < raw.length) {
            val marker = raw[index]
            val isMarker = marker == MENTION || marker == TOPIC

            if (isMarker) {
                // 「词内符号」检查只对 @ 生效：
                // 邮箱（a@b.com）里的 @ 前面是字母，必须整体当作普通文字；
                // 而 # 在自然语言里几乎不会嵌在单词中间，#a#b 应当识别为两个话题，
                // 因此不对 # 做此限制。（代价是 C# 会被当成话题，穿搭场景可忽略。）
                val blocked = marker == MENTION && isInsideWord(raw, index)
                if (!blocked) {
                    val token = readToken(raw, index + 1)
                    if (token.isNotEmpty()) {
                        flushPlain()
                        segments += if (marker == MENTION) {
                            TextSegment.Mention(token)
                        } else {
                            TextSegment.Topic(token)
                        }
                        index += token.length + 1
                        continue
                    }
                }
            }

            plain.append(marker)
            index++
        }
        flushPlain()
        return segments
    }

    /**
     * 判断位置 [at] 的 `@` 是否夹在西文词内部（即邮箱这类情况）。
     *
     * 只把 **ASCII 字母/数字** 视为词的一部分：
     * - `a@b.com` 中 `@` 前是 `a`，属于邮箱，不应识别为提及；
     * - 中文从不把 `@` 嵌在词内，因此 `你好@阿茶` 里的 `@` 应当正常识别。
     */
    private fun isInsideWord(raw: String, at: Int): Boolean {
        if (at == 0) return false
        return isAsciiWordChar(raw[at - 1])
    }

    /** ASCII 字母、数字、下划线。刻意不含中日韩字符。 */
    private fun isAsciiWordChar(c: Char): Boolean =
        (c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') || c == '_'

    /**
     * 提取全部话题标签，按**出现顺序去重**。
     *
     * 保留原始大小写用于展示，调用方做比较时应忽略大小写。
     */
    fun extractTopics(raw: String): List<String> =
        parse(raw).filterIsInstance<TextSegment.Topic>()
            .map { it.tag }
            .distinct()

    /** 提取全部被提及的昵称，按出现顺序去重。 */
    fun extractMentions(raw: String): List<String> =
        parse(raw).filterIsInstance<TextSegment.Mention>()
            .map { it.nickname }
            .distinct()

    /**
     * 从 [start] 起读取一个词，遇到非法字符即停。
     *
     * 允许 `-` / `_` / `.` 出现在词内部，但不允许作为首字符，
     * 避免把 `@，你好` 或 `#-` 这种误当成合法标签。
     */
    private fun readToken(raw: String, start: Int): String {
        var end = start
        while (end < raw.length) {
            val c = raw[end]
            val allowed = isWordChar(c) ||
                ((c == '-' || c == '_' || c == '.') && end > start)
            if (!allowed) break
            end++
        }
        return raw.substring(start, end)
    }

    /** 词字符：字母、数字、下划线。中日韩汉字属于字母，天然被涵盖。 */
    private fun isWordChar(c: Char): Boolean =
        Character.isLetterOrDigit(c) || c == '_'
}