package com.ootd.today.data.local

import com.ootd.today.data.model.ClothingCategory
import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.Occasion
import com.ootd.today.data.model.Season
import com.ootd.today.data.model.StyleTag

/**
 * 首次启动的示例单品。
 *
 * 目的有二：
 * 1. 让新用户打开 App 立刻有内容可看，推荐算法也有素材可算，避免「空壳感」；
 * 2. 覆盖全部 7 个大类、不同保暖度与场合，保证任何温度下都能搭出合法组合。
 *
 * 这些数据只在衣橱表为空时写入一次，之后用户增删完全由用户自己掌控，
 * 不会在每次启动时「复活」被删掉的单品。
 */
internal fun seedItems(): List<WardrobeItemEntity> = listOf(
    item("白色基础圆领T", ClothingCategory.TOP, "T恤", 0xFFF5F5F5, "本白", 1,
        setOf(Season.SPRING, Season.SUMMER),
        setOf(Occasion.COMMUTE, Occasion.DATE, Occasion.HOME),
        StyleTag.MINIMAL, "Uniqlo", 79.0, 12),
    item("雾霾蓝 oversize 卫衣", ClothingCategory.TOP, "卫衣", 0xFF7C9CBF, "雾霾蓝", 3,
        setOf(Season.SPRING, Season.AUTUMN),
        setOf(Occasion.COMMUTE, Occasion.SPORT, Occasion.HOME),
        StyleTag.STREET, null, null, 5),
    item("条纹法式衬衫", ClothingCategory.TOP, "衬衫", 0xFFE8E3D9, "米白条纹", 2,
        setOf(Season.SPRING, Season.AUTUMN),
        setOf(Occasion.COMMUTE, Occasion.FORMAL, Occasion.DATE),
        StyleTag.PREPPY, null, null, 2),
    item("高腰直筒牛仔裤", ClothingCategory.BOTTOM, "牛仔裤", 0xFF3A5F8A, "中蓝", 3,
        setOf(Season.SPRING, Season.AUTUMN, Season.WINTER),
        setOf(Occasion.COMMUTE, Occasion.DATE, Occasion.SPORT),
        StyleTag.STREET, "Levi's", 599.0, 20),
    item("垂坠感阔腿西裤", ClothingCategory.BOTTOM, "西裤", 0xFF2B2B2B, "炭黑", 3,
        setOf(Season.SPRING, Season.AUTUMN, Season.WINTER),
        setOf(Occasion.COMMUTE, Occasion.FORMAL),
        StyleTag.MINIMAL, null, null, 7),
    item("亚麻短裤", ClothingCategory.BOTTOM, "短裤", 0xFFD9C7A7, "燕麦", 1,
        setOf(Season.SUMMER),
        setOf(Occasion.VACATION, Occasion.HOME, Occasion.SPORT),
        StyleTag.MINIMAL, null, null, 3),
    item("碎花茶歇连衣裙", ClothingCategory.ONE_PIECE, "连衣裙", 0xFFE8A0B4, "藕粉", 2,
        setOf(Season.SPRING, Season.SUMMER),
        setOf(Occasion.DATE, Occasion.VACATION),
        StyleTag.SWEET, null, null, 1),
    item("黑色风衣", ClothingCategory.OUTERWEAR, "风衣", 0xFF1F1F1F, "墨黑", 3,
        setOf(Season.SPRING, Season.AUTUMN),
        setOf(Occasion.COMMUTE, Occasion.FORMAL, Occasion.DATE),
        StyleTag.MINIMAL, null, 1280.0, 9),
    item("白色短款羽绒服", ClothingCategory.OUTERWEAR, "羽绒服", 0xFFFDFDFD, "雪白", 5,
        setOf(Season.WINTER),
        setOf(Occasion.COMMUTE, Occasion.DATE, Occasion.VACATION),
        StyleTag.STREET, null, null, 4),
    item("复古皮质机车夹克", ClothingCategory.OUTERWEAR, "皮衣", 0xFF4A3226, "棕褐", 3,
        setOf(Season.SPRING, Season.AUTUMN),
        setOf(Occasion.DATE, Occasion.VACATION),
        StyleTag.RETRO, null, null, 0),
    item("德训小白鞋", ClothingCategory.SHOES, "运动鞋", 0xFFFAFAFA, "白", 2,
        setOf(Season.SPRING, Season.SUMMER, Season.AUTUMN),
        setOf(Occasion.COMMUTE, Occasion.SPORT, Occasion.DATE, Occasion.VACATION),
        StyleTag.STREET, "adidas", 899.0, 31),
    item("粗跟切尔西靴", ClothingCategory.SHOES, "靴子", 0xFF3B2B23, "深棕", 4,
        setOf(Season.AUTUMN, Season.WINTER),
        setOf(Occasion.COMMUTE, Occasion.FORMAL, Occasion.DATE),
        StyleTag.RETRO, null, null, 6),
    item("尼龙双肩包", ClothingCategory.BAG, "双肩包", 0xFF2F3542, "藏青", 1,
        setOf(Season.SPRING, Season.SUMMER, Season.AUTUMN, Season.WINTER),
        setOf(Occasion.COMMUTE, Occasion.SPORT),
        StyleTag.TECHWEAR, null, null, 15),
    item("编织托特包", ClothingCategory.BAG, "手提包", 0xFFC9A876, "焦糖", 1,
        setOf(Season.SPRING, Season.SUMMER),
        setOf(Occasion.DATE, Occasion.VACATION),
        StyleTag.RETRO, null, null, 2),
    item("羊绒围巾", ClothingCategory.ACCESSORY, "围巾", 0xFFB8A88A, "驼色", 4,
        setOf(Season.AUTUMN, Season.WINTER),
        setOf(Occasion.COMMUTE, Occasion.DATE, Occasion.FORMAL),
        StyleTag.MINIMAL, null, null, 8),
    item("棒球帽", ClothingCategory.ACCESSORY, "帽子", 0xFF1A1A1A, "黑", 1,
        setOf(Season.SPRING, Season.SUMMER, Season.AUTUMN),
        setOf(Occasion.SPORT, Occasion.VACATION),
        StyleTag.STREET, null, null, 11)
)

private fun item(
    name: String,
    category: ClothingCategory,
    subCategory: String,
    colorArgb: Long,
    colorName: String,
    warmthLevel: Int,
    seasons: Set<Season>,
    occasions: Set<Occasion>,
    style: StyleTag?,
    brand: String?,
    price: Double?,
    wearCount: Int
): WardrobeItemEntity = WardrobeItemEntity(
    id = 0,
    name = name,
    category = category,
    subCategory = subCategory,
    imagePath = null,
    colorArgb = colorArgb,
    colorName = colorName,
    warmthLevel = warmthLevel,
    seasons = seasons,
    occasions = occasions,
    style = style,
    brand = brand,
    price = price,
    purchaseDate = null,
    lastWornDate = null,
    status = ItemStatus.ACTIVE,
    note = null,
    wearCount = wearCount
)

/** Experience content has stable negative IDs and never enters the personal database. */
internal fun demoWardrobeItems(): List<com.ootd.today.data.model.WardrobeItem> =
    seedItems().mapIndexed { index, item -> item.copy(id = -(index + 1L)).toModel() }
