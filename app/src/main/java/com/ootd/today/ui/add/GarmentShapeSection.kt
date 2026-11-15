package com.ootd.today.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ootd.today.data.model.*
import com.ootd.today.domain.vision.GarmentVisionResult

@Composable
fun GarmentShapeSection(state: AddItemUiState, ready: Boolean, destination: String, onConfigure: () -> Unit, onRecognize: () -> Unit, onCancel: () -> Unit, onChange: (GarmentShape) -> Unit, onApply: () -> Unit, onDiscard: () -> Unit) {
    var confirmUpload by remember { mutableStateOf(false) }
    var manual by rememberSaveable { mutableStateOf(false) }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("从照片读懂版型", style = MaterialTheme.typography.titleMedium)
            Text("识别松紧、衣长、腰高、裤裙轮廓与肩型。结果需确认后才参与搭配。", style = MaterialTheme.typography.bodySmall)
            if (state.shape.confirmed) Text("当前版型：${state.shape.summary}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (state.imagePath == null) Text("先拍照或选一张单件衣服的清晰照片。", style = MaterialTheme.typography.bodySmall)
            else if (!ready) TextButton(onClick = onConfigure) { Text("配置云端视觉服务") }
            else if (state.isAnalyzingShape) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("正在观察这张衣物照片…", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onCancel) { Text("取消识别") }
            } else Button(onClick = { confirmUpload = true }) { Text("云端识别这张照片") }
            state.visionError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = { manual = !manual }) { Text(if (manual) "收起版型选项" else "手动填写／修改版型") }
            if (manual) ShapeFields(state.category, state.shape, onChange)
        }
    }
    if (confirmUpload) AlertDialog(onDismissRequest = { confirmUpload = false }, title = { Text("识别当前这张衣服照片？") }, text = {
        Text("将压缩后的当前照片发送到：$destination。不会附带其他衣橱照片或对话。调用可能产生服务商费用。请尽量选择衣服主体清晰、无遮挡的照片。")
    }, confirmButton = { TextButton(onClick = { confirmUpload = false; onRecognize() }) { Text("确认上传并识别") } }, dismissButton = { TextButton(onClick = { confirmUpload = false }) { Text("取消") } })
    state.visionDraft?.let { draft -> VisionReview(draft, onApply, onDiscard) }
}

@Composable
private fun VisionReview(draft: GarmentVisionResult, onApply: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(onDismissRequest = onDiscard, title = { Text("先确认，再参与搭配") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${draft.name} · ${draft.category?.label.orEmpty()}")
            Text(draft.shape.labels.joinToString(" · ").ifBlank { "未能可靠判断版型，仍保留为不确定" }, style = MaterialTheme.typography.titleSmall)
            if (draft.evidence.isNotBlank()) Text("可见依据：${draft.evidence}")
            if (draft.warning.isNotBlank()) Text(draft.warning, color = MaterialTheme.colorScheme.primary)
            Text("没有识别到的属性会保留为“不确定”。采用后仍可展开版型选项修改。", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = onApply) { Text("采用结果并继续") } }, dismissButton = { TextButton(onClick = onDiscard) { Text("暂不采用") } })
}

@Composable
private fun ShapeFields(category: ClothingCategory, shape: GarmentShape, onChange: (GarmentShape) -> Unit) {
    if (category !in setOf(ClothingCategory.TOP, ClothingCategory.OUTERWEAR, ClothingCategory.BOTTOM, ClothingCategory.ONE_PIECE)) {
        Text("鞋、包和配饰暂不使用衣服版型标签。", style = MaterialTheme.typography.bodySmall); return
    }
    ShapeChoice("松紧／廓量", GarmentFit.entries, shape.fit, { it.label }) { onChange(shape.copy(fit = it)) }
    ShapeChoice("衣长／裤裙长度", GarmentLength.entries, shape.length, { it.label }) { onChange(shape.copy(length = it)) }
    if (category == ClothingCategory.BOTTOM) {
        ShapeChoice("腰高", WaistRise.entries, shape.rise, { it.label }) { onChange(shape.copy(rise = it)) }
        ShapeChoice("裤／裙轮廓", GarmentCut.entries, shape.cut, { it.label }) { onChange(shape.copy(cut = it)) }
    }
    if (category == ClothingCategory.ONE_PIECE) ShapeChoice("裙身轮廓", listOf(GarmentCut.UNKNOWN, GarmentCut.A_LINE), shape.cut, { it.label }) { onChange(shape.copy(cut = it)) }
    if (category != ClothingCategory.BOTTOM) ShapeChoice("肩型", ShoulderShape.entries, shape.shoulder, { it.label }) { onChange(shape.copy(shoulder = it)) }
}

@Composable
private fun <T> ShapeChoice(label: String, values: List<T>, selected: T, title: (T) -> String, onSelect: (T) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value -> FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(title(value)) }) }
    }
}
