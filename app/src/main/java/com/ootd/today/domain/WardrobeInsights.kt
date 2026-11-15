package com.ootd.today.domain

import com.ootd.today.data.model.ItemStatus
import com.ootd.today.data.model.WardrobeItem

data class RewearOpportunity(val item: WardrobeItem, val currentCost: Double, val afterFiveMoreWears: Double)

/** Every suggestion refers to a real, active garment that can be worn again. */
fun rewearOpportunity(items: List<WardrobeItem>): RewearOpportunity? = items
    .filter { !it.isDemo && it.status == ItemStatus.ACTIVE && (it.price ?: 0.0) > 0 && it.wearCount > 0 }
    .maxByOrNull { it.price!! / it.wearCount }
    ?.let { RewearOpportunity(it, it.price!! / it.wearCount, it.price / (it.wearCount + 5)) }
