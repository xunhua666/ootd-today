package com.ootd.today.data.local

import com.ootd.today.data.model.OutfitLog
import com.ootd.today.data.model.Outfit
import com.ootd.today.data.model.WardrobeItem
import com.ootd.today.domain.vision.GarmentShapeCodec

/** 领域模型 -> 数据库实体。 */
fun WardrobeItem.toEntity(): WardrobeItemEntity = WardrobeItemEntity(
    id = id,
    name = name,
    category = category,
    subCategory = subCategory,
    imagePath = imagePath,
    colorArgb = colorArgb,
    colorName = colorName,
    warmthLevel = warmthLevel,
    seasons = seasons,
    occasions = occasions,
    style = style,
    brand = brand,
    price = price,
    purchaseDate = purchaseDate,
    lastWornDate = lastWornDate,
    status = status,
    note = note,
    wearCount = wearCount,
    shapeJson = GarmentShapeCodec.encode(shape)
)

/** 数据库实体 -> 领域模型。 */
fun WardrobeItemEntity.toModel(): WardrobeItem = WardrobeItem(
    id = id,
    name = name,
    category = category,
    subCategory = subCategory,
    imagePath = imagePath,
    colorArgb = colorArgb,
    colorName = colorName,
    warmthLevel = warmthLevel,
    seasons = seasons,
    occasions = occasions,
    style = style,
    brand = brand,
    price = price,
    purchaseDate = purchaseDate,
    lastWornDate = lastWornDate,
    status = status,
    note = note,
    wearCount = wearCount,
    shape = GarmentShapeCodec.decode(shapeJson)
)

/**
 * 领域模型 -> 数据库实体。
 *
 * 搭配里的单品列表被编码成快照字符串，理由见 [OutfitSnapshotCodec]。
 */
fun OutfitLog.toEntity(): OutfitLogEntity = OutfitLogEntity(
    id = id,
    timestamp = timestamp,
    occasion = outfit.occasion,
    score = outfit.score,
    reason = outfit.reason,
    itemsSnapshot = OutfitSnapshotCodec.encode(outfit.items),
    mood = mood,
    note = note
)

/** 数据库实体 -> 领域模型。快照解码失败时得到空单品列表，页面仍可展示文字信息。 */
fun OutfitLogEntity.toModel(): OutfitLog = OutfitLog(
    id = id,
    timestamp = timestamp,
    outfit = Outfit(
        id = id,
        items = OutfitSnapshotCodec.decode(itemsSnapshot),
        occasion = occasion,
        score = score,
        reason = reason
    ),
    mood = mood,
    note = note
)
