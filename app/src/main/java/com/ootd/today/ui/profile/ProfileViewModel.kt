package com.ootd.today.ui.profile

import androidx.lifecycle.ViewModel
import com.ootd.today.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/** 「我的」页概览数据。 */
data class ProfileUiState(
    val itemCount: Int = 0,
    val logCount: Int = 0
)

/**
 * 「我的」页 ViewModel。
 *
 * 只提供概览计数。统计报表（闲置率/性价比）属于 P1，届时在此扩展。
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    wardrobeRepository: WardrobeRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        wardrobeRepository.observeItems(),
        wardrobeRepository.observeLogs()
    ) { items, logs ->
        ProfileUiState(itemCount = items.count { !it.isDemo }, logCount = logs.size)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ProfileUiState()
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
