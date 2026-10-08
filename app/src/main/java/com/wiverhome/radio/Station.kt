package com.wiverhome.radio

import org.json.JSONObject

data class Station(
    val id: String,
    val name: String,
    val url: String,
    val favicon: String,
    val country: String,
    val countryCode: String,
    val tags: String,
    val bitrate: Int,
    /** Popularity, only used to sort merged search results; not saved with favorites. */
    val clicks: Int = 0,
) {
    /** "Russia · pop, rock · 128 kbps"; the country is skipped when the list is already filtered by it. */
    fun details(hideCountryCode: String?): String =
        listOfNotNull(
            country.ifBlank { null }?.takeUnless { hideCountryCode != null && countryCode.equals(hideCountryCode, true) },
            tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.take(2)
                .joinToString(", ").ifBlank { null },
            if (bitrate > 0) "$bitrate kbps" else null,
        ).joinToString(" · ")

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("url", url)
        .put("favicon", favicon)
        .put("country", country)
        .put("countryCode", countryCode)
        .put("tags", tags)
        .put("bitrate", bitrate)

    companion object {
        fun fromJson(o: JSONObject) = Station(
            id = o.optString("id"),
            name = o.optString("name"),
            url = o.optString("url"),
            favicon = o.optString("favicon"),
            country = o.optString("country"),
            countryCode = o.optString("countryCode"),
            tags = o.optString("tags"),
            bitrate = o.optInt("bitrate"),
        )

        /** Station object from the Radio Browser API, or null when it has no stream. */
        fun fromApi(o: JSONObject): Station? {
            val url = o.optString("url_resolved").ifBlank { o.optString("url") }.trim()
            val name = o.optString("name").trim()
            if (url.isEmpty() || name.isEmpty()) return null
            return Station(
                id = o.optString("stationuuid"),
                name = name,
                url = url,
                favicon = o.optString("favicon").trim(),
                country = o.optString("country").trim(),
                countryCode = o.optString("countrycode").trim(),
                tags = o.optString("tags"),
                bitrate = o.optInt("bitrate"),
                clicks = o.optInt("clickcount"),
            )
        }
    }
}
