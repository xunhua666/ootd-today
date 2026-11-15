package com.ootd.today.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.ui.theme.OotdTodayTheme

/**
 * 单品色卡的视觉验证快照。
 *
 * 覆盖三种关键情形，便于在 IDE 里一眼确认渲染正确、且不依赖真机：
 * 1. 有照片 —— 应显示照片而非色块
 * 2. 无照片 —— 应显示「主色块 + 品类 emoji」
 * 3. 浅色单品 —— 描边应可见，否则白 T 在白卡片上会「消失」
 *
 * 「有照片」用系统 drawable 的 resource URI，因为 Preview 环境里
 * 没有应用的私有文件；Coil 同样支持 resource URI，因此能真实加载出图。
 */
@Preview(name = "单品色卡 · 三种形态", showBackground = true, heightDp = 220)
@Composable
private fun ItemSwatchStatesPreview() {
    OotdTodayTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreviewCell(
                    item = sampleItem(
                        name = "有照片",
                        color = 0xFF7C9CBF,
                        // 系统 drawable，Coil 能真实加载，用于验证「照片盖在色块之上」。
                        imagePath = "android.resource://com.ootd.today/mipmap/ic_launcher"
                    )
                )
                PreviewCell(
                    item = sampleItem(name = "无照片", color = 0xFF3A5F8A, imagePath = null)
                )
                PreviewCell(
                    item = sampleItem(name = "浅色描边", color = 0xFFF5F5F5, imagePath = null)
                )
            }
        }
    }
}

@Composable
private fun PreviewCell(item: WardrobeItem) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ItemSwatch(item = item, modifier = Modifier.size(72.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

private fun sampleItem(
    name: String,
    color: Long,
    imagePath: String?
) = WardrobeItem(
    id = 1,
    name = name,
    category = ClothingCategory.TOP,
    subCategory = "T恤",
    imagePath = imagePath,
    colorArgb = color,
    colorName = "测试",
    warmthLevel = 2,
    seasons = setOf(Season.SPRING),
    occasions = setOf(Occasion.COMMUTE),
    style = StyleTag.MINIMAL,
    brand = null,
    price = null,
    purchaseDate = null,
    lastWornDate = null,
    status = ItemStatus.ACTIVE,
    note = null,
    wearCount = 0
)