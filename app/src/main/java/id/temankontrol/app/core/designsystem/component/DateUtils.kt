package id.temankontrol.app.core.designsystem.component

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {
    private val localeId = Locale("id", "ID")
    private val zoneId = ZoneId.systemDefault()

    private val longDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", localeId)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", localeId)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH.mm", localeId)

    fun formatLongDate(instant: Instant): String {
        val zonedDateTime = instant.atZone(zoneId)
        return longDateFormatter.format(zonedDateTime)
    }

    fun formatTime(instant: Instant): String {
        val zonedDateTime = instant.atZone(zoneId)
        return timeFormatter.format(zonedDateTime)
    }

    fun formatDateTime(instant: Instant): String {
        val zonedDateTime = instant.atZone(zoneId)
        return "${longDateFormatter.format(zonedDateTime)} • ${timeFormatter.format(zonedDateTime)}"
    }

    fun formatRelativeDateTime(instant: Instant): String {
        val date = instant.atZone(zoneId).toLocalDate()
        val today = LocalDate.now(zoneId)
        val timeStr = formatTime(instant)

        return when (ChronoUnit.DAYS.between(today, date)) {
            0L -> "Hari ini • $timeStr"
            1L -> "Besok • $timeStr"
            -1L -> "Kemarin • $timeStr"
            else -> formatDateTime(instant)
        }
    }

    fun formatLocalDate(date: LocalDate): String {
        return shortDateFormatter.format(date)
    }

    fun formatRelativeLocalDate(date: LocalDate): String {
        val today = LocalDate.now(zoneId)
        return when (ChronoUnit.DAYS.between(today, date)) {
            0L -> "Hari ini"
            1L -> "Besok"
            -1L -> "Kemarin"
            else -> shortDateFormatter.format(date)
        }
    }
}
