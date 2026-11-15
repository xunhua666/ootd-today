package com.ootd.today.ui.add

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag
import com.ootd.today.ui.common.readableOn
import com.ootd.today.ui.theme.OotdColors
import com.ootd.today.ui.theme.PillShape
import java.io.File

/** 预设色板，点一下就完成主色录入，比调取色器快得多。 */
private val presetColors: List<Pair<String, Long>> = listOf(
    "本白" to 0xFFF5F5F5L,
    "黑" to 0xFF1A1A1AL,
    "灰" to 0xFF9E9E9EL,
    "藏青" to 0xFF2F3542L,
    "中蓝" to 0xFF3A5F8AL,
    "雾霾蓝" to 0xFF7C9CBFL,
    "薄荷绿" to 0xFF00D9A5L,
    "柠檬黄" to 0xFFFFC400L,
    "珊瑚橙" to 0xFFFF6B35L,
    "玫粉" to 0xFFFF3D8BL,
    "藕粉" to 0xFFE8A0B4L,
    "焦糖" to 0xFFC9A876L,
    "驼色" to 0xFFB8A88AL,
    "燕麦" to 0xFFD9C7A7L,
    "酒红" to 0xFF8B2C3CL,
    "电光紫" to 0xFF5B4CFFL,
)

/** 预设色值集合，用于判断当前选中色是否在预设里。 */
private val presetColorValues: Set<Long> = presetColors.map { it.second }.toSet()

/**
 * 「录入单品」页。
 *
 * 拍照走应用内的 CameraX 相机（[onOpenCamera] 回调交给导航层打开），
 * 相册走系统的 Photo Picker。两者都会把结果拷贝/写入应用私有目录，
 * 因此数据库里只存持久有效的私有路径。
 */
