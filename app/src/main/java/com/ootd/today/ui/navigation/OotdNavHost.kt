package com.ootd.today.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ootd.today.ui.add.AddItemScreen
import com.ootd.today.ui.add.AddItemViewModel
import com.ootd.today.ui.calendar.CalendarScreen
import com.ootd.today.ui.calendar.CalendarViewModel
import com.ootd.today.ui.camera.CameraScreen
import com.ootd.today.ui.camera.CameraViewModel
import com.ootd.today.ui.community.CommunityScreen
import com.ootd.today.ui.community.CommunityViewModel
import com.ootd.today.ui.community.PostDetailScreen
import com.ootd.today.ui.community.PostDetailViewModel
import com.ootd.today.ui.community.PublishPostScreen
import com.ootd.today.ui.community.PublishPostViewModel
import com.ootd.today.ui.profile.ProfileScreen
import com.ootd.today.ui.profile.ProfileViewModel
import com.ootd.today.ui.stats.StatsScreen
import com.ootd.today.ui.stats.StatsViewModel
import com.ootd.today.ui.settings.SettingsScreen
import com.ootd.today.ui.settings.SettingsViewModel
import com.ootd.today.ui.settings.VisionSettingsScreen
import com.ootd.today.ui.today.TodayScreen
import com.ootd.today.ui.today.TodayViewModel
import com.ootd.today.ui.expert.ExpertScreen
import com.ootd.today.ui.expert.ExpertViewModel
import com.ootd.today.ui.expert.ExpertIdentityViewModel
import com.ootd.today.ui.expert.FloatingExpert
import com.ootd.today.ui.wardrobe.ItemDetailScreen
import com.ootd.today.ui.wardrobe.WardrobeScreen
import com.ootd.today.ui.wardrobe.WardrobeViewModel

/**
 * 应用主导航容器。
 *
 * 结构：`Scaffold` 承载内容区与悬浮底部栏。底部栏对「录入页」和「详情页」隐藏，
 * 因为这两个页面是全屏沉浸式流程，显示底部栏会挤占表单空间。
 */
