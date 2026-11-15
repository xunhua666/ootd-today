package com.ootd.today.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Brand palette — wardrobe studio / 奶油白与森林绿
// 主色：电光紫 | 撞色：荧光青 + 玫粉 + 柠檬黄
// ---------------------------------------------------------------------------

/** 品牌主色：电光紫。承载所有主操作与品牌识别。 */
val ElectricViolet = Color(0xFF344E41)
/** 主色深变体，用于深色模式下的主色。 */
val ElectricVioletDark = Color(0xFFB6CF9E)
/** 主色的按压态/容器色（浅）。 */
val VioletContainer = Color(0xFFE3EBD9)
/** 主色容器上的前景色（深）。 */
val OnVioletContainer = Color(0xFF243B2E)

/** 撞色 1：荧光青。用于次级强调、进度、成功态。 */
val NeonCyan = Color(0xFF648369)
val NeonCyanDark = Color(0xFFBDD9B0)
val CyanContainer = Color(0xFFE5EEDB)
val OnCyanContainer = Color(0xFF233C2B)

/** 撞色 2：玫粉。用于喜爱、打卡、情绪化强调。 */
val HotPink = Color(0xFFB47761)
val HotPinkDark = Color(0xFFE1B39E)
val PinkContainer = Color(0xFFF4E4DA)
val OnPinkContainer = Color(0xFF613C30)

/** 撞色 3：柠檬黄。用于高亮标记、"今日"角标、能量感。 */
val LemonYellow = Color(0xFFD9EC9C)
val LemonYellowDark = Color(0xFFD9EC9C)
val YellowContainer = Color(0xFFEDF3D6)
val OnYellowContainer = Color(0xFF394521)

/** 撞色 4：珊瑚橙。用于预警、天气高温、热度。 */
val CoralOrange = Color(0xFFB97B50)
val CoralOrangeDark = Color(0xFFD9A374)

// ---------------------------------------------------------------------------
// Neutral palette — 温暖纸张与自然墨色
// ---------------------------------------------------------------------------

/** 浅色主题背景：暖调象牙白。 */
val Ivory = Color(0xFFF7F7F0)
/** 浅色主题表面：纯白卡片。 */
val SurfaceLight = Color(0xFFFFFFFF)
/** 浅色主题次级表面：淡紫灰。 */
val SurfaceContainerLight = Color(0xFFEFEFE5)
/** 浅色主题三级表面：略深的紫灰，用于输入框底色。 */
val SurfaceVariantLight = Color(0xFFE9EBE0)
/** 浅色主题描边。 */
val OutlineLight = Color(0xFF7D8476)
/** 浅色主题弱描边，用于分隔线。 */
val OutlineVariantLight = Color(0xFFDCE0D3)

/** 深色主题背景：深紫黑，比纯黑更柔和、更有质感。 */
val InkBlack = Color(0xFF141C18)
/** 深色主题表面。 */
val SurfaceDark = Color(0xFF1D2921)
/** 深色主题次级表面。 */
val SurfaceContainerDark = Color(0xFF27352B)
/** 深色主题三级表面。 */
val SurfaceVariantDark = Color(0xFF344338)
/** 深色主题描边。 */
val OutlineDark = Color(0xFF929F90)
/** 深色主题弱描边。 */
val OutlineVariantDark = Color(0xFF465345)

/** 错误色。 */
val ErrorRed = Color(0xFFDC3545)
val ErrorRedDark = Color(0xFFFF8A8A)
val ErrorContainer = Color(0xFFFFDAD9)
val OnErrorContainer = Color(0xFF5C0010)

// ---------------------------------------------------------------------------
// Category accents — 7 大品类各有专属色，用于衣橱筛选与图表
// ---------------------------------------------------------------------------

val CatTop = Color(0xFF344E41)      // 上装 · 电光紫
val CatBottom = Color(0xFF648369)   // 下装 · 荧光青
val CatOnePiece = Color(0xFFB47761) // 连体 · 玫粉
val CatOuterwear = Color(0xFFB97B50)// 外套 · 珊瑚橙
val CatShoes = Color(0xFFD9EC9C)    // 鞋 · 柠檬黄
val CatBag = Color(0xFF718165)      // 包 · 蓝紫
val CatAccessory = Color(0xFF678C83)// 配饰 · 湖水蓝
