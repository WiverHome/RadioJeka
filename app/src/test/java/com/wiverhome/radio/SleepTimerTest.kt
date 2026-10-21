package com.wiverhome.radio

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerTest {
    @Test
    fun deadlineIsMinutesFromNow() {
        assertEquals(1_000L + 30 * 60_000L, SleepTimer.deadline(now = 1_000L, minutes = 30))
    }

    @Test
    fun minutesLeftRoundsUp() {
        val deadline = SleepTimer.deadline(0, 30)
        assertEquals(30, SleepTimer.minutesLeft(deadline, now = 0))
        assertEquals(30, SleepTimer.minutesLeft(deadline, now = 1))
        assertEquals(1, SleepTimer.minutesLeft(deadline, now = deadline - 10_000))
    }

    @Test
    fun minutesLeftIsZeroAfterDeadline() {
        assertEquals(0, SleepTimer.minutesLeft(deadline = 100, now = 100))
        assertEquals(0, SleepTimer.minutesLeft(deadline = 100, now = 5_000))
    }

    @Test
    fun durationText() {
        assertEquals("15 мин", SleepTimer.duration(15))
        assertEquals("1 ч", SleepTimer.duration(60))
        assertEquals("1 ч 30 мин", SleepTimer.duration(90))
        assertEquals("2 ч", SleepTimer.duration(120))
    }

    @Test
    fun labelShowsRemainingTime() {
        val deadline = SleepTimer.deadline(0, 90)
        assertEquals("Выключится через 1 ч 30 мин", SleepTimer.label(deadline, now = 0))
        assertEquals("Выключится через 25 мин", SleepTimer.label(deadline, now = 65 * 60_000L))
    }
}
