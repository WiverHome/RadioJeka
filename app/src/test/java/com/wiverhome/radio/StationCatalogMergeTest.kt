package com.wiverhome.radio

import org.junit.Assert.assertEquals
import org.junit.Test

class StationCatalogMergeTest {
    private fun station(id: String, url: String = "http://$id", clicks: Int = 0, name: String = id) =
        Station(id, name, url, "", "", "", "", 0, clicks)

    @Test
    fun bundledAddsStationsMissingFromFresh() {
        val merged = StationCatalog.merge(
            fresh = listOf(station("a", clicks = 10)),
            bundled = listOf(station("a", clicks = 5), station("rare", clicks = 1)),
        )
        assertEquals(listOf("a", "rare"), merged.map { it.id })
    }

    @Test
    fun freshWinsOnSameId() {
        val merged = StationCatalog.merge(
            fresh = listOf(station("a", url = "http://new", name = "Новое")),
            bundled = listOf(station("a", url = "http://old", name = "Старое")),
        )
        assertEquals(1, merged.size)
        assertEquals("http://new", merged[0].url)
    }

    @Test
    fun sameLinkIsKeptOnce() {
        val merged = StationCatalog.merge(
            fresh = listOf(station("a", url = "http://same")),
            bundled = listOf(station("b", url = "http://same")),
        )
        assertEquals(listOf("a"), merged.map { it.id })
    }

    @Test
    fun sortedByPopularity() {
        val merged = StationCatalog.merge(
            fresh = listOf(station("low", clicks = 1)),
            bundled = listOf(station("high", clicks = 100)),
        )
        assertEquals(listOf("high", "low"), merged.map { it.id })
    }

    @Test
    fun worksWithoutFreshSnapshot() {
        val merged = StationCatalog.merge(emptyList(), listOf(station("a"), station("b")))
        assertEquals(2, merged.size)
    }
}
