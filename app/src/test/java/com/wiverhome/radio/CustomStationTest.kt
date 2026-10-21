package com.wiverhome.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomStationTest {
    private fun ok(name: String, url: String) =
        (CustomStation.create(name, url, id = "custom:1") as CustomStation.Result.Ok).station

    @Test
    fun createsStationFromLink() {
        val s = ok("Моё радио", "  https://stream.example.com:8000/live.mp3 ")
        assertEquals("custom:1", s.id)
        assertEquals("Моё радио", s.name)
        assertEquals("https://stream.example.com:8000/live.mp3", s.url)
        assertTrue(CustomStation.isCustom(s))
    }

    @Test
    fun emptyNameBecomesHost() {
        assertEquals("stream.example.com", ok("  ", "http://stream.example.com/radio").name)
    }

    @Test
    fun defaultIdIsUniqueAndCustom() {
        val a = (CustomStation.create("", "http://a.example.com") as CustomStation.Result.Ok).station
        val b = (CustomStation.create("", "http://a.example.com") as CustomStation.Result.Ok).station
        assertTrue(a.id.startsWith(CustomStation.PREFIX))
        assertTrue(a.id != b.id)
    }

    @Test
    fun rejectsBadLinks() {
        listOf("", "   ", "stream.example.com/live", "ftp://example.com/a.mp3", "http://", "https://exa mple.com")
            .forEach { url ->
                assertTrue(url, CustomStation.create("x", url) is CustomStation.Result.Error)
            }
    }

    @Test
    fun catalogStationsAreNotCustom() {
        val s = Station("9617a958-0601-11e8-ae97-52543be04c81", "Радио", "http://x.ru", "", "", "", "", 0)
        assertFalse(CustomStation.isCustom(s))
    }

    @Test
    fun detailsShowHostForCustom() {
        assertEquals("Своя станция · stream.example.com", ok("A", "http://stream.example.com/live").details(null))
    }
}
