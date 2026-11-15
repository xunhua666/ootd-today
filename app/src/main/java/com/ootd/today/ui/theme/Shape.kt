package com.ootd.today.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 形状体系。
 *
 * 潮流风格倾向于"大圆角卡片 + 药丸形按钮"的组合：
 * 卡片圆角够大显得松弛、不刻板；按钮做成全圆角药丸形则强化年轻活力感。
 */
val OotdShapes = Shapes(
    // Chip、小标签
    extraSmall = RoundedCornerShape(8.dp),
    // 输入框、小元素
    small = RoundedCornerShape(12.dp),
    // 默认卡片、按钮
    medium = RoundedCornerShape(18.dp),
    // 大卡片、弹窗
    large = RoundedCornerShape(26.dp),
    // 底部弹层（顶部两角大圆角）
    extraLarge = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
)

/** 药丸形（全圆角），用于主操作按钮和 Chip。 */
val PillShape = RoundedCornerShape(percent = 50)
