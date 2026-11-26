package id.temankontrol.app.domain.reminder

import id.temankontrol.app.domain.model.Consultation
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.ConsultationStatus
import id.temankontrol.app.domain.model.FollowUpAction
import id.temankontrol.app.domain.model.ReminderSettings
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPlannerTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    @Test
    fun scheduledConsultation_producesAllV3VisitAndPrepareReminders() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 11, 17), LocalTime.of(10, 0), zone).toInstant()
        val appt = ZonedDateTime.of(LocalDate.of(2026, 11, 20), LocalTime.of(14, 0), zone).toInstant()

        val c = Consultation(
            id = ConsultationId("c-1"),
            title = "Kontrol Jantung",
            appointmentAt = appt,
            reason = "Evaluasi rutin",
            facilityName = null,
            providerName = null,
            status = ConsultationStatus.SCHEDULED,
            createdAt = now,
            updatedAt = now
        )

        val reminders = ReminderPlanner.computeUpcomingReminders(
            consultations = listOf(c),
            questionCounts = mapOf("c-1" to 0),
            openFollowUps = emptyList(),
            settings = ReminderSettings(backupReminderEnabled = false),
            lastBackupAt = null,
            dataChangedSinceBackup = false,
            firedKeys = emptySet(),
            now = now,
            zoneId = zone
        )

        // PREPARE_QUESTIONS (H-2), VISIT_DAY_BEFORE (H-1), VISIT_TWO_HOURS_BEFORE (-2h),
        // VISIT_THIRTY_MINUTES_BEFORE (-30m), VISIT_AT_TIME (0m)
        assertEquals(5, reminders.size)
        assertTrue(reminders.any { it.kind == ReminderKind.PREPARE_QUESTIONS })
        assertTrue(reminders.any { it.kind == ReminderKind.VISIT_DAY_BEFORE })
        assertTrue(reminders.any { it.kind == ReminderKind.VISIT_TWO_HOURS_BEFORE })
        assertTrue(reminders.any { it.kind == ReminderKind.VISIT_THIRTY_MINUTES_BEFORE })
        assertTrue(reminders.any { it.kind == ReminderKind.VISIT_AT_TIME })
        assertEquals(ReminderKind.PREPARE_QUESTIONS, reminders.first().kind)
        assertEquals(ReminderKind.VISIT_AT_TIME, reminders.last().kind)
    }

    @Test
    fun prepareQuestions_skippedWhenConsultationAlreadyHasQuestions() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 11, 17), LocalTime.of(10, 0), zone).toInstant()
        val appt = ZonedDateTime.of(LocalDate.of(2026, 11, 20), LocalTime.of(14, 0), zone).toInstant()

        val c = Consultation(
            id = ConsultationId("c-2"),
            title = "Kontrol Mata",
            appointmentAt = appt,
            reason = "Cek mata",
            facilityName = null,
            providerName = null,
            status = ConsultationStatus.SCHEDULED,
            createdAt = now,
            updatedAt = now
        )

        val reminders = ReminderPlanner.computeUpcomingReminders(
            consultations = listOf(c),
            questionCounts = mapOf("c-2" to 2),
            openFollowUps = emptyList(),
            settings = ReminderSettings(backupReminderEnabled = false),
            lastBackupAt = null,
            dataChangedSinceBackup = false,
            firedKeys = emptySet(),
            now = now,
            zoneId = zone
        )

        assertFalse(reminders.any { it.kind == ReminderKind.PREPARE_QUESTIONS })
        assertEquals(4, reminders.size)
    }

    @Test
    fun shortTermAppointment_fiveMinutesAhead_triggersExactTimeAndCustomOffset() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 11, 26), LocalTime.of(9, 0), zone).toInstant()
        val appt = now.plusSeconds(20 * 60) // 20 minutes in the future

        val c = Consultation(
            id = ConsultationId("c-short"),
            title = "Tes Alarm Cepat",
            appointmentAt = appt,
            reason = "Uji alarm",
            facilityName = null,
            providerName = null,
            status = ConsultationStatus.SCHEDULED,
            createdAt = now,
            updatedAt = now
        )

        val reminders = ReminderPlanner.computeUpcomingReminders(
            consultations = listOf(c),
            questionCounts = mapOf("c-short" to 1),
            openFollowUps = emptyList(),
            settings = ReminderSettings(
                backupReminderEnabled = false,
                customLeadMinutes = 15
            ),
            lastBackupAt = null,
            dataChangedSinceBackup = false,
            firedKeys = emptySet(),
            now = now,
            zoneId = zone
        )

        // Should include 15-min custom offset and exact appointment time (plus missed-tolerance catchups)
        assertTrue(reminders.any { it.kind == ReminderKind.VISIT_CUSTOM_OFFSET })
        assertTrue(reminders.any { it.kind == ReminderKind.VISIT_AT_TIME })
        val exactReminder = reminders.first { it.kind == ReminderKind.VISIT_AT_TIME }
        assertEquals(appt, exactReminder.triggerAt)
    }

    @Test
    fun cancelledConsultation_producesNoReminders() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 11, 17), LocalTime.of(10, 0), zone).toInstant()
        val appt = ZonedDateTime.of(LocalDate.of(2026, 11, 20), LocalTime.of(14, 0), zone).toInstant()

        val c = Consultation(
            id = ConsultationId("c-3"),
            title = "Batal",
            appointmentAt = appt,
            reason = "Batal",
            facilityName = null,
            providerName = null,
            status = ConsultationStatus.CANCELLED,
            createdAt = now,
            updatedAt = now
        )

        val reminders = ReminderPlanner.computeUpcomingReminders(
            consultations = listOf(c),
            questionCounts = emptyMap(),
            openFollowUps = emptyList(),
            settings = ReminderSettings(backupReminderEnabled = false),
            lastBackupAt = null,
            dataChangedSinceBackup = true,
            firedKeys = emptySet(),
            now = now,
            zoneId = zone
        )

        assertTrue(reminders.isEmpty())
    }

    @Test
    fun backupReminder_scheduledAfter30DaysWhenDataChanged() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 11, 17), LocalTime.of(10, 0), zone).toInstant()
        val lastBackup = ZonedDateTime.of(LocalDate.of(2026, 11, 10), LocalTime.of(10, 0), zone).toInstant()
        val c = Consultation(
            id = ConsultationId("c-4"),
            title = "Kontrol",
            appointmentAt = now.minusSeconds(3600),
            reason = "Kontrol",
            facilityName = null,
            providerName = null,
            status = ConsultationStatus.COMPLETED,
            createdAt = now,
            updatedAt = now
        )

        val reminders = ReminderPlanner.computeUpcomingReminders(
            consultations = listOf(c),
            questionCounts = emptyMap(),
            openFollowUps = emptyList(),
            settings = ReminderSettings(backupReminderEnabled = true),
            lastBackupAt = lastBackup,
            dataChangedSinceBackup = true,
            firedKeys = emptySet(),
            now = now,
            zoneId = zone
        )

        assertEquals(1, reminders.size)
        assertEquals(ReminderKind.BACKUP_PERIODIC, reminders.first().kind)
    }

    @Test
    fun quietHours_shiftsNightCandidateToMorning() {
        val appt = ZonedDateTime.of(LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), zone).toInstant()
        val nightCandidate = ZonedDateTime.of(LocalDate.of(2026, 11, 20), LocalTime.of(3, 0), zone).toInstant()

        val adjusted = ReminderPlanner.adjustQuietHours(nightCandidate, appt, zone)
        assertNotNull(adjusted)
        assertEquals(6, adjusted!!.atZone(zone).hour)
    }

    @Test
    fun followUpDueAndFiredDeduplication_worksProperly() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 11, 17), LocalTime.of(7, 0), zone).toInstant()
        val dueDate = LocalDate.of(2026, 11, 17)
        val action = FollowUpAction(
            id = "f-1",
            consultationId = ConsultationId("c-1"),
            text = "Cek darah",
            dueDate = dueDate,
            isCompleted = false,
            completedAt = null,
            createdAt = now,
            updatedAt = now
        )

        val firstPass = ReminderPlanner.computeUpcomingReminders(
            consultations = emptyList(),
            questionCounts = emptyMap(),
            openFollowUps = listOf(action),
            settings = ReminderSettings(followUpReminderHour = 8, backupReminderEnabled = false),
            lastBackupAt = null,
            dataChangedSinceBackup = false,
            firedKeys = emptySet(),
            now = now,
            zoneId = zone
        )
        assertEquals(2, firstPass.size)

        val firedKey = firstPass.first().key
        val secondPass = ReminderPlanner.computeUpcomingReminders(
            consultations = emptyList(),
            questionCounts = emptyMap(),
            openFollowUps = listOf(action),
            settings = ReminderSettings(followUpReminderHour = 8, backupReminderEnabled = false),
            lastBackupAt = null,
            dataChangedSinceBackup = false,
            firedKeys = setOf(firedKey),
            now = now,
            zoneId = zone
        )
        assertEquals(1, secondPass.size)
        assertEquals(ReminderKind.FOLLOW_UP_OVERDUE, secondPass.first().kind)
    }
}
