package com.ootd.today.ui.common

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.ootd.today.domain.SocialTextParser
import com.ootd.today.domain.TextSegment
import com.ootd.today.ui.theme.OotdColors

/**
 * 富文本社交文本。
 *
 * 把 `@提及` 与 `#话题` 渲染成品牌色高亮，与普通文字区分。
 *
 * 实现说明：这里用 [AnnotatedString] + 单个 [Text]，
 * 而不是拼接多个 Text —— 后者会破坏自动换行，让长文案在提及处被硬断行，
 * 且无法正确计算省略号位置。
 */
@Composable
fun SocialText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val annotated = rememberSocialAnnotatedString(text)
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow
    )
}

/**
 * 构造带高亮的 [AnnotatedString]。
 *
 * 提及用主色、话题用粉色，两者都加粗以便在长文案中一眼可辨；
 * 颜色取自主题扩展色，因此深浅两种模式都自动适配。
 *
 * 普通文字不设 span —— 它们自然继承外层 [Text] 的 style，
 * 无需在此重复声明字重与颜色。
 */
@Composable
fun rememberSocialAnnotatedString(text: String): AnnotatedString {
    val mentionColor = MaterialTheme.colorScheme.primary
    val topicColor = OotdColors.current.pink

    return buildAnnotatedString {
        SocialTextParser.parse(text).forEach { segment ->
            when (segment) {
                is TextSegment.Plain -> append(segment.text)
                is TextSegment.Mention -> withStyle(
                    SpanStyle(color = mentionColor, fontWeight = FontWeight.Bold)
                ) {
                    append("@")
                    append(segment.nickname)
                }
                is TextSegment.Topic -> withStyle(
                    SpanStyle(color = topicColor, fontWeight = FontWeight.Bold)
                ) {
                    append("#")
                    append(segment.tag)
                }
            }
        }
    }
}