package id.temankontrol.app.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import id.temankontrol.app.MainActivity
import id.temankontrol.app.core.common.AppLogger
import id.temankontrol.app.data.local.AppDatabase
import id.temankontrol.app.data.local.entity.ReminderLogEntity
import id.temankontrol.app.data.local.mapper.toDomain
import id.temankontrol.app.domain.reminder.ReminderKind
import id.temankontrol.app.domain.reminder.ReminderPlanner
import id.temankontrol.app.domain.reminder.ScheduledReminder
import id.temankontrol.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val preferencesRepository: PreferencesRepository
) {
    companion object {
        const val ACTION_FIRE_REMINDER = "id.temankontrol.app.ACTION_FIRE_REMINDER"
        const val ACTION_DISMISS_ALARM = "id.temankontrol.app.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "id.temankontrol.app.ACTION_SNOOZE_ALARM"
        private const val REQUEST_CODE_NEXT_ALARM = 4001
        private const val REQUEST_CODE_SNOOZE_ALARM = 4002
        private const val REQUEST_CODE_SHOW_INFO = 4003
    }

    suspend fun rescheduleAll() = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now()
            val cutoff = now.minus(30, ChronoUnit.DAYS).toEpochMilli()
            database.reminderLogDao().deleteOlderThan(cutoff)

            val consultations = database.consultationDao().getAll().map { it.toDomain() }
            val questions = database.questionDao().getAll()
            val questionCounts = questions.groupingBy { it.consultationId }.eachCount()
            val followUps = database.followUpDao().getAll().map { it.toDomain() }
            val settings = preferencesRepository.reminderSettings.first()
            val lastBackupAt = preferencesRepository.lastBackupAt.first()
            val dataChanged = preferencesRepository.dataChangedSinceBackup.first()

            val reminders = ReminderPlanner.computeUpcomingReminders(
                consultations = consultations,
                questionCounts = questionCounts,
                openFollowUps = followUps,
                settings = settings,
                lastBackupAt = lastBackupAt,
                dataChangedSinceBackup = dataChanged,
                firedKeys = emptySet(),
                now = now
            )

            // Filter already-logged keys
            val next = reminders.firstOrNull { !database.reminderLogDao().exists(it.key) }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_FIRE_REMINDER
                if (next != null) {
                    putExtra(ReminderReceiver.EXTRA_KEY, next.key)
                    putExtra(ReminderReceiver.EXTRA_KIND, next.kind.name)
                    putExtra(ReminderReceiver.EXTRA_REF_ID, next.refId)
                    putExtra(ReminderReceiver.EXTRA_TITLE, next.titleIfUnlocked)
                    putExtra(ReminderReceiver.EXTRA_TRIGGER_MS, next.triggerAt.toEpochMilli())
                }
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_NEXT_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            if (next != null) {
                scheduleWithAlarmClockOrFallback(
                    alarmManager = alarmManager,
                    triggerAtMillis = next.triggerAt.toEpochMilli(),
                    pendingIntent = pendingIntent,
                    useAlarmClock = next.kind.isVisitAlarmCandidate() && settings.alarmSoundModeEnabled
                )
                AppLogger.d("Reminder", "Scheduled next kind=${next.kind} at=${next.triggerAt}")
            }
        } catch (e: Exception) {
            AppLogger.w("Reminder", "Reschedule failed", e)
        }
    }

    /**
     * Schedules an exact +10 minute snooze alarm via AlarmManager.setAlarmClock (FR-069, AC-47).
     */
    suspend fun scheduleSnooze(
        refId: String?,
        title: String?,
        snoozeMinutes: Int = 10
    ) = withContext(Dispatchers.IO) {
        try {
            val triggerAt = Instant.now().plus(snoozeMinutes.toLong(), ChronoUnit.MINUTES)
            val triggerMs = triggerAt.toEpochMilli()
            val key = "${ReminderKind.VISIT_SNOOZED.name}:${refId ?: "visit"}:$triggerMs"

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_FIRE_REMINDER
                putExtra(ReminderReceiver.EXTRA_KEY, key)
                putExtra(ReminderReceiver.EXTRA_KIND, ReminderKind.VISIT_SNOOZED.name)
                putExtra(ReminderReceiver.EXTRA_REF_ID, refId)
                putExtra(ReminderReceiver.EXTRA_TITLE, title)
                putExtra(ReminderReceiver.EXTRA_TRIGGER_MS, triggerMs)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_SNOOZE_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleWithAlarmClockOrFallback(
                alarmManager = alarmManager,
                triggerAtMillis = triggerMs,
                pendingIntent = pendingIntent,
                useAlarmClock = true
            )
            AppLogger.d("Reminder", "Scheduled snooze (+${snoozeMinutes}m) at=$triggerAt")
        } catch (e: Exception) {
            AppLogger.w("Reminder", "Snooze schedule failed", e)
        }
    }

    /**
     * Immediately triggers a ringing test alarm + full-screen SCR-20 alert (FR-071, AC-48).
     */
    suspend fun triggerTestAlarmNow() = withContext(Dispatchers.Main) {
        val now = Instant.now()
        val settings = preferencesRepository.reminderSettings.first().copy(alarmSoundModeEnabled = true)
        val testReminder = ScheduledReminder(
            key = "TEST_ALARM:${now.toEpochMilli()}",
            kind = ReminderKind.VISIT_AT_TIME,
            refId = null,
            titleIfUnlocked = "Uji Coba Alarm Kunjungan",
            triggerAt = now
        )
        ReminderNotificationHelper.postReminderNotification(
            context = context,
            reminder = testReminder,
            settings = settings,
            isTestAlarm = true
        )
    }

    suspend fun handleAlarmFired(
        key: String,
        kindName: String,
        refId: String?,
        title: String?,
        triggerAtMs: Long
    ) = withContext(Dispatchers.IO) {
        try {
            val logDao = database.reminderLogDao()
            if (!logDao.exists(key)) {
                val kind = runCatching {
                    ReminderKind.valueOf(kindName)
                }.getOrNull()
                if (kind != null) {
                    val settings = preferencesRepository.reminderSettings.first()
                    val reminder = ScheduledReminder(
                        key = key,
                        kind = kind,
                        refId = refId,
                        titleIfUnlocked = title,
                        triggerAt = Instant.ofEpochMilli(triggerAtMs)
                    )
                    val posted = ReminderNotificationHelper.postReminderNotification(context, reminder, settings)
                    // FR-072: Only record reminder_log if the alarm/notification was actually posted
                    if (posted) {
                        logDao.insert(ReminderLogEntity(key = key, firedAt = Instant.now().toEpochMilli()))
                    }
                }
            }
        } finally {
            rescheduleAll()
        }
    }

    private fun scheduleWithAlarmClockOrFallback(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        useAlarmClock: Boolean
    ) {
        if (useAlarmClock) {
            val showIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_SHOW_INFO,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val scheduledExact = runCatching {
                val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            }.isSuccess
            if (scheduledExact) return
        }

        val scheduledIdleExact = runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }.isSuccess

        if (!scheduledIdleExact) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
