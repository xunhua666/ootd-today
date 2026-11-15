package com.ootd.today.ui.community

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.Occasion
import com.ootd.today.ui.common.EmptyPlaceholder
import com.ootd.today.ui.common.ItemSwatch
import com.ootd.today.ui.theme.OotdColors
import com.ootd.today.ui.theme.PillShape
import java.io.File

/** 话题快选：穿搭场景的常用标签，点一下就追加到文案。 */
private val SUGGESTED_TOPICS = listOf(
    "ootd", "通勤穿搭", "约会穿搭", "街头", "复古", "极简", "撞色", "二手",
)

/**
 * 发布动态页。
 *
 * 结构：已选预览（含一件件可取消）→ 文案 → 场合 → 从衣橱选品 → 发布。
 * 把「已选预览」放在最上方，让用户在选品过程中随时能看到最终成品长什么样。
 */
@Composable
fun PublishPostScreen(
    viewModel: PublishPostViewModel,
    onPublished: () -> Unit,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onGalleryPhotoPicked(it) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // --- 顶栏 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(PillShape)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "发布穿搭",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(44.dp))
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载衣橱…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        if (state.wardrobe.isEmpty()) {
            EmptyPlaceholder(
                emoji = "🧺",
                message = "衣橱还是空的\n先去录入单品再来分享吧"
            )
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- 已选预览 ---
            item {
                Text(
                    text = "已选 ${state.selected.size} 件",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                if (state.selected.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "从下方衣橱点选单品",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.selected, key = { it.id }) { item ->
                            Column(
                                modifier = Modifier
                                    .width(70.dp)
                                    .clickable { viewModel.toggleItem(item) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box {
                                    ItemSwatch(
                                        item = item,
                                        modifier = Modifier.size(64.dp),
                                        emojiSize = 26.sp
                                    )
                                    // 右上角「取消选择」标记
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "×",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // --- 配图（可选）---
            item {
                Text(
                    text = "配图（选填）",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                PublishPhotoPicker(
                    photoPath = state.photoPath,
                    onPickCamera = onOpenCamera,
                    onPickGallery = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemove = viewModel::removePhoto
                )
            }

            // --- 文案 ---
            item {
                Text(
                    text = "说点什么（选填）",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.caption,
                    onValueChange = viewModel::onCaptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "今天为什么这么穿？支持 @好友 与 #话题",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(8.dp))
                // 话题快选：点一下自动追加到文案末尾。
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SUGGESTED_TOPICS.forEach { tag ->
                        val used = state.usedTopics.any { it.equals(tag, ignoreCase = true) }
                        TopicChip(
                            tag = tag,
                            used = used,
                            onClick = { viewModel.appendTopic(tag) }
                        )
                    }
                }
            }

            // --- 场合 ---
            item {
                Text(
                    text = "场合",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Occasion.entries.forEach { occasion ->
                        FilterChip(
                            selected = state.occasion == occasion,
                            onClick = { viewModel.onOccasionChange(occasion) },
                            label = {
                                Text(
                                    text = "${occasion.emoji} ${occasion.label}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            shape = PillShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // --- 从衣橱选品 ---
            item {
                Text(
                    text = "从衣橱选择",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            ClothingCategory.entries.forEach { category ->
                val items = state.wardrobe.filter { it.category == category }
                if (items.isEmpty()) return@forEach
                item(key = "category_${category.name}") {
                    Column {
                        Text(
                            text = "${category.emoji} ${category.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(items, key = { it.id }) { item ->
                                val isSelected = state.selected.any { it.id == item.id }
                                Column(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .clickable { viewModel.toggleItem(item) },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) OotdColors.current.brandGradientStart
                                                else Color.Transparent,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                    ) {
                                        ItemSwatch(
                                            item = item,
                                            modifier = Modifier.size(64.dp),
                                            emojiSize = 26.sp
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) OotdColors.current.brandGradientStart
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 错误提示 ---
            state.error?.let { message ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // --- 发布按钮 ---
        // 有单品或有配图即可发布（与 ViewModel 的校验规则保持一致）。
        Button(
            onClick = { viewModel.publish(onPublished) },
            enabled = !state.isPublishing &&
                (state.selected.isNotEmpty() || state.photoPath != null),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (state.isPublishing) "发布中…" else "发布到社区",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * 发布页的配图选择器。
 *
 * 与单品录入页同一套交互约定：不管有没有图都保留操作入口，
 * 有图时额外提供「移除」，避免用户选错了只能退出重来。
 */
@Composable
private fun PublishPhotoPicker(
    photoPath: String?,
    onPickCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        if (photoPath == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "🖼",
                    fontSize = 40.sp,
                    modifier = Modifier.align(Alignment.Center).padding(bottom = 28.dp)
                )
            }
        } else {
            AsyncImage(
                model = photoPath,
                contentDescription = "动态配图预览",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // 底部压一层渐变，保证叠在上面的按钮文字始终可读。
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                            startY = 360f
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val label = if (photoPath == null) "📷 拍照" else "📷 重拍"
            PublishPhotoButton(label, onClick = onPickCamera)
            Spacer(Modifier.width(10.dp))
            PublishPhotoButton(
                if (photoPath == null) "🖼 相册" else "🖼 换图",
                onClick = onPickGallery
            )
            if (photoPath != null) {
                Spacer(Modifier.width(10.dp))
                PublishPhotoButton("🗑 移除", onClick = onRemove)
            }
        }
    }
}

@Composable
private fun PublishPhotoButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(Color.White.copy(alpha = 0.28f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/** 话题快选 Chip。已使用的标签变灰且不可点。 */
@Composable
private fun TopicChip(tag: String, used: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(
                if (used) MaterialTheme.colorScheme.surfaceVariant
                else OotdColors.current.pinkContainer
            )
            .clickable(enabled = !used, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (used) "✓ #$tag" else "#$tag",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (used) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}