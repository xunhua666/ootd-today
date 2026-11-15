package com.ootd.today.ui.today

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ootd.today.data.model.*
import com.ootd.today.domain.OutfitCandidate
import com.ootd.today.ui.common.*
import com.ootd.today.ui.theme.PillShape
import com.ootd.today.ui.expert.ExpertEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(viewModel: TodayViewModel, modifier: Modifier = Modifier, onAddItem: () -> Unit = {}, onOpenSettings: () -> Unit = {}, onOpenExpert: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val expertProfile by viewModel.expertProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) viewModel.refresh() }
    val requestLocation = {
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) viewModel.refresh()
        else locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    fun message(text: String) { scope.launch { snackbar.showSnackbar(text) } }
    LaunchedEffect(state.stylistError) { state.stylistError?.let { snackbar.showSnackbar(it);viewModel.clearStylistError() } }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=22.dp)) {
            Row(Modifier.fillMaxWidth().padding(top=17.dp,bottom=22.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(SimpleDateFormat("MM 月 dd 日 · EEEE",Locale.CHINA).format(Date()),fontSize=11.sp,letterSpacing=1.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment=Alignment.Bottom) {
                        Text("OOTD",fontSize=40.sp,lineHeight=46.sp,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,letterSpacing=(-2).sp,color=MaterialTheme.colorScheme.primary)
                        Text("今日穿搭",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(start=10.dp,bottom=7.dp))
                    }
                    Text("好好穿衣，慢慢生活。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.size(46.dp).clip(CircleShape).background(StudioLime),contentAlignment=Alignment.Center) { StudioIcon(StudioSymbol.SPARK,color=StudioInk) }
            }
            if (state.isDemo) {
                DemoWardrobeBanner(onStart = { viewModel.startPersonalWardrobe(onAddItem) })
                Spacer(Modifier.height(16.dp))
            }
            state.requiredItem?.let { focused ->
                Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(start=14.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("围绕这件搭一套",style=MaterialTheme.typography.labelSmall)
                            Text(focused.name,style=MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                        IconButton(onClick=viewModel::clearFocusItem) { StudioIcon(StudioSymbol.CLOSE,description="取消指定单品") }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            if(state.isLoading) {
                Box(Modifier.fillMaxWidth().height(260.dp),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
            } else {
                state.weather?.let { WeatherCard(it,!state.usingDeviceLocation,requestLocation) } ?: Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(22.dp)) {
                        Text("天气暂时未连接",style=MaterialTheme.typography.titleMedium)
                        TextButton(onClick=viewModel::refresh) { Text("点击重试") }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top=24.dp,bottom=9.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("今天，想去哪里？",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onBackground)
                    Text("DRESS FOR YOUR DAY",fontSize=8.sp,letterSpacing=1.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(Occasion.entries.size) { i ->
                        val occasion=Occasion.entries[i]
                        FilterChip(selected=state.occasion==occasion,onClick={viewModel.selectOccasion(occasion)},label={Text(occasion.label)},shape=PillShape,
                            colors=FilterChipDefaults.filterChipColors(selectedContainerColor=MaterialTheme.colorScheme.primary,selectedLabelColor=MaterialTheme.colorScheme.onPrimary))
                    }
                }
                Spacer(Modifier.height(18.dp))
                val candidate=state.recommendation
                if(candidate==null) {
                    Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface) {
                        Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            ClothingArt(ClothingCategory.TOP,MaterialTheme.colorScheme.primary,Modifier.size(110.dp))
                            Text("每一件，都有新可能",style=MaterialTheme.typography.titleLarge)
                            Text(if(state.requiredItem != null) "这件单品暂时无法组成适合当前天气与场合的完整搭配。\n可以换个场合、补充衣橱，或取消指定单品。" else if(state.wardrobeTooSmall) "录入上装、下装和鞋，或尝试其他场合，\n让衣橱为你搭出今天的灵感。" else "天气准备好后，穿搭灵感就会出现。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=14.dp))
                            Button(onClick=onAddItem,shape=PillShape) {Text("录入我的单品");Spacer(Modifier.width(8.dp));StudioIcon(StudioSymbol.ADD,color=MaterialTheme.colorScheme.onPrimary)}
                        }
                    }
                } else {
                    RecommendationCard(candidate,state.fromStylist,state.stylistHeadline,state.occasion)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick={if(state.isDemo) viewModel.startPersonalWardrobe(onAddItem) else viewModel.adoptCurrent { ok -> message(if(ok) "已记录到穿搭日历，今天也要好好穿衣。" else "记录失败，请稍后重试") }},enabled=!state.isSaving,modifier=Modifier.fillMaxWidth().height(54.dp),shape=PillShape) {
                        StudioIcon(StudioSymbol.CHECK,color=MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(10.dp));Text(if(state.isSaving) "正在记录…" else if(state.isDemo) "录入我的单品，开始真实打卡" else "就穿这套 · 记录今日")
                    }
                    Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick=viewModel::shuffle,modifier=Modifier.weight(1f).height(46.dp),contentPadding=PaddingValues(8.dp)) { StudioIcon(StudioSymbol.REFRESH,Modifier.size(17.dp),color=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(6.dp));Text("换一套",fontSize=12.sp) }
                        OutlinedButton(onClick={viewModel.rateCurrent(true);message("已记住你的喜好，推荐会更贴近你")},modifier=Modifier.weight(1f).height(46.dp),contentPadding=PaddingValues(8.dp)) { StudioIcon(StudioSymbol.HEART,Modifier.size(17.dp),color=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(6.dp));Text("喜欢",fontSize=12.sp) }
                        OutlinedButton(onClick={viewModel.rateCurrent(false);message("已减少这类搭配的推荐")},modifier=Modifier.weight(1f).height(46.dp),contentPadding=PaddingValues(8.dp)) { Text("换种风格",fontSize=12.sp) }
                    }
                    TextButton(onClick={
                        val text=buildString { append("我的今日穿搭 · ${state.occasion.label}\n");candidate.items.forEach { append("${it.category.label}：${it.name}\n") };append("\n${candidate.reason}\n来自 OOTD 今日穿搭") }
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain";putExtra(Intent.EXTRA_TEXT,text) },"分享今日穿搭"))
                    },modifier=Modifier.align(Alignment.CenterHorizontally)) { StudioIcon(StudioSymbol.SHARE,Modifier.size(16.dp),color=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(6.dp));Text("分享穿搭清单",fontSize=12.sp) }
                    Spacer(Modifier.height(8.dp))
                    if(state.requiredItem != null) {
                        Text("正在用本地搭配算法围绕指定单品生成方案。取消指定后，可向 AI 审美专家咨询。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if(state.stylistReady) {
                        StylistBar(state.stylistReady,state.isConsulting,state.fromStylist,viewModel::askStylist,viewModel::askStylistFollowUp,name=expertProfile.expertName.ifBlank { "审美专家" })
                    } else {
                        Surface(onClick=onOpenExpert,shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
                            Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                                StudioIcon(StudioSymbol.SPARK,color=MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                                    Text("你的专属穿搭顾问",style=MaterialTheme.typography.titleSmall)
                                    Text("下载本地专家，聊聊配色与风格",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StudioIcon(StudioSymbol.ARROW,color=MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(132.dp))
        }
        SnackbarHost(snackbar,Modifier.align(Alignment.BottomCenter).padding(bottom=118.dp))
    }
}

@Composable
private fun WeatherCard(weather: Weather, showLocationHint: Boolean, onRequestLocation: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(StudioInk)) {
        Canvas(Modifier.matchParentSize()) {
            val center=Offset(size.width*.86f,size.height*.48f)
            drawCircle(StudioLime.copy(alpha=.07f),size.width*.31f,center)
            drawCircle(StudioLime.copy(alpha=.12f),size.width*.23f,center,style=Stroke(1.dp.toPx()))
            val radius = size.width * .065f
            if (weather.condition == WeatherCondition.SUNNY) {
                drawCircle(StudioLime, radius, center)
                for (i in 0..7) {
                    val angle = i * Math.PI / 4
                    val inner = radius * 1.35f
                    val outer = radius * 1.65f
                    drawLine(StudioLime, Offset(center.x + (inner * kotlin.math.cos(angle)).toFloat(), center.y + (inner * kotlin.math.sin(angle)).toFloat()), Offset(center.x + (outer * kotlin.math.cos(angle)).toFloat(), center.y + (outer * kotlin.math.sin(angle)).toFloat()), 2.dp.toPx())
                }
            } else {
                drawCircle(StudioLime.copy(alpha=.8f), radius * .8f, Offset(center.x-radius*.7f,center.y))
                drawCircle(StudioLime, radius, Offset(center.x,center.y-radius*.4f))
                drawCircle(StudioLime, radius*.7f, Offset(center.x+radius*.8f,center.y))
                if (weather.condition.isRainy) {
                    for (i in -1..1) drawLine(StudioLime, Offset(center.x+i*radius*.7f,center.y+radius*1.1f),Offset(center.x+i*radius*.7f-radius*.15f,center.y+radius*1.5f),2.dp.toPx())
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("${weather.city}  /  ${weather.condition.label}",style=MaterialTheme.typography.labelMedium,color=Color.White.copy(alpha=.85f))
                Spacer(Modifier.width(9.dp))
                Text(if(weather.isLive) "实时天气" else "离线模拟",fontSize=9.sp,color=StudioLime,modifier=Modifier.clip(PillShape).background(Color.White.copy(alpha=.1f)).padding(horizontal=7.dp,vertical=3.dp))
            }
            Text("${weather.temperatureC}°",fontSize=57.sp,lineHeight=66.sp,fontWeight=FontWeight.Light,color=Color.White,letterSpacing=(-2).sp)
            Text("${weather.lowC}° — ${weather.highC}°   ·   湿度 ${weather.humidity}%",style=MaterialTheme.typography.bodySmall,color=Color.White.copy(alpha=.65f))
            Spacer(Modifier.height(13.dp))
            Text(when {weather.condition.isRainy -> "今日灵感 / 带上雨具，选择耐湿鞋履";weather.highC-weather.lowC>=10 -> "今日灵感 / 早晚温差大，记得叠穿一件外套";weather.temperatureC<15 -> "今日灵感 / 柔软叠穿，让温暖刚刚好";weather.temperatureC>=28 -> "今日灵感 / 轻盈透气，给穿搭一点呼吸感";else -> "今日灵感 / 舒适的温度，穿上喜欢的颜色"},fontSize=11.sp,color=StudioLime,modifier=Modifier.fillMaxWidth(.86f))
            if(showLocationHint) TextButton(onClick=onRequestLocation,contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=48.dp)) {Text("默认城市 · 点击使用我的位置  ↗",fontSize=10.sp,color=Color.White.copy(alpha=.7f))}
        }
    }
}

@Composable
private fun RecommendationCard(candidate:OutfitCandidate,fromStylist:Boolean,headline:String?,occasion:Occasion) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if(fromStylist) "STYLIST'S EDIT" else "YOUR DAILY EDIT",fontSize=9.sp,letterSpacing=2.sp,color=MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(5.dp))
                Text(if(fromStylist) headline ?: "专家的今日提案" else "今日灵感",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.onSurface)
            }
            if(!fromStylist) Column(Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal=12.dp,vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("${candidate.score}",fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
                Text("搭配评分",fontSize=8.sp,color=MaterialTheme.colorScheme.primary)
            }
        }
        Text("为${occasion.label}准备的 ${candidate.items.size} 件单品",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=5.dp,bottom=16.dp))
        val ordered=candidate.items.sortedBy { it.category.ordinal }
        ordered.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    Column(Modifier.weight(1f)) {
                        ItemSwatch(item,Modifier.fillMaxWidth().height(124.dp),shape=RoundedCornerShape(17.dp),showBorder=false)
                        Text(item.name,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=7.dp))
                        Text(item.category.label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if(row.size==1) Spacer(Modifier.weight(1f))
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(vertical=6.dp)) {
            ordered.map {it.colorArgb}.distinct().forEach {ColorDot(it,size=14.dp)}
            Spacer(Modifier.weight(1f));Text("今日配色",fontSize=9.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant,modifier=Modifier.padding(vertical=10.dp))
        Text(candidate.reason,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
