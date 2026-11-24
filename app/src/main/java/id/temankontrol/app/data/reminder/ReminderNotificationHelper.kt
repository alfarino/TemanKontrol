package id.temankontrol.app.data.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.temankontrol.app.MainActivity
import id.temankontrol.app.R
import id.temankontrol.app.domain.model.ReminderSettings
import id.temankontrol.app.domain.reminder.ReminderKind
import id.temankontrol.app.domain.reminder.ScheduledReminder

object ReminderNotificationHelper {

    const val CHANNEL_VISIT = "visit_reminders"
    const val CHANNEL_FOLLOWUP = "followup_reminders"
    const val CHANNEL_BACKUP = "backup_reminders"

    const val EXTRA_NAV_CONSULTATION_ID = "extra_nav_consultation_id"
    const val EXTRA_NAV_OPEN_BACKUP = "extra_nav_open_backup"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                CHANNEL_VISIT,
                "Pengingat Kunjungan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat jadwal kunjungan konsultasi dan persiapan pertanyaan"
            },
            NotificationChannel(
                CHANNEL_FOLLOWUP,
                "Pengingat Tindak Lanjut",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat tenggat tindak lanjut konsultasi"
            },
            NotificationChannel(
                CHANNEL_BACKUP,
                "Pengingat Cadangan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Pengingat berkala untuk membuat cadangan .tkbackup"
            }
        )
        manager.createNotificationChannels(channels)
    }

    fun postReminderNotification(
        context: Context,
        reminder: ScheduledReminder,
        settings: ReminderSettings
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannels(context)

        val channelId = when (reminder.kind) {
            ReminderKind.VISIT_DAY_BEFORE,
            ReminderKind.VISIT_TWO_HOURS_BEFORE,
            ReminderKind.PREPARE_QUESTIONS -> CHANNEL_VISIT
            ReminderKind.FOLLOW_UP_DUE,
            ReminderKind.FOLLOW_UP_OVERDUE -> CHANNEL_FOLLOWUP
            ReminderKind.BACKUP_PERIODIC -> CHANNEL_BACKUP
        }

        val genericBody = when (reminder.kind) {
            ReminderKind.VISIT_DAY_BEFORE -> "Ada jadwal kunjungan besok."
            ReminderKind.VISIT_TWO_HOURS_BEFORE -> "Jadwal kunjungan sekitar 2 jam lagi."
            ReminderKind.PREPARE_QUESTIONS -> "Jangan lupa siapkan pertanyaan untuk kunjungan mendatang."
            ReminderKind.FOLLOW_UP_DUE -> "Ada tindak lanjut yang jatuh tempo hari ini."
            ReminderKind.FOLLOW_UP_OVERDUE -> "Ada tindak lanjut yang melewati tenggat waktu."
            ReminderKind.BACKUP_PERIODIC -> "Sudah 30 hari sejak cadangan terakhir. Buat cadangan baru?"
        }

        val unlockedBody = if (settings.showTitleOnLockScreen && !reminder.titleIfUnlocked.isNullOrBlank()) {
            "${reminder.titleIfUnlocked}: $genericBody"
        } else {
            genericBody
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (reminder.kind == ReminderKind.BACKUP_PERIODIC) {
                putExtra(EXTRA_NAV_OPEN_BACKUP, true)
            } else if (!reminder.refId.isNullOrBlank()) {
                putExtra(EXTRA_NAV_CONSULTATION_ID, reminder.refId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.key.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Teman Kontrol")
            .setContentText("Pengingat Teman Kontrol")
            .build()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Teman Kontrol")
            .setContentText(unlockedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(unlockedBody))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(
                if (channelId == CHANNEL_VISIT) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .build()

        NotificationManagerCompat.from(context).notify(reminder.key.hashCode(), notification)
    }
}
