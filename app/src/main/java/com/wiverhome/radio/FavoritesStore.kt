package com.wiverhome.radio

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<Station>> = _items.asStateFlow()

    fun toggle(station: Station) {
        val current = _items.value
        val updated = if (current.any { it.id == station.id }) {
            current.filterNot { it.id == station.id }
        } else {
            current + station
        }
        _items.value = updated
        val array = JSONArray()
        updated.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun load(): List<Station> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { Station.fromJson(array.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val KEY = "stations"
    }
}
