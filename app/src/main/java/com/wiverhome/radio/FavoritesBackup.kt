package com.wiverhome.radio

import org.json.JSONArray
import org.json.JSONObject

/** Резервная копия избранного в файл: перенос на другой телефон. Формат — полный JSON станции, как в избранном. */
object FavoritesBackup {
    private const val APP = "RadioJeka"
    private const val VERSION = 1

    fun encode(favorites: List<Station>): String {
        val array = JSONArray()
        favorites.forEach { array.put(it.toJson()) }
        return JSONObject()
            .put("app", APP)
            .put("version", VERSION)
            .put("favorites", array)
            .toString(2)
    }

    /** Станции из файла. Бросает [IllegalArgumentException], если это не резервная копия RadioJeka. */
    fun decode(text: String): List<Station> {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("not json", e)
        }
        require(root.optString("app") == APP) { "not a RadioJeka backup" }
        val array = root.optJSONArray("favorites") ?: throw IllegalArgumentException("no favorites")
        return (0 until array.length())
            .mapNotNull { array.optJSONObject(it)?.let(Station::fromJson) }
            .filter { it.id.isNotBlank() && it.url.isNotBlank() }
    }

    /** Добавляет к текущему избранному станции из копии, которых ещё нет; порядок текущих не меняется. */
    fun merge(current: List<Station>, imported: List<Station>): List<Station> {
        val ids = current.mapTo(HashSet()) { it.id }
        return current + imported.filter { ids.add(it.id) }
    }
}
