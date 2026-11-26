package id.temankontrol.app.data.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.temankontrol.app.MainActivity
import id.temankontrol.app.R
import id.temankontrol.app.core.common.AppLogger
import id.temankontrol.app.domain.model.ReminderSettings
import id.temankontrol.app.domain.reminder.ReminderKind
import id.temankontrol.app.domain.reminder.ScheduledReminder
import id.temankontrol.app.feature.reminder.AlarmAlertActivity

object ReminderNotificationHelper {

    const val CHANNEL_VISIT_ALARM = "visit_alarm_v3"
    const val CHANNEL_VISIT = "visit_reminders"
    const val CHANNEL_FOLLOWUP = "followup_reminders"
    const val CHANNEL_BACKUP = "backup_reminders"

    const val EXTRA_NAV_CONSULTATION_ID = "extra_nav_consultation_id"
    const val EXTRA_NAV_OPEN_BACKUP = "extra_nav_open_backup"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmAudioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channels = listOf(
            NotificationChannel(
                CHANNEL_VISIT_ALARM,
                "Alarm Jadwal Kunjungan (Berbunyi Nyaring)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm berbunyi nyaring untuk jadwal kunjungan konsultasi"
                setSound(alarmSoundUri, alarmAudioAttrs)
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 800L, 400L, 800L, 400L, 1000L)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
            NotificationChannel(
                CHANNEL_VISIT,
                "Pengingat Kunjungan & Persiapan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat jadwal kunjungan konsultasi dan persiapan pertanyaan"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
            NotificationChannel(
                CHANNEL_FOLLOWUP,
                "Pengingat Tindak Lanjut",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat tenggat tindak lanjut konsultasi"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
            NotificationChannel(
                CHANNEL_BACKUP,
                "Pengingat Cadangan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Pengingat berkala untuk membuat cadangan .tkbackup"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
        )
        manager.createNotificationChannels(channels)
    }

    /**
     * Posts the notification/alarm and returns `true` if the alarm or notification was actually triggered.
     * Returns `false` if blocked by missing POST_NOTIFICATIONS permission when not using alarm sound mode.
     */
    fun postReminderNotification(
        context: Context,
        reminder: ScheduledReminder,
        settings: ReminderSettings,
        isTestAlarm: Boolean = false
    ): Boolean {
        ensureChannels(context)

        val isAlarmMode = reminder.kind.isVisitAlarmCandidate() && settings.alarmSoundModeEnabled

        val genericBody = when (reminder.kind) {
            ReminderKind.VISIT_DAY_BEFORE -> "Ada jadwal kunjungan besok. Pastikan pertanyaan Anda sudah siap."
            ReminderKind.VISIT_TWO_HOURS_BEFORE -> "Jadwal kunjungan sekitar 2 jam lagi."
            ReminderKind.VISIT_THIRTY_MINUTES_BEFORE -> "Jadwal kunjungan sekitar 30 menit lagi. Bersiaplah berangkat."
            ReminderKind.VISIT_CUSTOM_OFFSET -> "Pengingat jadwal kunjungan mendatang."
            ReminderKind.VISIT_AT_TIME -> "Waktunya jadwal kunjungan Anda sekarang. Buka Mode Konsultasi saat bertemu tenaga kesehatan."
            ReminderKind.VISIT_SNOOZED -> "Pengingat kunjungan (setelah ditunda 10 menit)."
            ReminderKind.PREPARE_QUESTIONS -> "Jangan lupa siapkan pertanyaan untuk kunjungan mendatang."
            ReminderKind.FOLLOW_UP_DUE -> "Ada tindak lanjut yang jatuh tempo hari ini."
            ReminderKind.FOLLOW_UP_OVERDUE -> "Ada tindak lanjut yang melewati tenggat waktu."
            ReminderKind.BACKUP_PERIODIC -> "Sudah 30 hari sejak cadangan terakhir. Buat cadangan baru?"
        }

        val displayTitle = if (settings.showTitleOnLockScreen && !reminder.titleIfUnlocked.isNullOrBlank()) {
            reminder.titleIfUnlocked
        } else {
            "Teman Kontrol"
        }

        val unlockedBody = if (!reminder.titleIfUnlocked.isNullOrBlank()) {
            "${reminder.titleIfUnlocked}: $genericBody"
        } else {
            genericBody
        }

        val payload = ActiveAlarmPayload(
            key = reminder.key,
            kindName = reminder.kind.name,
            refId = reminder.refId,
            title = if (settings.showTitleOnLockScreen) reminder.titleIfUnlocked else "Jadwal Kunjungan",
            message = if (settings.showTitleOnLockScreen) unlockedBody else genericBody,
            triggerAtMs = reminder.triggerAt.toEpochMilli(),
            isTestAlarm = isTestAlarm
        )

        // If Alarm Sound Mode is active for a visit reminder, ALWAYS start ringing + launch full-screen alert
        if (isAlarmMode) {
            AlarmSoundPlayer.startRinging(context, payload)
            runCatching {
                context.startActivity(AlarmAlertActivity.createIntent(context, payload))
            }.onFailure {
                AppLogger.d("Reminder", "FullScreen intent will be delivered via notification")
            }
        }

        val hasNotifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasNotifPermission) {
            // If alarm mode started ringing even without notification permission, return true; otherwise false
            return isAlarmMode
        }

        val channelId = when {
            isAlarmMode -> CHANNEL_VISIT_ALARM
            reminder.kind.isVisitAlarmCandidate() || reminder.kind == ReminderKind.PREPARE_QUESTIONS -> CHANNEL_VISIT
            reminder.kind == ReminderKind.FOLLOW_UP_DUE || reminder.kind == ReminderKind.FOLLOW_UP_OVERDUE -> CHANNEL_FOLLOWUP
            ReminderKind.BACKUP_PERIODIC == reminder.kind -> CHANNEL_BACKUP
            else -> CHANNEL_VISIT
        }

        val tapIntent = if (isAlarmMode) {
            AlarmAlertActivity.createIntent(context, payload)
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (reminder.kind == ReminderKind.BACKUP_PERIODIC) {
                    putExtra(EXTRA_NAV_OPEN_BACKUP, true)
                } else if (!reminder.refId.isNullOrBlank()) {
                    putExtra(EXTRA_NAV_CONSULTATION_ID, reminder.refId)
                }
            }
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminder.key.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Teman Kontrol")
            .setContentText("Alarm & Pengingat Teman Kontrol")
            .build()

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(displayTitle)
            .setContentText(unlockedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(unlockedBody))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setAutoCancel(!isAlarmMode)
            .setContentIntent(contentPendingIntent)

        if (isAlarmMode) {
            val fullScreenIntent = AlarmAlertActivity.createIntent(context, payload)
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                reminder.key.hashCode() + 1,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderScheduler.ACTION_DISMISS_ALARM
                putExtra(ReminderReceiver.EXTRA_KEY, reminder.key)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.key.hashCode() + 2,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderScheduler.ACTION_SNOOZE_ALARM
                putExtra(ReminderReceiver.EXTRA_KEY, reminder.key)
                putExtra(ReminderReceiver.EXTRA_REF_ID, reminder.refId)
                putExtra(ReminderReceiver.EXTRA_TITLE, reminder.titleIfUnlocked)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.key.hashCode() + 3,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .addAction(0, "Matikan Alarm", dismissPendingIntent)
                .addAction(0, "Tunda 10 Menit", snoozePendingIntent)
        } else {
            builder.setPriority(
                if (channelId == CHANNEL_VISIT) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
        }

        NotificationManagerCompat.from(context).notify(reminder.key.hashCode(), builder.build())
        return true
    }
}
