package com.ootd.today.domain

import com.ootd.today.data.model.*

/** An idle item has never been worn, or was last worn at least 30 days ago. */
fun WardrobeItem.isIdle(now: Long): Boolean = lastWornDate?.let { now - it >= 30L * 24 * 60 * 60 * 1000 } ?: true

enum class WardrobeSort(val label: String) { CATEGORY("默认排序"), NEWEST("最近加入"), MOST_WORN("穿着最多"), LEAST_WORN("穿着最少") }

data class WardrobeFilters(
    val category: ClothingCategory? = null,
    val query: String = "",
    val sort: WardrobeSort = WardrobeSort.CATEGORY,
    val idleOnly: Boolean = false,
    val activeOnly: Boolean = true
)

fun browseWardrobe(items: List<WardrobeItem>, filters: WardrobeFilters, now: Long): List<WardrobeItem> {
    val words = filters.query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val visible = items.filter { item ->
        val searchable = listOfNotNull(item.name, item.brand, item.colorName, item.subCategory, item.style?.label, item.category.label).joinToString(" ")
        (!filters.activeOnly || item.status == ItemStatus.ACTIVE) &&
            (filters.category == null || item.category == filters.category) &&
            (!filters.idleOnly || item.isIdle(now)) &&
            words.all { searchable.contains(it, ignoreCase = true) }
    }
    return when(filters.sort) {
        WardrobeSort.CATEGORY -> visible.sortedWith(compareBy<WardrobeItem> { it.category.ordinal }.thenByDescending { it.wearCount }.thenByDescending { it.id })
        WardrobeSort.NEWEST -> visible.sortedByDescending { it.id }
        WardrobeSort.MOST_WORN -> visible.sortedWith(compareByDescending<WardrobeItem> { it.wearCount }.thenByDescending { it.id })
        WardrobeSort.LEAST_WORN -> visible.sortedWith(compareBy<WardrobeItem> { it.wearCount }.thenByDescending { it.id })
    }
}
