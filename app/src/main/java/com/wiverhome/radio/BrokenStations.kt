package com.wiverhome.radio

/**
 * Станции, которые не заиграли при последней попытке. Пометка держится [TTL_MS] и снимается,
 * как только станция заиграет: потоки часто чинят, навсегда прятать нельзя.
 */
class BrokenStations(marks: Map<String, Long> = emptyMap()) {
    private val marks = marks.toMutableMap()

    fun snapshot(): Map<String, Long> = marks.toMap()

    /** true, если пометка изменилась (стоит сохранить). */
    fun markBroken(id: String, now: Long): Boolean = marks.put(id, now) == null

    fun markWorking(id: String): Boolean = marks.remove(id) != null

    fun isBroken(id: String, now: Long): Boolean = marks[id]?.let { now - it < TTL_MS } == true

    /** Убирает устаревшие пометки; true, если что-то убрано. */
    fun prune(now: Long): Boolean = marks.entries.removeAll { now - it.value >= TTL_MS }

    /** Рабочие станции сначала, неотвечающие — в конец, порядок внутри групп сохраняется. */
    fun workingFirst(stations: List<Station>, now: Long): List<Station> {
        val (broken, working) = stations.partition { isBroken(it.id, now) }
        return working + broken
    }

    companion object {
        const val TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
