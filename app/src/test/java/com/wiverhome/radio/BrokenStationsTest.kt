package com.wiverhome.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokenStationsTest {
    private fun station(id: String) = Station(id, id, "http://$id", "", "", "", "", 0)

    @Test
    fun markAndUnmark() {
        val b = BrokenStations()
        assertTrue(b.markBroken("a", now = 0))
        assertFalse("повторная пометка ничего не меняет", b.markBroken("a", now = 10))
        assertTrue(b.isBroken("a", now = 20))
        assertTrue(b.markWorking("a"))
        assertFalse(b.isBroken("a", now = 30))
        assertFalse(b.markWorking("a"))
    }

    @Test
    fun markExpires() {
        val b = BrokenStations(mapOf("a" to 0L))
        assertTrue(b.isBroken("a", now = BrokenStations.TTL_MS - 1))
        assertFalse(b.isBroken("a", now = BrokenStations.TTL_MS))
    }

    @Test
    fun pruneRemovesOnlyExpired() {
        val b = BrokenStations(mapOf("old" to 0L, "new" to BrokenStations.TTL_MS))
        assertTrue(b.prune(now = BrokenStations.TTL_MS + 1))
        assertEquals(setOf("new"), b.snapshot().keys)
        assertFalse(b.prune(now = BrokenStations.TTL_MS + 2))
    }

    @Test
    fun brokenStationsGoLastKeepingOrder() {
        val b = BrokenStations(mapOf("b" to 0L, "d" to 0L))
        val list = listOf("a", "b", "c", "d", "e").map(::station)
        assertEquals(listOf("a", "c", "e", "b", "d"), b.workingFirst(list, now = 1).map { it.id })
    }

    @Test
    fun expiredMarksDoNotReorder() {
        val b = BrokenStations(mapOf("a" to 0L))
        val list = listOf("a", "b").map(::station)
        assertEquals(listOf("a", "b"), b.workingFirst(list, now = BrokenStations.TTL_MS).map { it.id })
    }
}
