package com.ootd.today.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ootd.today.ui.common.*

@Composable
fun VisionSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    val saving by viewModel.visionSaving.collectAsStateWithLifecycle()
    val message by viewModel.visionMessage.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
        IconButton(onClick = onBack) { StudioIcon(StudioSymbol.ARROW, Modifier.rotate(180f), description = "返回单品") }
        VisionSettingsSection(settings.vision, saving, message, viewModel::saveVisionSettings, viewModel::clearVisionKey)
        Spacer(Modifier.height(28.dp))
    }
}
