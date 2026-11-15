package com.ootd.today.ui.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.ootd.today.data.model.*
import com.ootd.today.data.repository.WardrobeRepository
import com.ootd.today.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class WardrobeViewModel @Inject constructor(private val wardrobeRepository: WardrobeRepository) : ViewModel() {
    private val filters = MutableStateFlow(WardrobeFilters())
    val uiState: StateFlow<WardrobeUiState> = combine(wardrobeRepository.observeItems(), filters) { items, filter ->
        val now = System.currentTimeMillis()
        val active = items.filter { it.status == ItemStatus.ACTIVE }
        WardrobeUiState(
            isLoading = false,
            isDemo = items.any { it.isDemo },
            allCount = active.size,
            idleCount = active.count { it.isIdle(now) },
            items = browseWardrobe(items, filter, now),
            selectedCategory = filter.category,
            query = filter.query,
            sort = filter.sort,
            idleOnly = filter.idleOnly,
            onlyActive = filter.activeOnly
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), WardrobeUiState())

    fun startPersonalWardrobe(onDone: () -> Unit) { viewModelScope.launch { wardrobeRepository.startPersonalWardrobe(); onDone() } }

    fun selectCategory(category: ClothingCategory?) { filters.update { it.copy(category=category) } }
    fun search(query: String) { filters.update { it.copy(query=query) } }
    fun sortBy(sort: WardrobeSort) { filters.update { it.copy(sort=sort) } }
    fun toggleIdle() { filters.update { it.copy(idleOnly=!it.idleOnly) } }
    fun toggleOnlyActive() { filters.update { it.copy(activeOnly=!it.activeOnly) } }
    fun resetFilters() { filters.value = WardrobeFilters() }
}

data class WardrobeUiState(
    val isLoading: Boolean = true,
    val isDemo: Boolean = false,
    val allCount: Int = 0,
    val idleCount: Int = 0,
    val items: List<WardrobeItem> = emptyList(),
    val selectedCategory: ClothingCategory? = null,
    val onlyActive: Boolean = true,
    val query: String = "",
    val sort: WardrobeSort = WardrobeSort.CATEGORY,
    val idleOnly: Boolean = false
)
