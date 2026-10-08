package com.wiverhome.radio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Offline station catalog: a Radio Browser snapshot shipped in assets (tools/update_catalog.py),
 * replaced by a fresher copy in filesDir whenever the online catalog is reachable.
 * Works without access to radio-browser.info (blocked in Russia without a VPN).
 */
class StationCatalog(context: Context) {
    private val app = context.applicationContext
    private val file = File(app.filesDir, FILE_NAME)
    private val prefs = app.getSharedPreferences("catalog", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private var stations: List<Station>? = null

    suspend fun query(name: String, countryCode: String?, tags: List<String>): List<Station> {
        val all = load()
        return withContext(Dispatchers.Default) {
            all.asSequence()
                .filter { countryCode == null || it.countryCode.equals(countryCode, ignoreCase = true) }
                .filter { tags.isEmpty() || it.matchesAnyTag(tags) }
                .filter { name.isEmpty() || it.name.contains(name, ignoreCase = true) }
                .take(LIMIT)
                .toList()
        }
    }

    /** Downloads a fresh snapshot at most once a day. Returns true if the catalog changed. */
    suspend fun refreshIfStale(): Boolean {
        if (System.currentTimeMillis() - prefs.getLong(KEY_UPDATED, 0) < REFRESH_INTERVAL_MS) return false
        val fresh = try {
            RadioApi.snapshot()
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            return false
        }
        if (fresh.size < MIN_VALID_SIZE) return false
        withContext(Dispatchers.IO) {
            val array = JSONArray()
            fresh.forEach { array.put(it.toCompactJson()) }
            val tmp = File(app.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(array.toString())
            tmp.renameTo(file)
        }
        mutex.withLock { stations = fresh.sortedByDescending { it.clicks } }
        prefs.edit().putLong(KEY_UPDATED, System.currentTimeMillis()).apply()
        return true
    }

    private suspend fun load(): List<Station> = mutex.withLock {
        stations ?: withContext(Dispatchers.IO) {
            val text = file.takeIf { it.exists() }?.runCatching { readText() }?.getOrNull()
                ?: app.assets.open(FILE_NAME).bufferedReader().use { it.readText() }
            val array = JSONArray(text)
            (0 until array.length())
                .mapNotNull { Station.fromCompactJson(array.getJSONObject(it)) }
                .sortedByDescending { it.clicks }
        }.also { stations = it }
    }

    private fun Station.matchesAnyTag(wanted: List<String>): Boolean {
        val own = tags.lowercase()
        return wanted.any { own.contains(it) }
    }

    private companion object {
        const val FILE_NAME = "stations.json"
        const val KEY_UPDATED = "updated"
        const val LIMIT = 200
        const val MIN_VALID_SIZE = 1000
        const val REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}

private fun Station.toCompactJson(): JSONObject = JSONObject()
    .put("id", id).put("n", name).put("u", url).put("f", favicon)
    .put("c", country).put("cc", countryCode).put("t", tags).put("b", bitrate).put("k", clicks)

private fun Station.Companion.fromCompactJson(o: JSONObject): Station? {
    val url = o.optString("u")
    val name = o.optString("n")
    if (url.isEmpty() || name.isEmpty()) return null
    return Station(
        id = o.optString("id"),
        name = name,
        url = url,
        favicon = o.optString("f"),
        country = o.optString("c"),
        countryCode = o.optString("cc"),
        tags = o.optString("t"),
        bitrate = o.optInt("b"),
        clicks = o.optInt("k"),
    )
}
