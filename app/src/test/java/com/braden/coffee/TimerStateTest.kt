package com.braden.coffee

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {

    @Test
    fun startStopReset() {
        val idle = TimerState()
        assertFalse(idle.isRunning)
        assertFalse(idle.isStopped)
        assertEquals(0L, idle.elapsed(5_000))

        val running = idle.tap(now = 1_000)
        assertTrue(running.isRunning)
        assertEquals(2_500L, running.elapsed(now = 3_500))

        val stopped = running.tap(now = 4_000)
        assertTrue(stopped.isStopped)
        assertEquals(3_000L, stopped.elapsed(now = 99_000)) // frozen

        val reset = stopped.tap(now = 5_000)
        assertEquals(TimerState(), reset)
    }

    @Test
    fun formatting() {
        assertEquals("0:00", formatElapsed(0))
        assertEquals("0:59", formatElapsed(59_999))
        assertEquals("3:07", formatElapsed(187_000))
        assertEquals("1:02:03", formatElapsed(3_723_000))
    }
}
