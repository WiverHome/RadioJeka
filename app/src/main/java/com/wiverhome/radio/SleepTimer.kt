package com.wiverhome.radio

/** Таймер сна: через сколько минут остановить радио. Время — по часам SystemClock.elapsedRealtime(). */
object SleepTimer {
    /** Варианты в меню таймера, в минутах. */
    val OPTIONS = listOf(15, 30, 45, 60, 90)

    fun deadline(now: Long, minutes: Int): Long = now + minutes * 60_000L

    /** Сколько целых минут осталось, с округлением вверх: за 10 секунд до конца это ещё «1 мин». */
    fun minutesLeft(deadline: Long, now: Long): Int {
        val left = deadline - now
        return if (left <= 0) 0 else ((left + 59_999) / 60_000).toInt()
    }

    /** «25 мин», «1 ч», «1 ч 30 мин». */
    fun duration(minutes: Int): String = when {
        minutes < 60 -> "$minutes мин"
        minutes % 60 == 0 -> "${minutes / 60} ч"
        else -> "${minutes / 60} ч ${minutes % 60} мин"
    }

    /** «Выключится через 1 ч 5 мин». */
    fun label(deadline: Long, now: Long): String = "Выключится через ${duration(minutesLeft(deadline, now))}"
}