@Composable
fun OotdNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val identity: ExpertIdentityViewModel = hiltViewModel()
    val expertProfile by identity.profile.collectAsStateWithLifecycle()

    // Notification permission is requested when the reminder is enabled in Settings.
    // Opening the app or changing the system theme should never repeat the prompt.

    // 录入页、详情页、统计页是全屏流程，隐藏底部栏。
    val showBottomBar = currentRoute in setOf(
        Routes.TODAY,
        Routes.WARDROBE,
        Routes.CALENDAR,
        Routes.PROFILE
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                OotdBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { dest -> navController.navigateToTab(dest) },
                    onAddClick = { navController.navigate(Routes.ADD_ITEM) }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showBottomBar) PaddingValues() else innerPadding)
        ) {
            composable(Routes.TODAY) { entry ->
                val viewModel: TodayViewModel = hiltViewModel()
                val pendingItem by entry.savedStateHandle.getStateFlow<Long?>(KEY_FOCUS_ITEM, null).collectAsStateWithLifecycle()
                LaunchedEffect(pendingItem) {
                    pendingItem?.let { id ->
                        viewModel.focusOnItem(id)
                        entry.savedStateHandle.set<Long?>(KEY_FOCUS_ITEM, null)
                    }
                }
                TodayScreen(
                    viewModel = viewModel,
                    onAddItem = { navController.navigate(Routes.ADD_ITEM) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenExpert = { navController.navigate(Routes.EXPERT) },
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                )
            }

            composable(Routes.WARDROBE) {
                val viewModel: WardrobeViewModel = hiltViewModel()
                WardrobeScreen(
                    viewModel = viewModel,
                    onAddItem = { navController.navigate(Routes.ADD_ITEM) },
                    onItemClick = { itemId -> navController.navigate(Routes.itemDetail(itemId)) },
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                )
            }

            composable(Routes.ADD_ITEM) { entry ->
                val viewModel: AddItemViewModel = hiltViewModel()
                // 从相机页拍完回来的结果通过 SavedStateHandle 传递，
                // 避免把大对象塞进路由参数。
                val captured = entry.savedStateHandle.get<String>(Routes.KEY_CAPTURED_PHOTO)
                LaunchedEffect(captured) {
                    if (captured != null) {
                        viewModel.onCameraImagePicked(captured)
                        entry.savedStateHandle.remove<String>(Routes.KEY_CAPTURED_PHOTO)
                    }
                }
                AddItemScreen(
                    viewModel = viewModel,
                    onOpenVisionSettings = { navController.navigate(Routes.VISION_SETTINGS) },
                    onBuildOutfit = { id ->
                        navController.getBackStackEntry(Routes.TODAY).savedStateHandle[KEY_FOCUS_ITEM] = id
                        navController.navigateToTab(BottomDestination.TODAY)
                    },
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                    onOpenCamera = { navController.navigate(Routes.CAMERA) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            composable(Routes.CALENDAR) {
                val viewModel: CalendarViewModel = hiltViewModel()
                CalendarScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                )
            }

            composable(Routes.PROFILE) {
                val viewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenCommunity = { navController.navigate(Routes.COMMUNITY) },
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                )
            }

            composable(Routes.STATS) {
                val viewModel: StatsViewModel = hiltViewModel()
                StatsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onBuildOutfit = { id ->
                        navController.getBackStackEntry(Routes.TODAY).savedStateHandle[KEY_FOCUS_ITEM] = id
                        navController.navigateToTab(BottomDestination.TODAY)
                    },
                    onEditItem = { id -> navController.navigate(Routes.editItem(id)) },
                    onAddItem = { navController.navigate(Routes.ADD_ITEM) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            composable(
                route = Routes.ITEM_DETAIL,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { entry ->
                val itemId = entry.arguments?.getLong("itemId") ?: return@composable
                ItemDetailScreen(
                    itemId = itemId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(if (itemId < 0) Routes.ADD_ITEM else Routes.editItem(itemId)) },
                    onBuildOutfit = {
                        navController.getBackStackEntry(Routes.TODAY).savedStateHandle[KEY_FOCUS_ITEM] = itemId
                        navController.navigateToTab(BottomDestination.TODAY)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            composable(
                route = Routes.EDIT_ITEM,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { entry ->
                val itemId = entry.arguments?.getLong("itemId") ?: return@composable
                AddItemScreen(
                    viewModel = hiltViewModel(),
                    onOpenVisionSettings = { navController.navigate(Routes.VISION_SETTINGS) },
                    onBuildOutfit = { id ->
                        navController.getBackStackEntry(Routes.TODAY).savedStateHandle[KEY_FOCUS_ITEM] = id
                        navController.navigateToTab(BottomDestination.TODAY)
                    },
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                    // 编辑页同样支持重拍，因此也要传递相机入口。
                    onOpenCamera = { navController.navigate(Routes.CAMERA) },
                    editItemId = itemId,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            composable(Routes.SETTINGS) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenExpert = { navController.navigate(Routes.EXPERT) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            composable(Routes.VISION_SETTINGS) {
                val viewModel: SettingsViewModel = hiltViewModel()
                VisionSettingsScreen(viewModel, onBack = { navController.popBackStack() }, modifier = Modifier.padding(innerPadding))
            }
            composable(Routes.EXPERT) { entry ->
                val viewModel: ExpertViewModel = hiltViewModel()
                val todayEntry = remember(entry) { navController.getBackStackEntry(Routes.TODAY) }
                val today: TodayViewModel = hiltViewModel(todayEntry)
                val todayState by today.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(todayState.weather, todayState.occasion, todayState.requiredItem) {
                    viewModel.setContext(todayState.weather, todayState.occasion, todayState.requiredItem?.id)
                }
                ExpertScreen(viewModel, onBack = { navController.popBackStack() })
            }

            // --- 社区 ---
            composable(Routes.COMMUNITY) { entry ->
                val viewModel: CommunityViewModel = hiltViewModel()

                // 从动态详情页点话题返回时，读取并应用待筛选话题。
                // 用 LaunchedEffect 而非直接在组合中读取：读取后要立即清除，
                // 否则旋转屏幕会重复触发筛选。
                val pendingTopic = entry.savedStateHandle.get<String>(KEY_PENDING_TOPIC)
                LaunchedEffect(pendingTopic) {
                    if (pendingTopic != null) {
                        // 用 applyTopicFilter 而非 selectTopic：
                        // 用户是「点话题来看这个话题」，意图明确，不应因已选中而被取消。
                        viewModel.applyTopicFilter(pendingTopic)
                        entry.savedStateHandle.remove<String>(KEY_PENDING_TOPIC)
                    }
                }

                CommunityScreen(
                    viewModel = viewModel,
                    onPostClick = { postId -> navController.navigate(Routes.postDetail(postId)) },
                    onPublishClick = { navController.navigate(Routes.PUBLISH_POST) },
                    onBack = { navController.popBackStack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            composable(Routes.PUBLISH_POST) { entry ->
                val viewModel: PublishPostViewModel = hiltViewModel()
                val captured = entry.savedStateHandle.get<String>(Routes.KEY_CAPTURED_PHOTO)
                LaunchedEffect(captured) {
                    if (captured != null) {
                        viewModel.onCameraPhotoPicked(captured)
                        entry.savedStateHandle.remove<String>(Routes.KEY_CAPTURED_PHOTO)
                    }
                }
                PublishPostScreen(
                    viewModel = viewModel,
                    onPublished = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onOpenCamera = { navController.navigate(Routes.CAMERA) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            // --- 应用内相机 ---
            composable(Routes.CAMERA) {
                val cameraViewModel: CameraViewModel = hiltViewModel()
                CameraScreen(
                    viewModel = cameraViewModel,
                    onCaptured = { path ->
                        // 把结果写回**上一个**页面的 SavedStateHandle，
                        // 然后退栈；调用方在重组时读取并消费这个值。
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(Routes.KEY_CAPTURED_PHOTO, path)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(
                route = Routes.POST_DETAIL,
                arguments = listOf(
                    navArgument(PostDetailViewModel.ARG_POST_ID) { type = NavType.LongType }
                )
            ) {
                val viewModel: PostDetailViewModel = hiltViewModel()
                PostDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onTopicClick = { tag ->
                        // 点话题回到动态流；话题筛选状态由 feed 页自己持有，
                        // 这里先退回到 feed，再由下面的 key 机制触发选中。
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(KEY_PENDING_TOPIC, tag)
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        if (currentRoute != null && currentRoute !in setOf(Routes.EXPERT, Routes.CAMERA, Routes.ADD_ITEM, Routes.EDIT_ITEM, Routes.PUBLISH_POST, Routes.VISION_SETTINGS)) {
            FloatingExpert(
                name = expertProfile.expertName,
                onOpen = { navController.navigate(Routes.EXPERT) { launchSingleTop = true } },
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
            )
        }
        }
    }
}

/**
 * Tab 导航：单实例栈顶复用 + 保存状态。
 *
 * 这样在 Tab 间来回切换时不会重复创建页面，也能保留各 Tab 的滚动位置。
 */
private fun NavHostController.navigateToTab(destination: BottomDestination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** 从详情页返回动态流时，用于传递「要筛选的话题」。 */
const val KEY_PENDING_TOPIC = "pending_topic"

private const val KEY_FOCUS_ITEM = "focus_wardrobe_item"
