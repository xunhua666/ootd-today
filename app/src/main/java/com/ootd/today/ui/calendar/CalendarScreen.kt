package com.ootd.today.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ootd.today.data.model.*
import com.ootd.today.ui.common.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: CalendarViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var deletePlan by remember { mutableStateOf<CalendarPlan?>(null) }
    var deleteLog by remember { mutableStateOf<OutfitLog?>(null) }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() } }
    Box(modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                EditorialHeader("MAKE ROOM FOR GOOD DAYS", "穿搭日历", "给每一个期待的日子，留一套好搭配。")
                Surface(color = StudioInk, shape = RoundedCornerShape(24.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryNumber("${state.monthPlanCount}", "本月日程")
                        SummaryNumber("${state.pendingPlanCount}", "待完成")
                        SummaryNumber("${state.currentDayLogs.size}", "穿搭记录")
                    }
                }
            }
            item {
                MonthCalendar(state.monthStart, state.logsByDay, state.weatherByDay, viewModel::previousMonth, viewModel::nextMonth,
                    selectedDate = state.selectedDate, plansByDay = state.plansByDay, onSelectDate = viewModel::selectDate, onToday = viewModel::today)
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(dateHeading(state.selectedDate), style = MaterialTheme.typography.titleLarge)
                        Text("${state.selectedPlans.size} 个日程 · ${state.selectedLogs.size} 条穿搭记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { viewModel.openPlan() }, contentPadding = PaddingValues(horizontal = 14.dp)) {
                        StudioIcon(StudioSymbol.ADD, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("新建日程")
                    }
                }
            }
            if (state.selectedPlans.isEmpty()) item {
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp)) {
                        Text("这一天，还留着空白", style = MaterialTheme.typography.titleMedium)
                        Text("约会、通勤、旅行，提前记下要去的地方，\n也给喜欢的衣服安排一次出场。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        TextButton(onClick = { viewModel.openPlan() }, contentPadding = PaddingValues(top = 12.dp, bottom = 0.dp)) { Text("写下第一个安排  →") }
                    }
                }
            }
            items(state.selectedPlans, key = { "plan-${it.id}" }) { plan ->
                PlanCard(plan, onEdit = { viewModel.openPlan(plan) }, onComplete = { viewModel.completePlan(plan) }, onDelete = { deletePlan = plan })
            }
            item {
                Text("当天穿搭", style = MaterialTheme.typography.titleMedium)
                if (state.selectedLogs.isEmpty()) Text("还没有打卡记录。实际穿过后，可在首页记录。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            items(state.selectedLogs, key = { "log-${it.id}" }) { log ->
                LogCard(log, state.weatherByDay[Calendar.getInstance().apply { timeInMillis = log.timestamp }.get(Calendar.DAY_OF_MONTH)], onDelete = { deleteLog = log })
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
    }
    state.editor.draft?.let { PlanEditor(it, state.wardrobe, state.editor.saving, state.editor.error, viewModel::closeEditor, viewModel::savePlan) }
    deletePlan?.let { plan -> ConfirmDelete("删除这个日程？", plan.title, onDismiss = { deletePlan = null }) { viewModel.deletePlan(plan.id); deletePlan = null } }
    deleteLog?.let { log -> ConfirmDelete("删除这条穿搭记录？", "删除后无法恢复。", onDismiss = { deleteLog = null }) { viewModel.deleteLog(log.id); deleteLog = null } }
}

@Composable
private fun SummaryNumber(number: String, caption: String) {
    Column {
        Text(number.padStart(2, '0'), fontSize = 29.sp, lineHeight = 34.sp, color = StudioLime, fontWeight = FontWeight.Medium)
        Text(caption, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .72f))
    }
}

