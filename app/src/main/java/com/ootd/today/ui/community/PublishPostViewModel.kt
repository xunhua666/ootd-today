package com.ootd.today.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.data.local.ImageStore
import com.ootd.today.data.repository.CommunityRepository
import com.ootd.today.data.repository.WardrobeRepository
import com.ootd.today.domain.SocialTextParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 发布动态页 ViewModel。
 *
 * 选品逻辑：按大类分组展示衣橱单品，用户逐个点选。
 * 日常搭配的组成部分是固定的，因此按 [ClothingCategory] 分组比
 * 平铺一个长列表更好用。
 */
@HiltViewModel
class PublishPostViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val wardrobeRepository: WardrobeRepository,
    private val imageStore: ImageStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublishPostUiState())
    val uiState: StateFlow<PublishPostUiState> = _uiState.asStateFlow()

    init {
        loadWardrobe()
    }

    private fun loadWardrobe() {
        viewModelScope.launch {
            val items = wardrobeRepository.observeItems().first()
                .filter { it.status == ItemStatus.ACTIVE }
            _uiState.update { it.copy(isLoading = false, wardrobe = items) }
        }
    }

    /** 写入从相册选中的配图（拷贝进私有目录，避免临时授权失效）。 */
    fun onGalleryPhotoPicked(uri: android.net.Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { imageStore.copyToPrivate(uri) }
            _uiState.update { it.copy(photoPath = path) }
        }
    }

    /** 拍照完成后记录路径。 */
    fun onCameraPhotoPicked(path: String) = _uiState.update { it.copy(photoPath = path) }

    fun removePhoto() = _uiState.update { it.copy(photoPath = null) }

    /** 点选/取消点选一件单品。 */
    fun toggleItem(item: WardrobeItem) {
        _uiState.update { state ->
            val selected = state.selected
            val next = if (selected.any { it.id == item.id }) {
                selected.filterNot { it.id == item.id }
            } else {
                selected + item
            }
            state.copy(selected = next, error = null)
        }
    }

    fun onCaptionChange(value: String) = _uiState.update { it.copy(caption = value, error = null) }

    /**
     * 一键追加一个话题到文案末尾。
     *
     * 已存在（忽略大小写）时不重复追加，避免 #ootd 与 #OOTD 并存。
     */
    fun appendTopic(tag: String) {
        _uiState.update { state ->
            val existing = SocialTextParser.extractTopics(state.caption)
                .map { it.lowercase() }
            if (tag.lowercase() in existing) return@update state

            val separator = if (state.caption.isBlank() || state.caption.endsWith(" ")) "" else " "
            state.copy(caption = state.caption + separator + "#$tag ")
        }
    }

    fun onOccasionChange(occasion: Occasion) = _uiState.update { it.copy(occasion = occasion) }

    /**
     * 发布。
     *
     * 校验：至少选一件单品或上传一张配图 —— 两者都没有的动态没有任何内容。
     * 文案允许为空（很多人只想晒图）。
     */
    fun publish(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.selected.isEmpty() && state.photoPath == null) {
            _uiState.update { it.copy(error = "至少选一件单品或上传一张配图") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true) }
            communityRepository.createPost(
                items = state.selected,
                caption = state.caption,
                occasion = state.occasion,
                photoPath = state.photoPath
            )
            _uiState.update { it.copy(isPublishing = false) }
            onDone()
        }
    }
}

/** 发布动态页 UI 状态。 */
data class PublishPostUiState(
    val isLoading: Boolean = true,
    /** 我的衣橱里可选的单品（已滤掉「已处理」）。 */
    val wardrobe: List<WardrobeItem> = emptyList(),
    val selected: List<WardrobeItem> = emptyList(),
    val caption: String = "",
    val occasion: Occasion = Occasion.COMMUTE,
    /** 配图的本地路径，null 表示纯穿搭动态。 */
    val photoPath: String? = null,
    val isPublishing: Boolean = false,
    val error: String? = null
) {
    /** 选中的单品按大类归组，用于发布前的预览。 */
    val selectedByCategory: Map<ClothingCategory, List<WardrobeItem>>
        get() = selected.groupBy { it.category }

    /** 文案里已使用的话题（不含 #），用于在建议列表里标记「已添加」。 */
    val usedTopics: List<String>
        get() = SocialTextParser.extractTopics(caption)
}