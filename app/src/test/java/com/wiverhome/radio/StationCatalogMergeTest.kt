package com.wiverhome.radio

import org.junit.Assert.assertEquals
import org.junit.Test

class StationCatalogMergeTest {
    private fun station(id: String, url: String = "http://$id", clicks: Int = 0, name: String = id) =
        Station(id, name, url, "", "", "", "", 0, clicks)

    @Test
    fun extrasAddStationsMissingFromCore() {
        val merged = StationCatalog.merge(
            core = listOf(station("a", clicks = 10)),
            extras = listOf(station("a", clicks = 5), station("rare", clicks = 1)),
        )
        assertEquals(listOf("a", "rare"), merged.map { it.id })
    }

    @Test
    fun coreWinsOnSameId() {
        val merged = StationCatalog.merge(
            core = listOf(station("a", url = "http://new", name = "Новое")),
            extras = listOf(station("a", url = "http://old", name = "Старое")),
        )
        assertEquals(1, merged.size)
        assertEquals("http://new", merged[0].url)
    }

    @Test
    fun sameLinkIsKeptOnce() {
        val merged = StationCatalog.merge(
            core = listOf(station("a", url = "http://same")),
            extras = listOf(station("b", url = "http://same")),
        )
        assertEquals(listOf("a"), merged.map { it.id })
    }

    @Test
    fun sortedByPopularity() {
        val merged = StationCatalog.merge(
            core = listOf(station("low", clicks = 1)),
            extras = listOf(station("high", clicks = 100)),
        )
        assertEquals(listOf("high", "low"), merged.map { it.id })
    }

    @Test
    fun worksWithoutExtras() {
        val merged = StationCatalog.merge(listOf(station("a"), station("b")), emptyList())
        assertEquals(2, merged.size)
    }
}
