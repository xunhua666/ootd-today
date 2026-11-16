package com.ootd.today.ui.navigation

import androidx.annotation.StringRes
import com.ootd.today.R

/**
 * 全部路由。
 *
 * 用字符串常量而非 sealed class，是因为 Navigation Compose 的
 * `composable(route = ...)` 需要编译期常量；带参数的路由也在这里集中定义，
 * 避免路由字符串散落在各个页面里拼错。
 */
object Routes {
    const val TODAY = "today"
    const val WARDROBE = "wardrobe"
    const val ADD_ITEM = "add_item"
    const val CALENDAR = "calendar"
    const val PROFILE = "profile"
    const val STATS = "stats"

    /** 单品详情页，参数为单品 id。 */
    const val ITEM_DETAIL = "item_detail/{itemId}"
    fun itemDetail(itemId: Long) = "item_detail/$itemId"

    /** 编辑单品页，参数为单品 id。与新建复用同一表单。 */
    const val EDIT_ITEM = "edit_item/{itemId}"
    fun editItem(itemId: Long) = "edit_item/$itemId"

    const val SETTINGS = "settings"
    const val EXPERT = "expert"
    const val VISION_SETTINGS = "vision_settings"

    // --- 社区 ---
    const val COMMUNITY = "community"
    const val PUBLISH_POST = "publish_post"

    /** 动态详情页，参数为动态 id。 */
    const val POST_DETAIL = "post_detail/{postId}"
    fun postDetail(postId: Long) = "post_detail/$postId"

    /**
     * 应用内相机页。
     *
     * 刻意不带「调用来源」参数：拍照结果通过写回**上一个页面的**
     * SavedStateHandle 回传，因此相机页无需知道自己被谁调用。
     */
    const val CAMERA = "camera"

    /** 拍照结果回传的 SavedStateHandle key。 */
    const val KEY_CAPTURED_PHOTO = "captured_photo"
}

/**
 * 底部导航的 5 个入口。
 *
 * 中间的「录入」是凸起的圆形大按钮而非普通 Tab，用 [isCenterAction] 标记：
 * 点击它不切换 Tab，而是以全屏页方式进入录入流程，返回后停留在原来的 Tab。
 *
 * 图标统一使用 emoji 而不是 Material 矢量图标，原因有二：
 * 1. 衣橱品类本身已用 emoji 表达（👕👖👗…），导航栏沿用同一套视觉语言更统一；
 * 2. 省掉 `material-icons-extended` 这个体积很大的依赖。
 */
enum class BottomDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val emoji: String,
    val isCenterAction: Boolean = false
) {
    TODAY(Routes.TODAY, R.string.nav_today, "☀️"),
    WARDROBE(Routes.WARDROBE, R.string.nav_wardrobe, "👕"),
    ADD(Routes.ADD_ITEM, R.string.nav_add, "＋", isCenterAction = true),
    CALENDAR(Routes.CALENDAR, R.string.nav_calendar, "📅"),
    PROFILE(Routes.PROFILE, R.string.nav_profile, "🙋")
}
