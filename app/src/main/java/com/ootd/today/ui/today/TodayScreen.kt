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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ootd.today.R
import com.ootd.today.data.model.*
import com.ootd.today.domain.OutfitCandidate
import com.ootd.today.ui.common.*
import com.ootd.today.ui.theme.PillShape
import com.ootd.today.ui.expert.ExpertEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun TodayScreen(viewModel: TodayViewModel, modifier: Modifier = Modifier, onAddItem: () -> Unit = {}, onOpenSettings: () -> Unit = {}, onOpenExpert: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val expertProfile by viewModel.expertProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
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
                    val locale = LocalLocale.current.platformLocale
                    val dateLabel = remember(locale) {
                        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMddEEE")
                        SimpleDateFormat(pattern, locale).format(Date())
                    }
                    Text(dateLabel,fontSize=11.sp,letterSpacing=1.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment=Alignment.Bottom) {
                        Text("OOTD",fontSize=40.sp,lineHeight=46.sp,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,letterSpacing=(-2).sp,color=MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.today_brand_sub),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(start=10.dp,bottom=7.dp))
                    }
                    Text(stringResource(R.string.today_tagline),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(stringResource(R.string.today_focus_title),style=MaterialTheme.typography.labelSmall)
                            Text(focused.name,style=MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                        IconButton(onClick=viewModel::clearFocusItem) { StudioIcon(StudioSymbol.CLOSE,description=stringResource(R.string.today_focus_clear)) }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            if(state.isLoading) {
                Box(Modifier.fillMaxWidth().height(260.dp),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
            } else {
                state.weather?.let { WeatherCard(it,!state.usingDeviceLocation,requestLocation) } ?: Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(22.dp)) {
                        Text(stringResource(R.string.today_weather_unavailable),style=MaterialTheme.typography.titleMedium)
                        TextButton(onClick=viewModel::refresh) { Text(stringResource(R.string.today_retry)) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top=24.dp,bottom=9.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text(stringResource(R.string.today_occasion_prompt),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onBackground)
                    Text("DRESS FOR YOUR DAY",fontSize=8.sp,letterSpacing=1.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(Occasion.entries.size) { i ->
                        val occasion=Occasion.entries[i]
                        FilterChip(selected=state.occasion==occasion,onClick={viewModel.selectOccasion(occasion)},label={Text(stringResource(occasion.labelRes))},shape=PillShape,
                            colors=FilterChipDefaults.filterChipColors(selectedContainerColor=MaterialTheme.colorScheme.primary,selectedLabelColor=MaterialTheme.colorScheme.onPrimary))
                    }
                }
                Spacer(Modifier.height(18.dp))
                val candidate=state.recommendation
                if(candidate==null) {
                    Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface) {
                        Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            ClothingArt(ClothingCategory.TOP,MaterialTheme.colorScheme.primary,Modifier.size(110.dp))
                            Text(stringResource(R.string.today_empty_title),style=MaterialTheme.typography.titleLarge)
                            Text(
                                if(state.requiredItem != null) stringResource(R.string.today_empty_focused)
                                else if(state.wardrobeTooSmall) stringResource(R.string.today_empty_small_wardrobe)
                                else stringResource(R.string.today_empty_weather),
                                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=14.dp))
                            Button(onClick=onAddItem,shape=PillShape) {Text(stringResource(R.string.today_add_first_item));Spacer(Modifier.width(8.dp));StudioIcon(StudioSymbol.ADD,color=MaterialTheme.colorScheme.onPrimary)}
                        }
                    }
                } else {
                    RecommendationCard(candidate,state.fromStylist,state.stylistHeadline,state.occasion)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick={if(state.isDemo) viewModel.startPersonalWardrobe(onAddItem) else viewModel.adoptCurrent { ok -> message(resources.getString(if(ok) R.string.today_snack_adopted else R.string.today_snack_failed)) }},enabled=!state.isSaving,modifier=Modifier.fillMaxWidth().height(54.dp),shape=PillShape) {
                        StudioIcon(StudioSymbol.CHECK,color=MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(10.dp));Text(if(state.isSaving) stringResource(R.string.today_saving) else if(state.isDemo) stringResource(R.string.today_adopt_demo) else stringResource(R.string.today_adopt))
                    }
                    Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick=viewModel::shuffle,modifier=Modifier.weight(1f).height(46.dp),contentPadding=PaddingValues(8.dp)) { StudioIcon(StudioSymbol.REFRESH,Modifier.size(17.dp),color=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(6.dp));Text(stringResource(R.string.today_shuffle),fontSize=12.sp) }
                        OutlinedButton(onClick={viewModel.rateCurrent(true);message(resources.getString(R.string.today_snack_liked))},modifier=Modifier.weight(1f).height(46.dp),contentPadding=PaddingValues(8.dp)) { StudioIcon(StudioSymbol.HEART,Modifier.size(17.dp),color=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(6.dp));Text(stringResource(R.string.today_like),fontSize=12.sp) }
                        OutlinedButton(onClick={viewModel.rateCurrent(false);message(resources.getString(R.string.today_snack_disliked))},modifier=Modifier.weight(1f).height(46.dp),contentPadding=PaddingValues(8.dp)) { Text(stringResource(R.string.today_dislike),fontSize=12.sp) }
                    }
                    TextButton(onClick={
                        val text = buildString {
                            append(resources.getString(R.string.today_share_header, resources.getString(state.occasion.labelRes)))
                            append('\n')
                            candidate.items.forEach { append("${resources.getString(it.category.labelRes)}：${it.name}\n") }
                            append('\n')
                            append(candidate.reason)
                            append('\n')
                            append(resources.getString(R.string.today_share_footer))
                        }
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain";putExtra(Intent.EXTRA_TEXT,text) },resources.getString(R.string.today_share_chooser)))
                    },modifier=Modifier.align(Alignment.CenterHorizontally)) { StudioIcon(StudioSymbol.SHARE,Modifier.size(16.dp),color=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(6.dp));Text(stringResource(R.string.today_share),fontSize=12.sp) }
                    Spacer(Modifier.height(8.dp))
                    if(state.requiredItem != null) {
                        Text(stringResource(R.string.today_focus_algo_note),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if(state.stylistReady) {
                        StylistBar(state.stylistReady,state.isConsulting,state.fromStylist,viewModel::askStylist,viewModel::askStylistFollowUp,name=expertProfile.expertName)
                    } else {
                        Surface(onClick=onOpenExpert,shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
                            Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                                StudioIcon(StudioSymbol.SPARK,color=MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                                    Text(stringResource(R.string.today_stylist_title),style=MaterialTheme.typography.titleSmall)
                                    Text(stringResource(R.string.today_stylist_subtitle),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("${weather.city}  /  ${stringResource(weather.condition.labelRes)}",style=MaterialTheme.typography.labelMedium,color=Color.White.copy(alpha=.85f))
                Spacer(Modifier.width(9.dp))
                Text(if(weather.isLive) stringResource(R.string.today_weather_live) else stringResource(R.string.today_weather_offline),fontSize=9.sp,color=StudioLime,modifier=Modifier.clip(PillShape).background(Color.White.copy(alpha=.1f)).padding(horizontal=7.dp,vertical=3.dp))
            }
            Text("${weather.temperatureC}°",fontSize=57.sp,lineHeight=66.sp,fontWeight=FontWeight.Light,color=Color.White,letterSpacing=(-2).sp)
            Text(stringResource(R.string.today_weather_range, weather.lowC, weather.highC, weather.humidity),style=MaterialTheme.typography.bodySmall,color=Color.White.copy(alpha=.65f))
            Spacer(Modifier.height(13.dp))
            Text(
                when {
                    weather.condition.isRainy -> stringResource(R.string.today_weather_hint_rain)
                    weather.highC-weather.lowC>=10 -> stringResource(R.string.today_weather_hint_gap)
                    weather.temperatureC<15 -> stringResource(R.string.today_weather_hint_cold)
                    weather.temperatureC>=28 -> stringResource(R.string.today_weather_hint_hot)
                    else -> stringResource(R.string.today_weather_hint_mild)
                },
                fontSize=11.sp,color=StudioLime,modifier=Modifier.fillMaxWidth(.86f)
            )
            if(showLocationHint) TextButton(onClick=onRequestLocation,contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=48.dp)) {Text(stringResource(R.string.today_weather_location_hint),fontSize=10.sp,color=Color.White.copy(alpha=.7f))}
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
                Text(
                    if(fromStylist) headline ?: stringResource(R.string.today_recommendation_stylist_title) else stringResource(R.string.today_recommendation_local_title),
                    style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.onSurface
                )
            }
            if(!fromStylist) Column(Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal=12.dp,vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("${candidate.score}",fontSize=23.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.today_score_label),fontSize=8.sp,color=MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            stringResource(R.string.today_items_for_occasion, candidate.items.size, stringResource(occasion.labelRes)),
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=5.dp,bottom=16.dp)
        )
        val ordered=candidate.items.sortedBy { it.category.ordinal }
        ordered.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    Column(Modifier.weight(1f)) {
                        ItemSwatch(item,Modifier.fillMaxWidth().height(124.dp),shape=RoundedCornerShape(17.dp),showBorder=false)
                        Text(item.name,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=7.dp))
                        Text(stringResource(item.category.labelRes),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if(row.size==1) Spacer(Modifier.weight(1f))
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(vertical=6.dp)) {
            ordered.map {it.colorArgb}.distinct().forEach {ColorDot(it,size=14.dp)}
            Spacer(Modifier.weight(1f));Text(stringResource(R.string.today_palette),fontSize=9.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant,modifier=Modifier.padding(vertical=10.dp))
        Text(candidate.reason,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}