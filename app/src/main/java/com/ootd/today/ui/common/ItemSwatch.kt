package com.ootd.today.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ootd.today.data.model.WardrobeItem

/**
 * 单品色卡：有照片则显示照片，没有则回退到「主色块 + 品类 emoji」。
 *
 * 这是全 App 展示单品的统一入口（衣橱、今日推荐、日历、社区动态）
 * 因此在这里集中处理图片加载，调用方无需关心数据源是文件路径还是 content URI。
 *
 * 兜底策略：照片加载失败/不存在时，下面的色块与 emoji 仍然可见，
 * 不会出现空白方块。
 *
 * @param item 要渲染的单品。
 * @param emojiSize emoji 字号，也作为无照片时的占位图标大小。
 * @param shape 色卡形状。
 * @param showBorder 是否描边。浅色单品（如白T）在白底卡片上不描边会看不见。
 */
@Composable
fun ItemSwatch(
    item: WardrobeItem,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = 30.sp,
    shape: Shape = MaterialTheme.shapes.medium,
    showBorder: Boolean = true
) {
    val base = Color(item.colorArgb)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        ClothingArt(
            category = item.category,
            color = base,
            modifier = Modifier.fillMaxSize().padding(6.dp)
        )

        // 有照片则覆盖在色块之上；加载失败时 Coil 会保留底板，不会出现空白。
        item.imagePath?.let { path ->
            AsyncImage(
                model = path,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * 根据背景色亮度自动选择可读的前景色。
 *
 * 撞色主题下卡片底色可能是柠檬黄（需深色字）也可能是电光紫（需浅色字），
 * 写死颜色必然出现对比度不足的问题，因此按亮度动态决定。
 *
 * @param threshold 亮度阈值，0..1。低于此值视为深色背景。
 */
@Composable
fun readableOn(background: Color, threshold: Float = 0.55f): Color =
    if (background.luminance() > threshold) Color(0xFF1C1926) else Color.White

/**
 * 圆形小色点，用于展示单品主色调（详情页、颜色选择器）。
 */
@Composable
fun ColorDot(
    colorArgb: Long,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(colorArgb))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    )
}

/**
 * 空态占位。衣橱为空、无推荐结果时统一使用。
 */
@Composable
fun EmptyPlaceholder(
    emoji: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 56.sp)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, start = 32.dp, end = 32.dp)
        )
    }
}
