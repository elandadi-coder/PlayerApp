package com.foot22.samedi.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class SaturdaySchedulerTest {

    @Test
    fun `picks upcoming saturday at 22h00 from wednesday`() {
        // 2026-11-11 is a Wednesday
        val wednesday = LocalDateTime.of(2026, 11, 11, 16, 30)
        val next = SaturdayScheduler.nextMatchDateTime(wednesday, DayOfWeek.SATURDAY, 22, 0)
        assertEquals(LocalDateTime.of(2026, 11, 14, 22, 0), next)
    }

    @Test
    fun `keeps today when it is saturday before 22h00`() {
        val saturdayAfternoon = LocalDateTime.of(2026, 11, 14, 15, 0)
        val next = SaturdayScheduler.nextMatchDateTime(saturdayAfternoon, DayOfWeek.SATURDAY, 22, 0)
        assertEquals(LocalDateTime.of(2026, 11, 14, 22, 0), next)
    }

    @Test
    fun `advances to next saturday when past sunday`() {
        val sunday = LocalDateTime.of(2026, 11, 15, 10, 0)
        val next = SaturdayScheduler.nextMatchDateTime(sunday, DayOfWeek.SATURDAY, 22, 0)
        assertEquals(LocalDateTime.of(2026, 11, 21, 22, 0), next)
    }
}
