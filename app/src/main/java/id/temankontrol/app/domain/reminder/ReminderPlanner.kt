package id.temankontrol.app.domain.reminder

import id.temankontrol.app.domain.model.Consultation
import id.temankontrol.app.domain.model.ConsultationStatus
import id.temankontrol.app.domain.model.FollowUpAction
import id.temankontrol.app.domain.model.ReminderSettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

enum class ReminderKind {
    VISIT_DAY_BEFORE,
    VISIT_TWO_HOURS_BEFORE,
    PREPARE_QUESTIONS,
    FOLLOW_UP_DUE,
    FOLLOW_UP_OVERDUE,
    BACKUP_PERIODIC
}

data class ScheduledReminder(
    val key: String,
    val kind: ReminderKind,
    val refId: String?,
    val titleIfUnlocked: String?,
    val triggerAt: Instant
)

object ReminderPlanner {

    private const val MISSED_TOLERANCE_HOURS = 6L

    fun computeUpcomingReminders(
        consultations: List<Consultation>,
        questionCounts: Map<String, Int>,
        openFollowUps: List<FollowUpAction>,
        settings: ReminderSettings,
        lastBackupAt: Instant?,
        dataChangedSinceBackup: Boolean,
        firedKeys: Set<String>,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ScheduledReminder> {
        val out = mutableListOf<ScheduledReminder>()
        val toleranceCutoff = now.minus(MISSED_TOLERANCE_HOURS, ChronoUnit.HOURS)

        // R-A & R-B: Consultation reminders
        consultations
            .filter { it.status == ConsultationStatus.SCHEDULED }
            .forEach { c ->
                val apptZoned = c.appointmentAt.atZone(zoneId)
                val apptDate = apptZoned.toLocalDate()

                if (settings.visitReminderEnabled && c.appointmentAt.isAfter(now)) {
                    // H-1 at configured hour
                    val h1Raw = ZonedDateTime.of(
                        apptDate.minusDays(1),
                        LocalTime.of(settings.visitReminderHour.coerceIn(6, 21), 0),
                        zoneId
                    ).toInstant()
                    val h1 = adjustQuietHours(h1Raw, c.appointmentAt, zoneId)
                    if (h1 != null && h1.isAfter(toleranceCutoff) && h1.isBefore(c.appointmentAt)) {
                        val key = "${ReminderKind.VISIT_DAY_BEFORE.name}:${c.id.value}:${h1.toEpochMilli()}"
                        if (key !in firedKeys) {
                            out.add(
                                ScheduledReminder(
                                    key = key,
                                    kind = ReminderKind.VISIT_DAY_BEFORE,
                                    refId = c.id.value,
                                    titleIfUnlocked = c.title,
                                    triggerAt = if (h1.isBefore(now)) now.plusSeconds(10) else h1
                                )
                            )
                        }
                    }

                    // 2 hours before appointment
                    val twoHoursRaw = c.appointmentAt.minus(2, ChronoUnit.HOURS)
                    val twoHours = adjustQuietHours(twoHoursRaw, c.appointmentAt, zoneId)
                    if (twoHours != null && twoHours.isAfter(toleranceCutoff) && twoHours.isBefore(c.appointmentAt)) {
                        val key = "${ReminderKind.VISIT_TWO_HOURS_BEFORE.name}:${c.id.value}:${twoHours.toEpochMilli()}"
                        if (key !in firedKeys) {
                            out.add(
                                ScheduledReminder(
                                    key = key,
                                    kind = ReminderKind.VISIT_TWO_HOURS_BEFORE,
                                    refId = c.id.value,
                                    titleIfUnlocked = c.title,
                                    triggerAt = if (twoHours.isBefore(now)) now.plusSeconds(10) else twoHours
                                )
                            )
                        }
                    }
                }

                // R-B: Prepare questions (H-2 at 19:00, only if 0 questions)
                val qCount = questionCounts[c.id.value] ?: 0
                if (settings.prepareReminderEnabled && qCount == 0 && c.appointmentAt.isAfter(now)) {
                    val h2Raw = ZonedDateTime.of(
                        apptDate.minusDays(2),
                        LocalTime.of(19, 0),
                        zoneId
                    ).toInstant()
                    if (h2Raw.isAfter(now) && h2Raw.isBefore(c.appointmentAt)) {
                        val key = "${ReminderKind.PREPARE_QUESTIONS.name}:${c.id.value}:${h2Raw.toEpochMilli()}"
                        if (key !in firedKeys) {
                            out.add(
                                ScheduledReminder(
                                    key = key,
                                    kind = ReminderKind.PREPARE_QUESTIONS,
                                    refId = c.id.value,
                                    titleIfUnlocked = c.title,
                                    triggerAt = h2Raw
                                )
                            )
                        }
                    }
                }
            }

        // R-C: Follow-up due date & H+1 overdue (non-repeating)
        if (settings.followUpReminderEnabled) {
            val consultMap = consultations.associateBy { it.id.value }
            openFollowUps
                .filter { !it.isCompleted && it.dueDate != null }
                .forEach { action ->
                    val dueDate: LocalDate = action.dueDate!!
                    val hour = settings.followUpReminderHour.coerceIn(6, 21)
                    val title = consultMap[action.consultationId.value]?.title

                    val dueAt = ZonedDateTime.of(dueDate, LocalTime.of(hour, 0), zoneId).toInstant()
                    if (dueAt.isAfter(toleranceCutoff)) {
                        val key = "${ReminderKind.FOLLOW_UP_DUE.name}:${action.id}:${dueAt.toEpochMilli()}"
                        if (key !in firedKeys) {
                            out.add(
                                ScheduledReminder(
                                    key = key,
                                    kind = ReminderKind.FOLLOW_UP_DUE,
                                    refId = action.consultationId.value,
                                    titleIfUnlocked = title,
                                    triggerAt = if (dueAt.isBefore(now)) now.plusSeconds(15) else dueAt
                                )
                            )
                        }
                    }

                    val overdueAt = ZonedDateTime.of(dueDate.plusDays(1), LocalTime.of(hour, 0), zoneId).toInstant()
                    if (overdueAt.isAfter(toleranceCutoff)) {
                        val key = "${ReminderKind.FOLLOW_UP_OVERDUE.name}:${action.id}:${overdueAt.toEpochMilli()}"
                        if (key !in firedKeys) {
                            out.add(
                                ScheduledReminder(
                                    key = key,
                                    kind = ReminderKind.FOLLOW_UP_OVERDUE,
                                    refId = action.consultationId.value,
                                    titleIfUnlocked = title,
                                    triggerAt = if (overdueAt.isBefore(now)) now.plusSeconds(20) else overdueAt
                                )
                            )
                        }
                    }
                }
        }

        // R-D: Periodic backup reminder (every 30 days if data changed)
        if (settings.backupReminderEnabled && dataChangedSinceBackup && consultations.isNotEmpty()) {
            val baseInstant = lastBackupAt ?: now
            val rawTrigger = baseInstant.plus(30, ChronoUnit.DAYS)
            val scheduled = if (rawTrigger.isAfter(now)) {
                rawTrigger
            } else {
                // Due already -> schedule 1 hour from now (unless already fired today)
                val todayBucket = now.atZone(zoneId).toLocalDate().toEpochDay()
                val key = "${ReminderKind.BACKUP_PERIODIC.name}:backup:$todayBucket"
                if (key !in firedKeys) {
                    out.add(
                        ScheduledReminder(
                            key = key,
                            kind = ReminderKind.BACKUP_PERIODIC,
                            refId = null,
                            titleIfUnlocked = null,
                            triggerAt = now.plus(1, ChronoUnit.HOURS)
                        )
                    )
                }
                null
            }
            if (scheduled != null) {
                val key = "${ReminderKind.BACKUP_PERIODIC.name}:backup:${scheduled.epochSecond / 86400}"
                if (key !in firedKeys) {
                    out.add(
                        ScheduledReminder(
                            key = key,
                            kind = ReminderKind.BACKUP_PERIODIC,
                            refId = null,
                            titleIfUnlocked = null,
                            triggerAt = scheduled
                        )
                    )
                }
            }
        }

        return out.sortedBy { it.triggerAt }
    }

    /**
     * Jam hening 22.00–05.59 digeser ke 06.00 pagi hari tersebut/berikutnya bila masih sebelum upperBound.
     */
    internal fun adjustQuietHours(candidate: Instant, upperBound: Instant, zoneId: ZoneId): Instant? {
        val zoned = candidate.atZone(zoneId)
        val hour = zoned.hour
        val adjusted = when {
            hour >= 22 -> ZonedDateTime.of(zoned.toLocalDate().plusDays(1), LocalTime.of(6, 0), zoneId).toInstant()
            hour < 6 -> ZonedDateTime.of(zoned.toLocalDate(), LocalTime.of(6, 0), zoneId).toInstant()
            else -> candidate
        }
        return if (adjusted.isBefore(upperBound)) adjusted else null
    }
}
