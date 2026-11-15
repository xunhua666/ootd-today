package com.ootd.today.ui.expert

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ootd.today.data.prefs.SettingsRepository
import com.ootd.today.data.prefs.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ExpertIdentityViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val profile = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())
}

/** Only the character's small bounds consume touch; the rest of the screen stays interactive. */
@Composable
fun FloatingExpert(name: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("floating_expert", Context.MODE_PRIVATE) }
    var fraction by remember { mutableStateOf(Offset(prefs.getFloat("x", 1f), prefs.getFloat("y", 0.82f))) }
    var dragging by remember { mutableStateOf(false) }
    val latestOpen by rememberUpdatedState(onOpen)
    val infinite = rememberInfiniteTransition(label = "expert-breath")
    val bob by infinite.animateFloat(0f, -3f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "expert-bob")
    fun persist() { prefs.edit().putFloat("x", fraction.x).putFloat("y", fraction.y).apply() }
    BoxWithConstraints(modifier.fillMaxSize().padding(8.dp)) {
        val density = LocalDensity.current
        val rangeX = with(density) { (maxWidth - 86.dp).toPx().coerceAtLeast(0f) }
        val rangeY = with(density) { (maxHeight - 132.dp).toPx().coerceAtLeast(0f) }
        Column(
            Modifier.offset { IntOffset((fraction.x.coerceIn(0f, 1f) * rangeX).roundToInt(), (fraction.y.coerceIn(0f, 1f) * rangeY).roundToInt()) }
                .size(86.dp, 132.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "悬浮审美专家"
                    stateDescription = "拖动可移动，点按进入对话"
                    customActions = listOf(CustomAccessibilityAction("移回右下方") { fraction = Offset(1f, 0.82f); persist(); true })
                }
                .pointerInput(rangeX, rangeY) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false; persist() },
                        onDragCancel = { dragging = false; persist() },
                        onDrag = { change, delta ->
                            change.consume()
                            fraction = Offset(
                                if (rangeX > 0) (fraction.x + delta.x / rangeX).coerceIn(0f, 1f) else 0f,
                                if (rangeY > 0) (fraction.y + delta.y / rangeY).coerceIn(0f, 1f) else 0f
                            )
                        }
                    )
                }
                .clickable(role = Role.Button, onClickLabel = "和审美专家对话") { latestOpen() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExpertMascot(Modifier.weight(1f).fillMaxWidth().graphicsLayer { translationY = if (dragging) 0f else bob.dp.toPx() })
            Surface(shape = RoundedCornerShape(30.dp), shadowElevation = 3.dp, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(name.ifBlank { "聊聊穿搭" }, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
            }
        }
    }
}
