package id.temankontrol.app.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import id.temankontrol.app.core.common.AppLogger
import id.temankontrol.app.data.local.AppDatabase
import id.temankontrol.app.data.local.entity.ReminderLogEntity
import id.temankontrol.app.data.local.mapper.toDomain
import id.temankontrol.app.domain.reminder.ReminderPlanner
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
        private const val REQUEST_CODE_NEXT_ALARM = 4001
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
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    next.triggerAt.toEpochMilli(),
                    pendingIntent
                )
                AppLogger.d("Reminder", "Scheduled next kind=${next.kind} at=${next.triggerAt}")
            }
        } catch (e: Exception) {
            AppLogger.w("Reminder", "Reschedule failed", e)
        }
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
                    id.temankontrol.app.domain.reminder.ReminderKind.valueOf(kindName)
                }.getOrNull()
                if (kind != null) {
                    val settings = preferencesRepository.reminderSettings.first()
                    val reminder = id.temankontrol.app.domain.reminder.ScheduledReminder(
                        key = key,
                        kind = kind,
                        refId = refId,
                        titleIfUnlocked = title,
                        triggerAt = Instant.ofEpochMilli(triggerAtMs)
                    )
                    ReminderNotificationHelper.postReminderNotification(context, reminder, settings)
                    logDao.insert(ReminderLogEntity(key = key, firedAt = Instant.now().toEpochMilli()))
                }
            }
        } finally {
            rescheduleAll()
        }
    }
}
