package id.temankontrol.app.data.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationManagerCompat
import id.temankontrol.app.core.common.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveAlarmPayload(
    val key: String,
    val kindName: String,
    val refId: String?,
    val title: String?,
    val message: String,
    val triggerAtMs: Long,
    val isTestAlarm: Boolean = false
)

object AlarmSoundPlayer {

    private const val AUTO_STOP_TIMEOUT_MS = 60_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _activeAlarm = MutableStateFlow<ActiveAlarmPayload?>(null)
    val activeAlarm: StateFlow<ActiveAlarmPayload?> = _activeAlarm.asStateFlow()

    private var currentRingtone: Ringtone? = null
    private var currentVibrator: Vibrator? = null
    private var autoStopJob: Job? = null

    @Synchronized
    fun startRinging(context: Context, payload: ActiveAlarmPayload) {
        stopRingingInternal()
        _activeAlarm.value = payload

        val appContext = context.applicationContext
        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // 1. Start looping alarm audio on USAGE_ALARM stream
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(appContext, uri)
            if (ringtone != null) {
                ringtone.audioAttributes = alarmAttributes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.isLooping = true
                }
                ringtone.play()
                currentRingtone = ringtone
            }
        }.onFailure { e ->
            AppLogger.w("AlarmSoundPlayer", "Failed to play alarm ringtone", e)
        }

        // 2. Start rhythmic alarm vibration
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0L, 800L, 400L, 800L, 400L, 1000L)
                val effect = VibrationEffect.createWaveform(pattern, 0)
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, alarmAttributes)
                currentVibrator = vibrator
            }
        }.onFailure { e ->
            AppLogger.w("AlarmSoundPlayer", "Failed to start alarm vibration", e)
        }

        // 3. Auto-stop after 60 seconds so unattended alarm doesn't drain battery indefinitely
        autoStopJob = scope.launch {
            delay(AUTO_STOP_TIMEOUT_MS)
            stopRinging(appContext)
        }
    }

    @Synchronized
    fun stopRinging(context: Context? = null) {
        val prev = _activeAlarm.value
        stopRingingInternal()
        if (context != null && prev != null) {
            runCatching {
                NotificationManagerCompat.from(context.applicationContext).cancel(prev.key.hashCode())
            }
        }
    }

    private fun stopRingingInternal() {
        autoStopJob?.cancel()
        autoStopJob = null
        runCatching { currentRingtone?.stop() }
        currentRingtone = null
        runCatching { currentVibrator?.cancel() }
        currentVibrator = null
        _activeAlarm.value = null
    }
}
