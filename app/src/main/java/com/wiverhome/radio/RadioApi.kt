package com.wiverhome.radio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
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

    private suspend fun search(filters: List<String>): List<Station> = withContext(Dispatchers.IO) {
        val params = filters + listOf("hidebroken=true", "order=clickcount", "reverse=true", "limit=$LIMIT")
        val path = "/json/stations/search?" + params.joinToString("&")
        var lastError: Exception? = null
        for (i in hosts.indices) {
            val index = (preferredHost + i) % hosts.size
            try {
                val stations = parse(get("https://${hosts[index]}$path"))
                preferredHost = index
                return@withContext stations
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
            conn.connectTimeout = 8000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "RadioJeka/1.0")
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
