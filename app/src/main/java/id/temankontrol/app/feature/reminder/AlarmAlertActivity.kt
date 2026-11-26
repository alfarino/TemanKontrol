package id.temankontrol.app.feature.reminder

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import id.temankontrol.app.MainActivity
import id.temankontrol.app.data.reminder.ActiveAlarmPayload
import id.temankontrol.app.data.reminder.AlarmSoundPlayer
import id.temankontrol.app.data.reminder.ReminderNotificationHelper
import id.temankontrol.app.data.reminder.ReminderScheduler
import id.temankontrol.app.ui.theme.TemanKontrolTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AlarmAlertActivity : ComponentActivity() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    companion object {
        const val EXTRA_KEY = "extra_alarm_key"
        const val EXTRA_KIND = "extra_alarm_kind"
        const val EXTRA_REF_ID = "extra_alarm_ref_id"
        const val EXTRA_TITLE = "extra_alarm_title"
        const val EXTRA_MESSAGE = "extra_alarm_message"
        const val EXTRA_TRIGGER_MS = "extra_alarm_trigger_ms"
        const val EXTRA_IS_TEST = "extra_alarm_is_test"

        fun createIntent(
            context: Context,
            payload: ActiveAlarmPayload
        ): Intent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_KEY, payload.key)
            putExtra(EXTRA_KIND, payload.kindName)
            putExtra(EXTRA_REF_ID, payload.refId)
            putExtra(EXTRA_TITLE, payload.title)
            putExtra(EXTRA_MESSAGE, payload.message)
            putExtra(EXTRA_TRIGGER_MS, payload.triggerAtMs)
            putExtra(EXTRA_IS_TEST, payload.isTestAlarm)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val payload = parsePayload(intent)
        if (AlarmSoundPlayer.activeAlarm.value == null) {
            AlarmSoundPlayer.startRinging(this, payload)
        }

        enableEdgeToEdge()
        setContent {
            val active by AlarmSoundPlayer.activeAlarm.collectAsStateWithLifecycle()
            val displayPayload = active ?: payload

            TemanKontrolTheme {
                AlarmAlertScreen(
                    payload = displayPayload,
                    onDismissAlarm = {
                        AlarmSoundPlayer.stopRinging(this)
                        finish()
                    },
                    onSnoozeTenMinutes = {
                        AlarmSoundPlayer.stopRinging(this)
                        lifecycleScope.launch {
                            reminderScheduler.scheduleSnooze(
                                refId = displayPayload.refId,
                                title = displayPayload.title,
                                snoozeMinutes = 10
                            )
                        }
                        Toast.makeText(
                            this,
                            "Alarm ditunda 10 menit",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    },
                    onOpenConsultation = { refId ->
                        AlarmSoundPlayer.stopRinging(this)
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(ReminderNotificationHelper.EXTRA_NAV_CONSULTATION_ID, refId)
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val payload = parsePayload(intent)
        AlarmSoundPlayer.startRinging(this, payload)
    }

    private fun parsePayload(intent: Intent?): ActiveAlarmPayload {
        val nowMs = System.currentTimeMillis()
        val key = intent?.getStringExtra(EXTRA_KEY) ?: "alarm:$nowMs"
        val kind = intent?.getStringExtra(EXTRA_KIND) ?: "VISIT_AT_TIME"
        val refId = intent?.getStringExtra(EXTRA_REF_ID)
        val title = intent?.getStringExtra(EXTRA_TITLE)
        val message = intent?.getStringExtra(EXTRA_MESSAGE)
            ?: "Waktunya bersiap untuk jadwal kunjungan Anda."
        val triggerMs = intent?.getLongExtra(EXTRA_TRIGGER_MS, nowMs) ?: nowMs
        val isTest = intent?.getBooleanExtra(EXTRA_IS_TEST, false) ?: false
        return ActiveAlarmPayload(
            key = key,
            kindName = kind,
            refId = refId,
            title = title,
            message = message,
            triggerAtMs = triggerMs,
            isTestAlarm = isTest
        )
    }
}

@Composable
fun AlarmAlertScreen(
    payload: ActiveAlarmPayload,
    onDismissAlarm: () -> Unit,
    onSnoozeTenMinutes: () -> Unit,
    onOpenConsultation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH.mm", Locale.forLanguageTag("id-ID"))
    }
    val formattedTime = remember(payload.triggerAtMs) {
        Instant.ofEpochMilli(payload.triggerAtMs)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top header badge
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (payload.isTestAlarm) {
                            "Uji Coba Alarm Teman Kontrol"
                        } else {
                            "Alarm Pengingat Kunjungan"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Center visual alarm card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = payload.title?.takeIf { it.isNotBlank() } ?: "Teman Kontrol",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = payload.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }

            // Bottom Action Buttons (Dismiss, Snooze 10m, Open Detail)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismissAlarm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Matikan Alarm / Saya Siap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onSnoozeTenMinutes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tunda 10 Menit (Snooze)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!payload.refId.isNullOrBlank() && !payload.isTestAlarm) {
                    TextButton(
                        onClick = { onOpenConsultation(payload.refId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buka Detail Kunjungan & Pertanyaan",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
