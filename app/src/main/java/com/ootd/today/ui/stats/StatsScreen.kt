package com.ootd.today.ui.stats

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
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.ui.common.DemoWardrobeBanner
import com.ootd.today.ui.common.ColorDot
import com.ootd.today.ui.common.ItemSwatch
import com.ootd.today.ui.theme.OotdColors
import com.ootd.today.ui.theme.PillShape

/**
 * 穿搭统计页。
 *
 * 指标全部来自 [StatsViewModel] 的派生计算：
 * 概览（单品/打卡/连续天数）、最常穿、闲置唤醒、色彩偏好、性价比、品类分布。
 */
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onBuildOutfit: (Long) -> Unit = {},
    onEditItem: (Long) -> Unit = {},
    onAddItem: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "穿搭统计",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(18.dp))

        if (state.isDemo) {
            DemoWardrobeBanner(onStart = { viewModel.startPersonalWardrobe(onAddItem) })
            Spacer(Modifier.height(16.dp))
        }
        OverviewCard(state)

        Spacer(Modifier.height(16.dp))
        Section("让统计变成下一步") {
            val pick = state.rewear
            when {
                state.activeItems == 0 -> {
                    Hint("先录入真实单品，这里会根据你的衣橱提供建议。")
                    Button(onClick=onAddItem) { Text("录入我的单品") }
                }
                pick != null -> {
                    Text("让「${pick.item.name}」多出场几次",style=MaterialTheme.typography.titleSmall)
                    Text("目前 ¥%.1f/次，再穿 5 次可降至 ¥%.1f/次。".format(pick.currentCost,pick.afterFiveMoreWears),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=8.dp))
                    Button(onClick={onBuildOutfit(pick.item.id)},modifier=Modifier.fillMaxWidth().height(48.dp)) { Text("围绕它搭一套") }
                    TextButton(onClick={onEditItem(pick.item.id)}) { Text("完善这件单品的信息") }
                }
                else -> {
                    Hint("给单品补充购入价格，打卡后可查看单次穿着成本。")
                    state.mostWorn.firstOrNull()?.let { item -> TextButton(onClick={onEditItem(item.id)}) { Text("为「${item.name}」补充价格") } }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Section("最常穿 TOP ${state.mostWorn.size}") {
            if (state.mostWorn.isEmpty()) {
                Hint("还没有穿着记录，去「今日」页打卡吧")
            } else {
                val max = state.mostWorn.maxOf { it.wearCount }
                state.mostWorn.forEachIndexed { index, item ->
                    WornRow(item = item, rank = index + 1, maxWear = max)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Section("闲置唤醒") {
            if (state.idleItems.isEmpty()) {
                Hint("太好了，衣橱里没有吃灰的单品")
            } else {
                Text("${state.idleItems.size} 件单品至少 30 天没有穿着记录，选一件重新搭配。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                state.idleItems.take(3).forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                        ItemSwatch(item,Modifier.size(54.dp))
                        Column(Modifier.weight(1f).padding(horizontal=10.dp)) {
                            Text(item.name,style=MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Text(if(item.wearCount==0) "还没有穿过" else "最近 30 天无穿着记录",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick={onBuildOutfit(item.id)}) { Text("搭一套") }
                    }
                }
            }
        }

        Section("色彩偏好") {
            if (state.topColors.isEmpty()) {
                Hint("录入单品后这里会展示你的色系倾向")
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.topColors.forEach { stat ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ColorDot(colorArgb = stat.argb, size = 28.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${stat.name} ×${stat.count}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Section("性价比之选（单次穿着成本）") {
            if (state.valuePicks.isEmpty()) {
                Hint("给单品填上价格并穿几次，这里会算出最值的几件")
            } else {
                state.valuePicks.forEach { pick ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ItemSwatch(
                            item = pick.item,
                            modifier = Modifier.size(34.dp),
                            emojiSize = 16.sp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = pick.item.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "¥%.1f/次".format(pick.costPerWear),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = OotdColors.current.accentText
                        )
                    }
                }
            }
        }

        Section("品类分布") {
            val maxCount = state.categoryCounts.maxOfOrNull { it.second } ?: 0
            if (maxCount == 0) {
                Hint("衣橱还是空的")
            } else {
                state.categoryCounts.forEach { (category, count) ->
                    CategoryBar(category = category, count = count, max = maxCount)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        Spacer(Modifier.height(110.dp))
    }
}

/** 顶部概览卡。 */
@Composable
private fun OverviewCard(state: StatsUiState) {
    val gradient = Brush.linearGradient(
        colors = listOf(OotdColors.current.statGradientStart, OotdColors.current.statGradientEnd)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(gradient)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OverviewStat(state.activeItems.toString(), "件在用")
        OverviewStat(state.totalLogs.toString(), "次打卡")
        OverviewStat("${state.streakDays}", "天连续")
        OverviewStat(state.idleItems.size.toString(), "件闲置")
    }
}

@Composable
private fun OverviewStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** 最常穿条目：排名 + 色卡 + 名称 + 次数条。 */
@Composable
private fun WornRow(item: WardrobeItem, rank: Int, maxWear: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = when (rank) {
                1 -> OotdColors.current.rankFirst
                2 -> OotdColors.current.rankSecond
                3 -> OotdColors.current.rankThird
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.width(24.dp)
        )
        ItemSwatch(item = item, modifier = Modifier.size(34.dp), emojiSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = item.wearCount.toFloat() / maxWear)
                        .height(6.dp)
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${item.wearCount}次",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 品类分布条，颜色取品类专属色。 */
@Composable
private fun CategoryBar(category: ClothingCategory, count: Int, max: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${category.emoji} ${category.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(64.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = count.toFloat() / max)
                    .height(10.dp)
                    .clip(PillShape)
                    .background(categoryColor(category))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 品类专属色，与衣橱筛选、图表保持一致的视觉语言。 */
@Composable
private fun categoryColor(category: ClothingCategory): Color = with(OotdColors.current) {
    when (category) {
        ClothingCategory.TOP -> categoryTop
        ClothingCategory.BOTTOM -> categoryBottom
        ClothingCategory.ONE_PIECE -> categoryOnePiece
        ClothingCategory.OUTERWEAR -> categoryOuterwear
        ClothingCategory.SHOES -> categoryShoes
        ClothingCategory.BAG -> categoryBag
        ClothingCategory.ACCESSORY -> categoryAccessory
    }
}


