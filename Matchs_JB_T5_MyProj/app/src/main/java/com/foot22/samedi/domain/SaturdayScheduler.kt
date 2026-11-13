package com.foot22.samedi.domain

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object SaturdayScheduler {

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val frenchLocale = Locale.FRENCH

    /**
     * Computes the upcoming match date-time (default: Saturday at 22:00).
     * If today matches [dayOfWeek] and kickoff was less than 2 hours ago, keeps today's slot
     * so the admin can still manage teams or close the match that evening.
     */
    fun nextMatchDateTime(
        from: LocalDateTime = LocalDateTime.now(),
        dayOfWeek: DayOfWeek = DayOfWeek.SATURDAY,
        hour: Int = 22,
        minute: Int = 0
    ): LocalDateTime {
        val todayAtTarget = from.toLocalDate().atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        if (from.dayOfWeek == dayOfWeek && !from.isAfter(todayAtTarget.plusHours(2))) {
            return todayAtTarget
        }
        val nextDate = from.toLocalDate().with(TemporalAdjusters.next(dayOfWeek))
        return nextDate.atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    /** Advances strictly one week after [referenceIso] (used after closing a match). */
    fun nextWeekFrom(
        referenceIso: String,
        dayOfWeek: DayOfWeek = DayOfWeek.SATURDAY,
        hour: Int = 22,
        minute: Int = 0
    ): LocalDateTime {
        val ref = parseIso(referenceIso) ?: return nextMatchDateTime(
            from = LocalDateTime.now(),
            dayOfWeek = dayOfWeek,
            hour = hour,
            minute = minute
        )
        val candidate = ref.toLocalDate()
            .with(TemporalAdjusters.next(dayOfWeek))
            .atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        val now = LocalDateTime.now()
        return if (candidate.isAfter(now.minusHours(2))) {
            candidate
        } else {
            nextMatchDateTime(now, dayOfWeek, hour, minute)
        }
    }

    fun toIso(dateTime: LocalDateTime): String = dateTime.truncatedTo(ChronoUnit.MINUTES).format(isoFormatter)

    fun parseIso(iso: String): LocalDateTime? =
        runCatching { LocalDateTime.parse(iso, isoFormatter) }.getOrNull()

    fun formatFrenchDateTime(iso: String): String {
        val dt = parseIso(iso) ?: return iso
        val formatter = DateTimeFormatter.ofPattern("EEEE d MMM • HH'h'mm", frenchLocale)
        return dt.format(formatter).replaceFirstChar { it.titlecase(frenchLocale) }
    }

    fun formatShortDate(iso: String): String {
        val dt = parseIso(iso) ?: return iso
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", frenchLocale))
    }

    fun countdownLabel(iso: String, now: LocalDateTime = LocalDateTime.now()): String {
        val target = parseIso(iso) ?: return ""
        val minutes = ChronoUnit.MINUTES.between(now, target)
        return when {
            minutes in -120..0 -> "Match en cours / ce soir !"
            minutes < -120 -> "À clôturer"
            minutes < 60 -> "Dans $minutes min"
            minutes < 24 * 60 -> {
                val hours = minutes / 60
                val remMin = minutes % 60
                if (remMin == 0L) "Dans ${hours}h" else "Dans ${hours}h${"%02d".format(remMin)}"
            }
            else -> {
                val days = minutes / (24 * 60)
                val hours = (minutes % (24 * 60)) / 60
                if (hours == 0L) "Dans ${days}j" else "Dans ${days}j ${hours}h"
            }
        }
    }
}
