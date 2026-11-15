package com.ootd.today.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ootd.today.ui.theme.OotdColors
import com.ootd.today.ui.theme.PillShape

/**
 * 话题标签行。
 *
 * 用于动态详情页：把这条动态包含的话题以 Chip 列出，点击即跳回动态流
 * 并按该话题筛选，让「看到感兴趣的话题 → 点进去看更多」形成闭环。
 */
@Composable
fun TopicRow(
    topics: List<String>,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        topics.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(OotdColors.current.pinkContainer)
                    .clickable { onTopicClick(tag) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "#$tag",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}