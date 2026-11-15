package com.ootd.today.domain

import com.ootd.today.data.model.*

/** Soft preferences, never a substitute for weather, occasion or a user's taste. */
object SilhouetteRules {
    private fun upperShape(items: List<WardrobeItem>): GarmentShape? =
        items.firstOrNull { it.category == ClothingCategory.OUTERWEAR && it.shape.confirmed && (it.shape.fit != GarmentFit.UNKNOWN || it.shape.length != GarmentLength.UNKNOWN) }?.shape
            ?: items.firstOrNull { it.category == ClothingCategory.TOP && it.shape.confirmed }?.shape
    fun score(items: List<WardrobeItem>): Double? {
        val top = upperShape(items) ?: return null
        val bottom = items.firstOrNull { it.category == ClothingCategory.BOTTOM && it.shape.confirmed }?.shape ?: return null
        val factors = mutableListOf<Double>()
        val upperLoose = top.fit in setOf(GarmentFit.RELAXED, GarmentFit.OVERSIZED)
        val lowerNarrow = bottom.fit == GarmentFit.FITTED || bottom.cut in setOf(GarmentCut.SKINNY, GarmentCut.TAPERED, GarmentCut.STRAIGHT)
        if (upperLoose && lowerNarrow) factors += .95
        if (top.fit in setOf(GarmentFit.FITTED, GarmentFit.REGULAR) && bottom.cut in setOf(GarmentCut.WIDE, GarmentCut.FLARED, GarmentCut.A_LINE)) factors += .93
        if (top.length == GarmentLength.CROPPED && bottom.rise == WaistRise.HIGH) factors += .98
        if (top.length == GarmentLength.LONG && bottom.cut == GarmentCut.WIDE) factors += .65
        if (top.fit == GarmentFit.OVERSIZED && bottom.fit == GarmentFit.OVERSIZED) factors += .70
        return factors.takeIf { it.isNotEmpty() }?.average()
    }
    fun describe(items: List<WardrobeItem>): String? {
        val top = upperShape(items) ?: return null
        val bottom = items.firstOrNull { it.category == ClothingCategory.BOTTOM && it.shape.confirmed }?.shape ?: return null
        return when {
            top.length == GarmentLength.CROPPED && bottom.rise == WaistRise.HIGH -> "短上装衔接高腰下装，腰线更清楚"
            top.fit in setOf(GarmentFit.RELAXED, GarmentFit.OVERSIZED) && bottom.cut in setOf(GarmentCut.STRAIGHT, GarmentCut.TAPERED, GarmentCut.SKINNY) -> "上宽下利落，轮廓有主次"
            top.fit in setOf(GarmentFit.FITTED, GarmentFit.REGULAR) && bottom.cut == GarmentCut.WIDE -> "上装收敛、下装舒展，轮廓更有层次"
            top.length == GarmentLength.LONG && bottom.cut == GarmentCut.WIDE -> "长上装配阔腿下装，可试着塞一角衣摆，露出腰线"
            else -> null
        }
    }
}
