package com.wiverhome.radio

import java.net.URI
import java.util.UUID

/** Своя станция, добавленная по ссылке на поток. Хранится в избранном, id начинается с [PREFIX]. */
object CustomStation {
    const val PREFIX = "custom:"

    sealed interface Result {
        data class Ok(val station: Station) : Result
        data class Error(val message: String) : Result
    }

    /** Проверяет ссылку и собирает станцию. Пустое название заменяется адресом сервера. */
    fun create(name: String, url: String, id: String = PREFIX + UUID.randomUUID()): Result {
        val link = url.trim()
        if (link.isEmpty()) return Result.Error("Вставьте ссылку на поток")
        val uri = try {
            URI(link)
        } catch (e: Exception) {
            return Result.Error("Ссылка написана с ошибкой")
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return Result.Error("Ссылка должна начинаться с http:// или https://")
        val host = uri.host?.takeIf { it.isNotBlank() } ?: return Result.Error("В ссылке нет адреса сервера")
        return Result.Ok(
            Station(
                id = id,
                name = name.trim().ifEmpty { host },
                url = link,
                favicon = "",
                country = "",
                countryCode = "",
                tags = "",
                bitrate = 0,
            ),
        )
    }

    fun isCustom(station: Station) = station.id.startsWith(PREFIX)

    /** Адрес сервера для подписи в списке: «Своя станция · stream.example.com». */
    fun host(station: Station): String = try {
        URI(station.url).host.orEmpty()
    } catch (e: Exception) {
        ""
    }
}
