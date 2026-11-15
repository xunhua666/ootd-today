package com.ootd.today.ui.community

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ootd.today.data.model.Comment
import com.ootd.today.data.model.Post
import com.ootd.today.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 动态详情页 ViewModel。
 *
 * postId 从导航参数直接读（SavedStateHandle），
 * 因此详情页不需要在 UI 层手动传参再回灌给 ViewModel。
 */
@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: Long = savedStateHandle.get<Long>(ARG_POST_ID) ?: 0L

    /** 输入框内容。 */
    private val draft = MutableStateFlow("")

    val uiState: StateFlow<PostDetailUiState> = combine(
        communityRepository.observePost(postId),
        communityRepository.observeComments(postId),
        draft
    ) { post, comments, draftText ->
        PostDetailUiState(
            isLoading = false,
            post = post,
            comments = comments,
            draft = draftText,
            canSend = draftText.isNotBlank()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = PostDetailUiState()
    )

    fun onDraftChange(value: String) {
        draft.value = value
    }

    fun toggleLike() {
        viewModelScope.launch { communityRepository.toggleLike(postId) }
    }

    fun sendComment() {
        val content = draft.value
        if (content.isBlank()) return
        viewModelScope.launch {
            communityRepository.addComment(postId, content)
            // 发送后清空输入框，符合评论场景的预期。
            draft.value = ""
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch { communityRepository.deleteComment(commentId) }
    }

    companion object {
        /** 导航参数名，与 Routes.postDetail 保持一致。 */
        const val ARG_POST_ID = "postId"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** 动态详情页 UI 状态。 */
data class PostDetailUiState(
    val isLoading: Boolean = true,
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val draft: String = "",
    val canSend: Boolean = false
)