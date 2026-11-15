package com.ootd.today.ui.expert

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ootd.today.R
import com.ootd.today.domain.stylist.LocalModel
import com.ootd.today.ui.common.StudioIcon
import com.ootd.today.ui.common.StudioSymbol

@Composable
fun ExpertMascot(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.expert_mascot), contentDescription = null, modifier = modifier)
}

@Composable
fun ExpertEntry(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 18.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 18.dp)) {
                Text("你的口袋审美专家", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(name.ifBlank { "给灵感，取个名字" }, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 5.dp))
                Text("聊配色，也聊今天怎么穿", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text("点击和 TA 见面  →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            ExpertMascot(Modifier.width(112.dp).height(158.dp))
        }
    }
}

@Composable
fun ExpertScreen(viewModel: ExpertViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val list = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var modelPanel by rememberSaveable { mutableStateOf(true) }
    var editProfile by rememberSaveable { mutableStateOf(false) }
    var download by remember { mutableStateOf<LocalModel?>(null) }
    var deleting by remember { mutableStateOf<LocalModel?>(null) }
    var importId by rememberSaveable { mutableStateOf(LocalModel.RECOMMENDED.id) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(LocalModel.find(importId), uri)
    }
    val canChat = state.models.any { it.model == state.selected && it.ready } && profile.expertName.isNotBlank()
    LaunchedEffect(canChat) { if (canChat) modelPanel = false }
    LaunchedEffect(state.messages.size, state.draftReply.isNotBlank()) {
        if (state.messages.isNotEmpty()) list.animateScrollToItem((list.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { StudioIcon(StudioSymbol.ARROW, Modifier.rotate(180f), description = "返回") }
            Column(Modifier.weight(1f)) {
                Text(profile.expertName.ifBlank { "我的审美专家" }, style = MaterialTheme.typography.titleMedium)
                Text("手机本地运行 · 对话不上传", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = { editProfile = true }) { Text("取名与偏好") }
        }
        LazyColumn(state = list, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExpertMascot(Modifier.width(110.dp).height(165.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("把好品味，\n装进口袋。", fontSize = 27.sp, lineHeight = 35.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Text("从已有衣服里找灵感，\n每次给你具体的小建议。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (profile.expertName.isBlank()) item {
                ProfileEditor("", profile.expertPreferences, "先给 TA 起个名字", onSave = viewModel::saveProfile)
            }
            item {
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("本地模型", style = MaterialTheme.typography.titleSmall)
                                Text(state.selected.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { modelPanel = !modelPanel }) { Text(if (modelPanel) "收起" else "管理") }
                        }
                        if (modelPanel) {
                            Text("量化文本模型 + 穿搭方法。优先试用推荐档，速度取决于手机芯片与可用内存。", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))
                            state.models.forEach { info ->
                                val model = info.model
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = state.selected == model, onClick = { viewModel.select(model) }, enabled = !state.busy && !state.fileBusy)
                                    Column(Modifier.weight(1f)) {
                                        Text(model.title, style = MaterialTheme.typography.titleSmall)
                                        Text("${model.sizeLabel} · INT4 · Apache 2.0", style = MaterialTheme.typography.labelSmall)
                                        Text(model.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(info.message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                                if (info.downloading) {
                                    LinearProgressIndicator(progress = { (info.received.toFloat() / model.bytes).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                                    Text("${info.received.coerceAtLeast(0) / 1_000_000} / ${model.bytes / 1_000_000} MB", style = MaterialTheme.typography.labelSmall)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    when {
                                        info.ready -> TextButton(onClick = { deleting = model }, enabled = !state.busy && !state.fileBusy) { Text("删除模型") }
                                        info.downloading -> TextButton(onClick = { viewModel.cancelDownload(model) }, enabled = !state.fileBusy) { Text("取消下载") }
                                        else -> {
                                            Button(onClick = { download = model }, enabled = !state.fileBusy && !state.busy) { Text("下载模型") }
                                            TextButton(onClick = { importId = model.id; importer.launch(arrayOf("*/*")) }, enabled = !state.fileBusy && !state.busy) { Text("导入文件") }
                                        }
                                    }
                                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, model.sourceUrl.toUri())) }) { Text("来源") }
                                }
                                if (model != state.models.last().model) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                            }
                            if (state.fileBusy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在处理文件与校验，请稍候…", style = MaterialTheme.typography.bodySmall) }
                            Text("下载使用 Hugging Face 官方文件。无法连接时，可从来源页取得同名文件后导入。文件大小不等于运行内存；首次建议关闭大型应用。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Text("我根据衣橱和你确认的版型给建议。照片识别请在单品录入／编辑页使用。对话仅保留在本次会话，偏好保存在手机。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.messages.isEmpty()) {
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { modelPanel = false; viewModel.send("请点评系统给出的这套搭配，给我两个可以照着做的小建议。") }, label = { Text("点评今天这套") }, enabled = canChat && !state.busy)
                        AssistChip(onClick = { modelPanel = false; viewModel.send("教我一组适合日常通勤的配色，并说明使用比例。") }, label = { Text("聊聊配色") }, enabled = canChat && !state.busy)
                    }
                }
            }
            if (state.outfit.isNotEmpty()) item {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Text(if (state.isDemo) "体验衣橱 · 本次参考搭配" else "本次参考搭配", style = MaterialTheme.typography.labelMedium)
                        Text(state.outfit.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
            items(state.messages) { turn ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (turn.user) Arrangement.End else Arrangement.Start) {
                    Surface(shape = RoundedCornerShape(20.dp), color = if (turn.user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, modifier = Modifier.widthIn(max = 360.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            if (!turn.user) Text(profile.expertName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 6.dp))
                            SelectionContainer { Text(turn.text.replace("**", ""), style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
            if (state.busy) item {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(state.draftReply.ifBlank { state.status }.replace("**", ""), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = viewModel::stop) { Text("停止生成") }
                    }
                }
            }
            state.error?.let { error -> item {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(error, style = MaterialTheme.typography.bodySmall); TextButton(onClick = viewModel::clearError) { Text("知道了") } }
                }
            } }
            if (state.messages.isNotEmpty()) item { TextButton(onClick = viewModel::clearChat, enabled = !state.busy) { Text("清空本次对话") } }
        }
        Surface(tonalElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = input, onValueChange = { input = it.take(500) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), maxLines = 3,
                    placeholder = { Text(if (canChat) "想怎么穿，和我说说…" else "取名并下载模型后，就能聊天") }, enabled = canChat && !state.busy && !state.fileBusy)
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { modelPanel = false; viewModel.send(input); input = "" }, enabled = canChat && !state.busy && !state.fileBusy && input.isNotBlank()) { StudioIcon(StudioSymbol.ARROW, description = "发送", color = MaterialTheme.colorScheme.onPrimary) }
            }
        }
    }
    if (editProfile) AlertDialog(onDismissRequest = { editProfile = false }, confirmButton = {}, text = {
        ProfileEditor(profile.expertName, profile.expertPreferences, "名字与穿衣偏好") { name, preferences -> viewModel.saveProfile(name, preferences); editProfile = false }
    })
    download?.let { model ->
        var wifi by remember(model) { mutableStateOf(true) }
        var mirror by remember(model) { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { download = null }, title = { Text("把专家请到手机里") }, text = {
            Column {
                Text("${model.title}\n下载 ${model.sizeLabel}，需额外预留空间。下载后校验 SHA-256，通过后可离线聊天。\n模型来自 LiteRT Community，采用 Apache 2.0 许可。")
                Row(Modifier.fillMaxWidth().toggleable(value = wifi, role = Role.Checkbox, onValueChange = { wifi = it }), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = wifi, onCheckedChange = null); Text("仅通过 Wi-Fi 下载") }
                Row(Modifier.fillMaxWidth().toggleable(value = mirror, role = Role.Checkbox, onValueChange = { mirror = it }), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = mirror, onCheckedChange = null); Text("使用社区镜像 hf-mirror.com") }
                Text("镜像是第三方下载渠道，文件仍须匹配官方 SHA-256。", style = MaterialTheme.typography.labelSmall)
            }
        }, confirmButton = { TextButton(onClick = { viewModel.download(model, wifi, mirror); download = null }) { Text("开始下载") } }, dismissButton = { TextButton(onClick = { download = null }) { Text("稍后") } })
    }
    deleting?.let { model -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除本地模型？") }, text = { Text("将释放约 ${model.sizeLabel} 空间，专家名字和穿衣偏好会保留。需要时可重新下载。") }, confirmButton = { TextButton(onClick = { viewModel.delete(model); deleting = null }) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }) }
}

@Composable
private fun ProfileEditor(initialName: String, initialPreferences: String, title: String, onSave: (String, String) -> Unit) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var preferences by rememberSaveable(initialPreferences) { mutableStateOf(initialPreferences) }
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(name, { name = it.take(16) }, label = { Text("专家名字 · 由你来定") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(preferences, { preferences = it.take(300) }, label = { Text("偏好（选填）") }, placeholder = { Text("例如：喜欢简洁配色，日常通勤优先舒适") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onSave(name.trim(), preferences.trim()) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("保存") }
        }
    }
}
