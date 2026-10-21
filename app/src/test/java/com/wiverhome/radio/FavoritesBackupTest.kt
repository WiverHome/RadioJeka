package com.wiverhome.radio

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesBackupTest {
    private fun station(id: String) = Station(id, "Станция $id", "http://$id.example.com/live", "", "Russia", "RU", "pop", 128)

    @Test
    fun roundTripKeepsStationsAndOrder() {
        val favorites = listOf(station("b"), station("a"), station("custom:c"))
        assertEquals(favorites, FavoritesBackup.decode(FavoritesBackup.encode(favorites)))
    }

    @Test
    fun emptyFavoritesRoundTrip() {
        assertEquals(emptyList<Station>(), FavoritesBackup.decode(FavoritesBackup.encode(emptyList())))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonJson() {
        FavoritesBackup.decode("это не json")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsForeignJson() {
        FavoritesBackup.decode("""{"favorites":[]}""")
    }

    @Test
    fun skipsBrokenEntries() {
        val text = """{"app":"RadioJeka","version":1,"favorites":[{"id":"a","url":"http://a"},{"id":"","url":"http://b"},{"id":"c"},42]}"""
        assertEquals(listOf("a"), FavoritesBackup.decode(text).map { it.id })
    }

    @Test
    fun mergeAddsOnlyNewStationsAfterCurrent() {
        val current = listOf(station("a"), station("b"))
        val imported = listOf(station("c"), station("a"), station("d"), station("c"))
        assertEquals(listOf("a", "b", "c", "d"), FavoritesBackup.merge(current, imported).map { it.id })
    }
}
