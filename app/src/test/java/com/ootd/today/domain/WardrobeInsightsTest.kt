package com.ootd.today.domain

import com.ootd.today.data.local.demoWardrobeItems
import com.ootd.today.data.model.ItemStatus
import org.junit.Assert.*
import org.junit.Test

class WardrobeInsightsTest {
    private val sample = demoWardrobeItems().first()

    @Test fun `experience IDs are unique negative IDs and excluded from cost advice`() {
        val demo = demoWardrobeItems()
        assertTrue(demo.isNotEmpty())
        assertTrue(demo.all { it.isDemo })
        assertEquals(demo.size, demo.map { it.id }.toSet().size)
        assertNull(rewearOpportunity(demo))
    }

    @Test fun `cost projection uses five additional wears and chooses the largest current cost`() {
        val cheap = sample.copy(id=1,price=100.0,wearCount=10)
        val expensive = sample.copy(id=2,price=600.0,wearCount=5)
        val result = rewearOpportunity(listOf(cheap,expensive))!!
        assertEquals(2L,result.item.id)
        assertEquals(120.0,result.currentCost,0.001)
        assertEquals(60.0,result.afterFiveMoreWears,0.001)
    }

    @Test fun `missing prices zero wears retired and experience records are never suggested`() {
        val real = sample.copy(id=1)
        assertNull(rewearOpportunity(listOf(
            sample, real.copy(price=null),real.copy(price=100.0,wearCount=0),
            real.copy(price=-1.0,wearCount=1),real.copy(status=ItemStatus.RETIRED)
        )))
    }
}
