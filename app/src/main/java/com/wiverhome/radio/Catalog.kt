package com.wiverhome.radio

/** Region filter: a country code, or null for worldwide. */
enum class Region(val title: String, val countryCode: String?) {
    RUSSIA("Россия", "RU"),
    WORLD("Весь мир", null),
}

/** Genre chip; stations matching any of the Radio Browser [tags] belong to it. */
data class Genre(val title: String, val tags: List<String>)

val GENRES = listOf(
    Genre("Поп", listOf("pop")),
    Genre("Рок", listOf("rock")),
    Genre("Новости", listOf("news")),
    Genre("Разговорное", listOf("talk")),
    Genre("Танцевальная", listOf("dance")),
    Genre("Электроника", listOf("electronic")),
    Genre("Хип-хоп", listOf("hip hop", "rap")),
    Genre("Ретро", listOf("retro", "80s", "oldies")),
    Genre("Шансон", listOf("chanson", "шансон")),
    Genre("Джаз", listOf("jazz")),
    Genre("Классика", listOf("classical")),
    Genre("Лаунж", listOf("chillout", "lounge")),
    Genre("Метал", listOf("metal")),
    Genre("Фолк", listOf("folk")),
)
