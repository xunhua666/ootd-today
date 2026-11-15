package com.ootd.today.ui.wardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.repository.WardrobeRepository
import com.ootd.today.ui.common.DemoWardrobeBanner
import com.ootd.today.ui.common.EmptyPlaceholder
import com.ootd.today.ui.common.ItemSwatch
import com.ootd.today.ui.common.readableOn
import com.ootd.today.ui.theme.PillShape
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val wardrobeRepository: WardrobeRepository
) : ViewModel() {

    private val _item = MutableStateFlow<WardrobeItem?>(null)
    val item: StateFlow<WardrobeItem?> = _item.asStateFlow()

    fun load(itemId: Long) {
        viewModelScope.launch { _item.value = wardrobeRepository.getItem(itemId) }
    }

    fun delete(onDone: () -> Unit) {
        val id = _item.value?.id ?: return
        viewModelScope.launch {
            wardrobeRepository.deleteItem(id)
            onDone()
        }
    }
}

/**
 * 单品详情页。
 *
 * 展示录入时填的全部字段，并提供删除入口。
 * 编辑功能留到 P1（需要复用录入表单，改造成本较高，P0 先保证「能看、能删」）。
 */
@Composable
fun ItemDetailScreen(
    itemId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onBuildOutfit: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ItemDetailViewModel = hiltViewModel()
) {
    val item by viewModel.item.collectAsStateWithLifecycle()

    // itemId 变化时重新加载。
    LaunchedEffect(itemId) { viewModel.load(itemId) }

    val current = item
    if (current == null) {
        EmptyPlaceholder(emoji = "🔍", message = "找不到这件单品\n可能已被删除", modifier = modifier)
        return
    }

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
                text = "单品详情",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(18.dp))

        // 主视觉：大色卡
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center
        ) {
            ItemSwatch(
                item = current,
                modifier = Modifier.fillMaxSize(),
                emojiSize = 76.sp,
                shape = MaterialTheme.shapes.large
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = current.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(current.colorArgb))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${current.colorName} · ${current.category.label} / ${current.subCategory}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(20.dp))

        DetailSection("已确认版型") { Text(current.shape.summary, style = MaterialTheme.typography.bodyMedium) }
        if (!current.isDemo) OutlinedButton(onClick = onBuildOutfit, modifier = Modifier.fillMaxWidth()) { Text("围绕这件搭一套") }

        DetailSection("保暖度") {
            WarmthBar(level = current.warmthLevel)
        }

        DetailSection("适用季节") {
            TagFlow(current.seasons.map { "${it.emoji} ${it.label}" })
        }

        DetailSection("适用场合") {
            TagFlow(current.occasions.map { "${it.emoji} ${it.label}" })
        }

        current.style?.let { style ->
            DetailSection("风格") { TagFlow(listOf(style.label)) }
        }

        DetailSection("购入信息") {
            val brand = current.brand ?: "未填写品牌"
            val price = current.price?.let { "¥${it.toInt()}" } ?: "未填写价格"
            Text(
                text = "$brand · $price",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        DetailSection("穿着情况") {
            Text(
                text = buildString {
                    append("累计穿过 ${current.wearCount} 次")
                    current.lastWornDate?.let { append(" · 最近一次 ${formatDate(it)}") }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        current.note?.let { note ->
            DetailSection("备注") {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (current.isDemo) {
            DemoWardrobeBanner(onStart = onEdit)
        } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = PillShape
            ) {
                Text("✏️ 编辑", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = { viewModel.delete(onBack) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = PillShape
            ) {
                Text(
                    text = "移除",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        content()
        Spacer(Modifier.height(8.dp))
    }
}

/** 保暖度条：5 格，点亮到当前等级。 */
@Composable
private fun WarmthBar(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { index ->
            val active = index <= level
            val color = when (index) {
                1 -> Color(0xFF4FC3F7)
                2 -> Color(0xFF81D4FA)
                3 -> Color(0xFFFFC400)
                4 -> Color(0xFFFF9068)
                else -> Color(0xFFFF5252)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(PillShape)
                    .background(if (active) color else color.copy(alpha = 0.18f))
            )
        }
    }
}

/** 通用标签流。 */
@Composable
private fun TagFlow(tags: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return "${c.get(Calendar.MONTH) + 1}月${c.get(Calendar.DAY_OF_MONTH)}日"
}
