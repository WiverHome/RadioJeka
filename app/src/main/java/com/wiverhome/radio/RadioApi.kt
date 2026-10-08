package com.wiverhome.radio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException

/** Station catalog from https://www.radio-browser.info (free, no key). */
object RadioApi {
    private val hosts = listOf(
        "de1.api.radio-browser.info",
        "all.api.radio-browser.info",
    )
    private const val LIMIT = 80
    private const val SNAPSHOT_WORLD_TOP = 4000

    @Volatile
    private var preferredHost = 0

    /**
     * Most listened stations matching all given filters.
     * [countryCode] null means worldwide; several [tags] are OR-ed (one request per tag, merged).
     */
    suspend fun stations(name: String, countryCode: String?, tags: List<String>): List<Station> = coroutineScope {
        val filters = buildList {
            if (name.isNotEmpty()) add("name=${enc(name)}")
            if (countryCode != null) add("countrycode=${enc(countryCode)}")
        }
        if (tags.isEmpty()) {
            search(filters)
        } else {
            tags.map { tag -> async { search(filters + "tag=${enc(tag)}") } }
                .awaitAll()
                .flatten()
                .distinctBy { it.url }
                .sortedByDescending { it.clicks }
                .take(LIMIT)
        }
    }

    /** Same content as tools/update_catalog.py: every Russian station plus the world's most popular. */
    suspend fun snapshot(): List<Station> = coroutineScope {
        val russia = async { search(listOf("countrycode=RU"), limit = 100_000) }
        val world = async { search(emptyList(), limit = SNAPSHOT_WORLD_TOP) }
        (russia.await() + world.await()).distinctBy { it.url }
    }

    private suspend fun search(filters: List<String>, limit: Int = LIMIT): List<Station> = runInterruptible(Dispatchers.IO) {
        val params = filters + listOf("hidebroken=true", "order=clickcount", "reverse=true", "limit=$limit")
        val path = "/json/stations/search?" + params.joinToString("&")
        var lastError: Exception? = null
        for (i in hosts.indices) {
            // Cancelled while blocked on the network: don't try the next mirror.
            if (Thread.currentThread().isInterrupted) throw CancellationException("Search cancelled")
            val index = (preferredHost + i) % hosts.size
            try {
                val stations = parse(get("https://${hosts[index]}$path"))
                preferredHost = index
                return@runInterruptible stations
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("No servers")
    }

    private fun parse(body: String): List<Station> {
        val array = JSONArray(body)
        return (0 until array.length())
            .mapNotNull { Station.fromApi(array.getJSONObject(it)) }
            .distinctBy { it.url }
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 6000
            conn.readTimeout = 20000
            conn.setRequestProperty("User-Agent", "RadioJeka/1.0")
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
