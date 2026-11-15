package com.ootd.today.domain

import com.ootd.today.data.model.*
import org.junit.Assert.*
import org.junit.Test

class WardrobeBrowserTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day
    private fun item(id: Long = 1, category: ClothingCategory = ClothingCategory.TOP, worn: Long? = null, count: Int = 0) = WardrobeItem(
        id=id, name="白色基础衬衫", category=category, subCategory="衬衫", imagePath=null,
        colorArgb=0xFFFFFFFF, colorName="白色", warmthLevel=2, seasons=Season.entries.toSet(),
        occasions=Occasion.entries.toSet(), style=StyleTag.MINIMAL, brand="Uniqlo", price=null,
        purchaseDate=null, lastWornDate=worn, status=ItemStatus.ACTIVE, note=null, wearCount=count
    )

    @Test fun `search matches multiple fields and ignores case and spaces`() {
        assertEquals(1, browseWardrobe(listOf(item()), WardrobeFilters(query="  UNIQLO   白色 极简 "), now).size)
        assertTrue(browseWardrobe(listOf(item()), WardrobeFilters(query="不存在"), now).isEmpty())
    }

    @Test fun `idle threshold includes day thirty and excludes future dates`() {
        assertTrue(item().isIdle(now))
        assertTrue(item(worn=now-30*day).isIdle(now))
        assertFalse(item(worn=now-30*day+1).isIdle(now))
        assertFalse(item(worn=now+day).isIdle(now))
    }

    @Test fun `category search idle and active filters apply together`() {
        val clothes=listOf(item(1),item(2,ClothingCategory.BOTTOM),item(3,worn=now),item(4).copy(status=ItemStatus.RETIRED))
        assertEquals(listOf(1L),browseWardrobe(clothes,WardrobeFilters(category=ClothingCategory.TOP,query="白色",idleOnly=true),now).map { it.id })
    }

    @Test fun `sort handles new and most or least worn items deterministically`() {
        val clothes=listOf(item(1,count=5),item(3,count=2),item(2,count=2))
        assertEquals(listOf(3L,2L,1L),browseWardrobe(clothes,WardrobeFilters(sort=WardrobeSort.NEWEST),now).map {it.id})
        assertEquals(listOf(1L,3L,2L),browseWardrobe(clothes,WardrobeFilters(sort=WardrobeSort.MOST_WORN),now).map {it.id})
        assertEquals(listOf(3L,2L,1L),browseWardrobe(clothes,WardrobeFilters(sort=WardrobeSort.LEAST_WORN),now).map {it.id})
    }
}
