package com.ootd.today.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ootd.today.ui.theme.OotdTodayTheme

/**
 * 富文本渲染的视觉验证。
 *
 * 覆盖设计上容易出错的三类输入：
 * 1. 提及与话题混排 —— 两者应分别着色且加粗
 * 2. 邮箱不得被误染色 —— `a@b.com` 应整体为普通文字
 * 3. 中文紧邻标记 —— `今天#通勤` 应当识别出话题
 */
@Preview(name = "富文本 · 浅色", showBackground = true, widthDp = 360)
@Composable
private fun SocialTextLightPreview() {
    OotdTodayTheme(darkTheme = false) {
        SocialTextPreviewContent()
    }
}

@Preview(name = "富文本 · 深色", showBackground = true, widthDp = 360)
@Composable
private fun SocialTextDarkPreview() {
    OotdTodayTheme(darkTheme = true) {
        SocialTextPreviewContent()
    }
}

@Composable
private fun SocialTextPreviewContent() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SocialText(
                text = "今天这套 @阿茶 觉得怎么样？#通勤穿搭 #ootd",
                style = MaterialTheme.typography.bodyMedium
            )
            SocialText(
                text = "邮箱不该被染色：a@b.com 是普通文字",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            SocialText(
                text = "中文紧邻标记也应识别：今天#通勤很不错",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            SocialText(
                text = "多行截断验证：@张三 @李四 @王五 这套搭配真的很适合周末逛街，颜色也好看 #街头 #ootd #周末",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}