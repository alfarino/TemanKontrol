package id.temankontrol.app.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    companion object {
        const val EXTRA_KEY = "extra_key"
        const val EXTRA_KIND = "extra_kind"
        const val EXTRA_REF_ID = "extra_ref_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TRIGGER_MS = "extra_trigger_ms"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderScheduler.ACTION_FIRE_REMINDER -> {
                        val key = intent.getStringExtra(EXTRA_KEY)
                        val kind = intent.getStringExtra(EXTRA_KIND)
                        val refId = intent.getStringExtra(EXTRA_REF_ID)
                        val title = intent.getStringExtra(EXTRA_TITLE)
                        val triggerMs = intent.getLongExtra(EXTRA_TRIGGER_MS, System.currentTimeMillis())
                        if (!key.isNullOrBlank() && !kind.isNullOrBlank()) {
                            reminderScheduler.handleAlarmFired(key, kind, refId, title, triggerMs)
                        } else {
                            reminderScheduler.rescheduleAll()
                        }
                    }

                    ReminderScheduler.ACTION_DISMISS_ALARM -> {
                        val key = intent.getStringExtra(EXTRA_KEY)
                        AlarmSoundPlayer.stopRinging(context)
                        if (!key.isNullOrBlank()) {
                            runCatching {
                                NotificationManagerCompat.from(context).cancel(key.hashCode())
                            }
                        }
                    }

                    ReminderScheduler.ACTION_SNOOZE_ALARM -> {
                        val key = intent.getStringExtra(EXTRA_KEY)
                        val refId = intent.getStringExtra(EXTRA_REF_ID)
                        val title = intent.getStringExtra(EXTRA_TITLE)
                        AlarmSoundPlayer.stopRinging(context)
                        if (!key.isNullOrBlank()) {
                            runCatching {
                                NotificationManagerCompat.from(context).cancel(key.hashCode())
                            }
                        }
                        reminderScheduler.scheduleSnooze(refId = refId, title = title, snoozeMinutes = 10)
                    }

                    else -> {
                        reminderScheduler.rescheduleAll()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
