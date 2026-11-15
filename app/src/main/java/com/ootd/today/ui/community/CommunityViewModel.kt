package com.ootd.today.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ootd.today.data.model.CommunityUser
import com.ootd.today.data.model.Post
import com.ootd.today.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 动态流筛选维度。 */
enum class FeedFilter(val label: String) {
    ALL("全部"),
    FOLLOWING("关注"),
    MINE("我的")
}

/**
 * 社区动态流 ViewModel。
 *
 * 筛选在内存里做（数据量小），避免为三种视图各写一套 DAO 查询。
 * 接真后端后应改为服务端分页查询，届时只需替换本类内部的实现。
 */
@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(FeedFilter.ALL)

    /** 当前选中的话题（null 表示不按话题筛选）。 */
    private val topic = MutableStateFlow<String?>(null)

    /**
     * flatMapLatest 在此是必要的：切换话题时需要**取消**上一个话题的订阅，
     * 否则旧话题的数据流会继续发射，导致列表短暂回闪旧内容。
     * 它目前标记为实验性 API，这里显式 opt-in。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CommunityUiState> = combine(
        topic.flatMapLatest { activeTopic ->
            if (activeTopic == null) {
                communityRepository.observeFeed()
            } else {
                communityRepository.observePostsByTopic(activeTopic)
            }
        },
        filter,
        topic,
        communityRepository.observeUsers(),
        communityRepository.observeTrendingTopics()
    ) { posts, currentFilter, activeTopic, users, trending ->
        CommunityUiState(
            isLoading = false,
            filter = currentFilter,
            activeTopic = activeTopic,
            posts = posts.filterBy(currentFilter),
            users = users.filterNot { it.isMe },
            trendingTopics = trending
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = CommunityUiState()
    )

    fun selectFilter(next: FeedFilter) {
        filter.value = next
    }

    /**
     * 点击热门话题 Chip 切换筛选：再次点选同一话题则取消。
     *
     * 与 [applyTopicFilter] 语义不同：这个方法服务于「开关」式交互，
     * 而 [applyTopicFilter] 服务于「跳转过来就是要看这个话题」的场景。
     */
    fun selectTopic(next: String?) {
        val isSame = next != null && topic.value.equals(next, ignoreCase = true)
        topic.value = if (isSame) null else next
    }

    /**
     * 无条件应用话题筛选，不做开关切换。
     *
     * 专用于「从动态详情页点话题跳回来」的场景：用户意图明确是「看这个话题」，
     * 因此即使该话题已在筛选中，也应保持筛选而不是被取消。
     */
    fun applyTopicFilter(next: String) {
        topic.value = next
    }

    fun toggleLike(postId: Long) {
        viewModelScope.launch { communityRepository.toggleLike(postId) }
    }

    fun toggleFollow(userId: Long, following: Boolean) {
        viewModelScope.launch { communityRepository.setFollowing(userId, following) }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch { communityRepository.deletePost(postId) }
    }

    private fun List<Post>.filterBy(filter: FeedFilter): List<Post> = when (filter) {
        FeedFilter.ALL -> this
        FeedFilter.FOLLOWING -> filter { it.author.isFollowing || it.author.isMe }
        FeedFilter.MINE -> filter { it.isMine }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** 社区动态流 UI 状态。 */
data class CommunityUiState(
    val isLoading: Boolean = true,
    val filter: FeedFilter = FeedFilter.ALL,
    /** 当前话题筛选，null 表示未筛选。 */
    val activeTopic: String? = null,
    val posts: List<Post> = emptyList(),
    /** 可关注的其它用户，用于「发现同好」横向列表。 */
    val users: List<CommunityUser> = emptyList(),
    /** 热门话题（标签 -> 动态数），按热度降序。 */
    val trendingTopics: List<Pair<String, Int>> = emptyList()
)