@Composable
fun AddItemScreen(
    viewModel: AddItemViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier,
    editItemId: Long? = null,
    onOpenVisionSettings: () -> Unit = {},
    onBuildOutfit: (Long) -> Unit = { onSaved() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // 编辑模式：进入页面即把已有单品填进表单。
    LaunchedEffect(editItemId) {
        editItemId?.let { viewModel.loadForEdit(it) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        // 相册返回的 content:// 授权是临时的，交给 ViewModel 拷贝进私有目录。
        uri?.let { viewModel.onGalleryImagePicked(it) }
    }

    // 保存成功后返回上一页。
    LaunchedEffect(state.saved) {
        if (state.saved) {
            if (state.recommendAfterSave && state.savedItemId != null) onBuildOutfit(state.savedItemId!!) else onSaved()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(PillShape)
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            Text(
                text = if (state.isEditing) "编辑单品" else "录入单品",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            // 占位，让标题真正居中。
            Spacer(Modifier.width(44.dp))
        }

        Spacer(Modifier.height(18.dp))

        // --- 图片区 ---
        ImagePicker(
            imagePath = state.imagePath,
            category = state.category,
            colorArgb = state.colorArgb,
            onPickCamera = onOpenCamera,
            onPickGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRemovePhoto = viewModel::onRemovePhoto,
        )
        Spacer(Modifier.height(12.dp))
        GarmentShapeSection(state, settings.vision.isReady, runCatching { java.net.URI(settings.vision.baseUrl).host }.getOrNull().orEmpty(), onOpenVisionSettings,
            viewModel::analyzeShape, viewModel::cancelShapeAnalysis, viewModel::onShapeChange, viewModel::applyVisionDraft, viewModel::discardVisionDraft)

        // --- AI 识别提示 ---
        if (state.isRecognizing || state.recognitionHint != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(10.dp),
            ) {
                Text(
                    text = if (state.isRecognizing) "🤖 AI 正在识别品类与颜色…"
                    else "🤖 ${state.recognitionHint}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- 名称 ---
        SectionLabel("单品名称")
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("例如：雾霾蓝 oversize 卫衣") },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(18.dp))

        // --- 品类 ---
        SectionLabel("品类")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ClothingCategory.entries.forEach { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { viewModel.onCategoryChange(category) },
                    label = {
                        Text(
                            text = "${category.emoji} ${category.label}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.category.subCategories.forEach { sub ->
                FilterChip(
                    selected = state.subCategory == sub,
                    onClick = { viewModel.onSubCategoryChange(sub) },
                    label = { Text(sub, style = MaterialTheme.typography.labelSmall) },
                    shape = PillShape,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // --- 主色 ---
        SectionLabel("主色调")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            presetColors.forEach { (_, argb) ->
                val selected = state.colorArgb == argb
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        )
                        .clickable { viewModel.onColorPicked(argb) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Text("✓", color = readableOn(Color(argb)), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // AI 从照片里取到的颜色不一定落在预设里。不额外显示的话，
            // 界面上看不出到底选中了什么 —— 明明已经提示「已提取主色」。
            if (state.colorArgb !in presetColorValues) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(state.colorArgb))
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✓",
                        color = readableOn(Color(state.colorArgb)),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.colorName,
            onValueChange = viewModel::onColorNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("颜色名（留空自动生成）") },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(18.dp))

        // --- 保暖度 ---
        SectionLabel("保暖度 · ${warmthLabel(state.warmthLevel)}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { level ->
                val selected = state.warmthLevel == level
                val color = warmthColor(level)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(if (selected) color else color.copy(alpha = 0.22f))
                        .clickable { viewModel.onWarmthChange(level) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = level.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        // 黄色档上白字对比度不足，按背景亮度自动选前景色。
                        color = if (selected) readableOn(color) else color,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // --- 季节 ---
        SectionLabel("适用季节（可多选）")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Season.entries.forEach { season ->
                FilterChip(
                    selected = season in state.seasons,
                    onClick = { viewModel.toggleSeason(season) },
                    label = {
                        Text(
                            text = "${season.emoji} ${season.label}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    shape = PillShape,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // --- 场合 ---
        SectionLabel("适用场合（可多选）")
        Text(
            text = "推荐算法按场合硬性筛选，一件都没选就永远不会被推荐出来。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Occasion.entries.forEach { occasion ->
                FilterChip(
                    selected = occasion in state.occasions,
                    onClick = { viewModel.toggleOccasion(occasion) },
                    label = {
                        Text(
                            text = "${occasion.emoji} ${occasion.label}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OotdColors.current.cyan,
                        selectedLabelColor = readableOn(OotdColors.current.cyan),
                    ),
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // --- 风格 ---
        SectionLabel("风格（单选，可留空）")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyleTag.entries.forEach { tag ->
                FilterChip(
                    selected = state.style == tag,
                    onClick = { viewModel.toggleStyle(tag) },
                    label = { Text(tag.label, style = MaterialTheme.typography.labelMedium) },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OotdColors.current.pink,
                        selectedLabelColor = readableOn(OotdColors.current.pink),
                    ),
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // --- 品牌与价格 ---
        SectionLabel("品牌与价格（选填）")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.brand,
                onValueChange = viewModel::onBrandChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("品牌") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
            )
            OutlinedTextField(
                value = state.price,
                onValueChange = viewModel::onPriceChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("价格") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
            )
        }

        Spacer(Modifier.height(18.dp))

        // --- 备注 ---
        SectionLabel("备注（选填）")
        OutlinedTextField(
            value = state.note,
            onValueChange = viewModel::onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("例如：袖口有点起球、只适合春秋") },
            minLines = 2,
            shape = MaterialTheme.shapes.small,
        )

        // --- 错误提示 ---
        state.error?.let { message ->
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(12.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { viewModel.save() },
            enabled = !state.isSaving && !state.isAnalyzingShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(
                text = when {
                    state.isSaving -> "保存中…"
                    state.isEditing -> "保存修改"
                    else -> "保存到衣橱"
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (state.shape.confirmed && state.shape.labels.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Button(onClick = { viewModel.save(buildOutfit = true) }, enabled = !state.isSaving && !state.isAnalyzingShape, modifier = Modifier.fillMaxWidth().height(52.dp), shape = PillShape) { Text("保存并围绕它搭配") }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/**
 * 图片选择区。
 *
 * **无论有没有照片都保留操作入口**：拍错了需要能重拍，选错了需要能换，
 * 不想要照片了需要能移除。早期版本在有照片时隐藏了按钮，
 * 导致用户只能删掉整个单品重录，这是不可接受的。
 */
@Composable
private fun ImagePicker(
    imagePath: String?,
    category: ClothingCategory,
    colorArgb: Long,
    onPickCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(MaterialTheme.shapes.large)
            .background(Color(colorArgb))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large,
            ),
    ) {
        // 底层：有照片显示照片，没有则显示品类 emoji 占位。
        if (imagePath == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = category.emoji, fontSize = 44.sp)
            }
        } else {
            AsyncImage(
                model = imagePath,
                contentDescription = "单品照片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // 底部加一层浅渐变，保证叠在上面的按钮文字始终可读，
            // 否则遇到浅色照片时白字会看不见。
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                            startY = 400f,
                        )
                    )
            )
        }

        // 顶层：操作按钮固定在底部，不随内容变化而移动。
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val contentColor = if (imagePath == null) readableOn(Color(colorArgb)) else Color.White
            SourceButton(
                text = if (imagePath == null) "📷 拍照" else "📷 重拍",
                contentColor = contentColor,
                onClick = onPickCamera,
            )
            Spacer(Modifier.width(10.dp))
            SourceButton(
                text = if (imagePath == null) "🖼 相册" else "🖼 换图",
                contentColor = contentColor,
                onClick = onPickGallery,
            )
            if (imagePath != null) {
                Spacer(Modifier.width(10.dp))
                SourceButton(
                    text = "🗑 移除",
                    contentColor = Color.White,
                    onClick = onRemovePhoto,
                )
            }
        }

        // 无照片时把提示语放在中下方，避免与按钮重叠。
        if (imagePath == null) {
            Text(
                text = "不拍照也可以，会用主色卡表示",
                style = MaterialTheme.typography.labelSmall,
                color = readableOn(Color(colorArgb)).copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp),
            )
        }
    }
}

@Composable
private fun SourceButton(text: String, contentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(Color.White.copy(alpha = 0.28f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}

private fun warmthLabel(level: Int): String = when (level) {
    1 -> "很凉快（短袖短裤）"
    2 -> "偏凉（单层长袖）"
    3 -> "适中（长袖+薄外套）"
    4 -> "偏暖（厚外套/毛衣）"
    else -> "很保暖（羽绒服级别）"
}

private fun warmthColor(level: Int): Color = when (level) {
    1 -> Color(0xFF4FC3F7)
    2 -> Color(0xFF81D4FA)
    3 -> Color(0xFFFFC400)
    4 -> Color(0xFFFF9068)
    else -> Color(0xFFFF5252)
}


