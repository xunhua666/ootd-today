package com.ootd.today.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ootd.today.data.model.*
import com.ootd.today.ui.common.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanEditor(
    initial: CalendarPlan, wardrobe: List<WardrobeItem>, saving: Boolean, error: String?,
    onDismiss: () -> Unit, onSave: (CalendarPlan) -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable(initial.id) { mutableStateOf(initial.title) }
    var date by rememberSaveable(initial.id) { mutableStateOf(initial.dateKey) }
    var minute by rememberSaveable(initial.id) { mutableStateOf(initial.startMinute) }
    var occasionName by rememberSaveable(initial.id) { mutableStateOf(initial.occasion.name) }
    var location by rememberSaveable(initial.id) { mutableStateOf(initial.location) }
    var note by rememberSaveable(initial.id) { mutableStateOf(initial.note) }
    var selected by rememberSaveable(initial.id) { mutableStateOf(initial.items.map { it.id }) }
    var chooseItems by rememberSaveable(initial.id) { mutableStateOf(false) }
    var search by rememberSaveable(initial.id) { mutableStateOf("") }
    val choices = (wardrobe + initial.items).distinctBy { it.id }
    val selectedItems = selected.mapNotNull { id -> choices.find { it.id == id } }
    val latestSaving by rememberUpdatedState(saving)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !latestSaving })
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (initial.id == 0L) "写下新的期待" else "编辑这一天的安排", style = MaterialTheme.typography.titleLarge)
                    Text("把行程和穿什么，一起准备好。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, enabled = !saving) { StudioIcon(StudioSymbol.CLOSE, description = "关闭日程编辑") }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                OutlinedTextField(title, { title = it.take(60) }, modifier = Modifier.fillMaxWidth(), label = { Text("日程名称") }, placeholder = { Text("例如：周五的画展约会") }, singleLine = true, enabled = !saving)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        val c = PlanDates.calendar(date)
                        DatePickerDialog(context, { _, y, m, d -> date = "%04d-%02d-%02d".format(java.util.Locale.ROOT, y, m + 1, d) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }, modifier = Modifier.weight(1f), enabled = !saving) { Text(date) }
                    OutlinedButton(onClick = { TimePickerDialog(context, { _, h, m -> minute = h * 60 + m }, (minute ?: 9 * 60) / 60, (minute ?: 9 * 60) % 60, true).show() }, modifier = Modifier.weight(1f), enabled = !saving) {
                        Text(minute?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "全天 · 设置时间")
                    }
                }
                if (minute != null) TextButton(onClick = { minute = null }, enabled = !saving, contentPadding = PaddingValues(0.dp)) { Text("改为全天日程") }
                Text("这次是什么场合？", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Occasion.entries.forEach { occasion -> FilterChip(selected = occasionName == occasion.name, onClick = { occasionName = occasion.name }, label = { Text(occasion.label) }, enabled = !saving) }
                }
                OutlinedTextField(location, { location = it.take(100) }, modifier = Modifier.fillMaxWidth(), label = { Text("地点（选填）") }, singleLine = true, enabled = !saving)
                OutlinedTextField(note, { note = it.take(500) }, modifier = Modifier.fillMaxWidth(), label = { Text("计划与备注（选填）") }, placeholder = { Text("想做什么、搭配灵感、出门前要准备的事…") }, minLines = 2, maxLines = 4, enabled = !saving)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("提前挑好衣服", style = MaterialTheme.typography.titleSmall)
                        Text(if (selectedItems.isEmpty()) "可以先定行程，再慢慢选衣服" else "已选 ${selectedItems.size} 件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { chooseItems = !chooseItems }, enabled = !saving) { Text(if (chooseItems) "收起" else "挑选单品") }
                }
                if (selectedItems.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    selectedItems.forEach { item -> InputChip(selected = true, onClick = { selected = selected - item.id }, label = { Text(item.name) }, trailingIcon = { StudioIcon(StudioSymbol.CLOSE, Modifier.size(12.dp)) }, enabled = !saving) }
                }
                if (chooseItems) {
                    if (choices.isEmpty()) Text("录入真实单品后，就可以在这里预选衣服。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        OutlinedTextField(search, { search = it }, label = { Text("搜索衣服名称或颜色") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        val matching = choices.filter { it.name.contains(search, true) || it.colorName.contains(search, true) }
                        if (matching.isEmpty()) Text("没有找到这件衣服，换个关键词试试。", style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            matching.take(12).forEach { item -> FilterChip(selected = item.id in selected, onClick = { selected = if (item.id in selected) selected - item.id else selected + item.id }, enabled = !saving, label = { Text(item.name) }) }
                        }
                        if (matching.size > 12) Text("输入关键词，可以找到更多单品。", style = MaterialTheme.typography.labelSmall)
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(12.dp))
            }
            Button(onClick = { onSave(initial.copy(title = title.trim(), dateKey = date, startMinute = minute, occasion = Occasion.valueOf(occasionName), location = location, note = note, items = selectedItems)) }, enabled = title.isNotBlank() && !saving, modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp).height(52.dp)) {
                Text(if (saving) "正在保存…" else "保存日程")
            }
        }
    }
}