@Composable
internal fun MonthCalendar(
    monthStart: Long, logsByDay: Map<Int, OutfitLog>, weatherByDay: Map<Int, DailyWeather>,
    onPrevious: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier,
    selectedDate: String = PlanDates.key(monthStart), plansByDay: Map<Int, List<CalendarPlan>> = emptyMap(),
    onSelectDate: (String) -> Unit = {}, onToday: () -> Unit = {}
) {
    val month = Calendar.getInstance().apply { timeInMillis = monthStart }
    val monthKey = PlanDates.key(monthStart).take(7)
    val today = PlanDates.key(System.currentTimeMillis())
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) { StudioIcon(StudioSymbol.ARROW, Modifier.rotate(180f), description = "上个月") }
                Text("${month.get(Calendar.YEAR)} 年 ${month.get(Calendar.MONTH) + 1} 月", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                TextButton(onClick = onToday, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("今天") }
                IconButton(onClick = onNext) { StudioIcon(StudioSymbol.ARROW, description = "下个月") }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            PlanDates.monthCells(monthStart).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { day ->
                        if (day == null) Spacer(Modifier.weight(1f).height(54.dp)) else {
                            val date = "$monthKey-${day.toString().padStart(2, '0')}"
                            val selected = date == selectedDate
                            val planCount = plansByDay[day].orEmpty().size
                            val log = logsByDay[day]
                            Column(Modifier.weight(1f).height(54.dp).padding(vertical = 2.dp).clip(RoundedCornerShape(15.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .then(if (date == today && !selected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(15.dp)) else Modifier)
                                .semantics(mergeDescendants = true) { contentDescription = "$date，${planCount}个日程，${if (log != null) "有" else "无"}穿搭记录"; this.selected = selected }
                                .clickable { onSelectDate(date) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text("$day", style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected || date == today) FontWeight.Bold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                Row(Modifier.height(12.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (planCount > 0) Box(Modifier.size(4.dp).clip(CircleShape).background(if (selected) StudioLime else MaterialTheme.colorScheme.primary))
                                    if (log != null) Box(Modifier.size(4.dp).clip(CircleShape).background(if (selected) Color.White else Color(log.outfit.items.firstOrNull()?.colorArgb ?: 0xFFC8B797)))
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(start = 9.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Text("  日程     ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFC8B797)))
                Text("  穿搭记录", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlanCard(plan: CalendarPlan, onEdit: () -> Unit, onComplete: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, onClick = onEdit) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.width(52.dp).padding(top = 4.dp)) {
                Text(plan.timeLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Box(Modifier.padding(start = 4.dp, top = 9.dp).width(2.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
            Column(Modifier.weight(1f).alpha(if (plan.completed) .62f else 1f)) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium, textDecoration = if (plan.completed) TextDecoration.LineThrough else TextDecoration.None)
                Text("${plan.occasion.label}${if (plan.location.isNotBlank()) " · ${plan.location}" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
                if (plan.note.isNotBlank()) Text(plan.note, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                if (plan.items.isNotEmpty()) {
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) { plan.items.take(5).forEach { ItemSwatch(it, Modifier.size(31.dp), emojiSize = 12.sp) } }
                    Text(plan.items.joinToString(" · ") { it.name }, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
                Text(if (plan.completed) "已完成" else "待完成", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Checkbox(checked = plan.completed, onCheckedChange = { onComplete() }, modifier = Modifier.semantics { contentDescription = if (plan.completed) "标记日程未完成" else "完成日程" })
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(38.dp)) { Text("⋯", fontSize = 25.sp, modifier = Modifier.semantics { contentDescription = "日程更多操作" }) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("编辑日程") }, onClick = { menu = false; onEdit() })
                        DropdownMenuItem(text = { Text("删除日程") }, onClick = { menu = false; onDelete() })
                    }
                }
            }
        }
    }
}

@Composable
internal fun LogCard(log: OutfitLog, weather: DailyWeather?, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudioIcon(StudioSymbol.CHECK, Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary)
                Text("  ${log.outfit.occasion.label} · ${SimpleDateFormat("HH:mm", Locale.CHINA).format(log.timestamp)}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                weather?.let { Text("${it.condition.label} ${it.lowC}°–${it.highC}°", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) { StudioIcon(StudioSymbol.CLOSE, Modifier.size(16.dp), description = "删除穿搭记录") }
            }
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { log.outfit.items.take(6).forEach { ItemSwatch(it, Modifier.size(42.dp), emojiSize = 18.sp) } }
            Text(log.outfit.items.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConfirmDelete(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) }, confirmButton = { TextButton(onClick = onConfirm) { Text("删除") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

private fun dateHeading(date: String): String = runCatching {
    SimpleDateFormat("M月d日 · EEEE", Locale.CHINA).format(PlanDates.calendar(date).time)
}.getOrDefault("这一天的安排")
