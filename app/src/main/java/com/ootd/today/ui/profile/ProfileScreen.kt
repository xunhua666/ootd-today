package com.ootd.today.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.ootd.today.ui.common.EditorialHeader
import com.ootd.today.ui.theme.OotdColors
import com.ootd.today.ui.theme.PillShape
import kotlinx.coroutines.launch

/**
 * 「我的」页。
 *
 * P0 阶段主要是入口聚合 + 少量本地概览数据。
 * 「穿搭统计」的详细报表属于 P1，「穿搭社区」按方案约定做成占位页，
 * 但入口和 Repository 接口已预留，后续接后端时不用改导航结构。
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCommunity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        EditorialHeader("MY STYLE JOURNAL", "我的风格手记", "让每一次出门，都有自己的风格。")

        OverviewCard(itemCount = state.itemCount, logCount = state.logCount)

        Spacer(Modifier.height(20.dp))

        EntryRow(
            emoji = "📊",
            title = "穿搭统计",
            subtitle = "闲置率、性价比、色彩偏好、连续打卡",
            onClick = onOpenStats
        )
        EntryRow(
            emoji = "🌐",
            title = "穿搭社区",
            subtitle = "分享搭配、关注同好、互相点赞",
            onClick = onOpenCommunity
        )
        EntryRow(
            emoji = "🤖",
            title = "AI 智能识别",
            subtitle = "拍照后自动预填品类与主色",
            onClick = {
                scope.launch {
                    snackbarHostState.showSnackbar("在录入页拍照即可，识别会自动完成")
                }
            }
        )
        EntryRow(
            emoji = "⏰",
            title = "每日推送",
            subtitle = "每天早上提醒今日穿搭",
            onClick = onOpenSettings
        )
        EntryRow(
            emoji = "⚙️",
            title = "设置",
            subtitle = "推送时间、审美专家、数据说明",
            onClick = onOpenSettings
        )
        EntryRow(
            emoji = "ℹ️",
            title = "关于",
            subtitle = "今日穿搭 v1.5 · 你的随身衣橱",
            onClick = { scope.launch { snackbarHostState.showSnackbar("衣橱与日历保存在本机；天气及 AI 顾问会按需联网") } }
        )

        Spacer(Modifier.height(110.dp))
    }
}

/** 顶部概览卡，用品牌渐变承接首页的视觉调性。 */
@Composable
private fun OverviewCard(itemCount: Int, logCount: Int) {
    val gradient = Brush.linearGradient(
        colors = listOf(OotdColors.current.statGradientStart, OotdColors.current.statGradientEnd)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(gradient)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatBlock(value = itemCount.toString(), label = "件单品", contentColor = Color.White)
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )
        StatBlock(value = logCount.toString(), label = "次打卡", contentColor = Color.White)
    }
}

@Composable
private fun StatBlock(value: String, label: String, contentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun EntryRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 19.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        badge?.let {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        Text(
            text = "›",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
