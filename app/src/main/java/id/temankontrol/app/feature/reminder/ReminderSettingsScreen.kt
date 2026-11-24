package id.temankontrol.app.feature.reminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.AppScaffold
import id.temankontrol.app.core.designsystem.component.AppTopBar
import id.temankontrol.app.core.designsystem.component.SectionCard
import id.temankontrol.app.domain.model.ReminderSettings
import id.temankontrol.app.ui.theme.StatusWarningDark
import id.temankontrol.app.ui.theme.StatusWarningLight
import java.util.Locale

@Composable
fun ReminderSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReminderSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setHasNotificationPermission(granted || NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.setHasNotificationPermission(enabled)
    }

    ReminderSettingsContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleVisit = {
            if (it) requestNotifPermissionIfNeeded()
            viewModel.setVisitReminderEnabled(it)
        },
        onSelectVisitHour = viewModel::setVisitReminderHour,
        onTogglePrepare = {
            if (it) requestNotifPermissionIfNeeded()
            viewModel.setPrepareReminderEnabled(it)
        },
        onToggleFollowUp = {
            if (it) requestNotifPermissionIfNeeded()
            viewModel.setFollowUpReminderEnabled(it)
        },
        onSelectFollowUpHour = viewModel::setFollowUpReminderHour,
        onToggleBackup = {
            if (it) requestNotifPermissionIfNeeded()
            viewModel.setBackupReminderEnabled(it)
        },
        onToggleShowTitle = viewModel::setShowTitleOnLockScreen,
        onOpenAppNotificationSettings = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        },
        onOpenBatterySettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    )
}

@Composable
fun ReminderSettingsContent(
    uiState: ReminderSettings,
    onNavigateBack: () -> Unit,
    onToggleVisit: (Boolean) -> Unit,
    onSelectVisitHour: (Int) -> Unit,
    onTogglePrepare: (Boolean) -> Unit,
    onToggleFollowUp: (Boolean) -> Unit,
    onSelectFollowUpHour: (Int) -> Unit,
    onToggleBackup: (Boolean) -> Unit,
    onToggleShowTitle: (Boolean) -> Unit,
    onOpenAppNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val warnBg = if (isDark) StatusWarningDark.copy(alpha = 0.2f) else StatusWarningLight.copy(alpha = 0.14f)
    val warnFg = if (isDark) StatusWarningDark else StatusWarningLight

    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.reminder_title),
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.hasNotificationPermission) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notifikasi dimatikan di pengaturan HP",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Aktifkan agar pengingat muncul. Aplikasi tetap berfungsi penuh tanpa notifikasi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    TextButton(onClick = onOpenAppNotificationSettings) {
                        Text("Buka")
                    }
                }
            }

            // Info offline-only local notification
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Semua pengingat dijadwalkan secara lokal di HP ini (100% offline, tanpa internet & server).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Jenis Pengingat
            SectionCard(title = "Jenis Pengingat") {
                ReminderToggleItem(
                    title = "Pengingat Kunjungan",
                    subtitle = "Muncul H-1 pukul ${formatHour(uiState.visitReminderHour)} dan 2 jam sebelum jadwal",
                    checked = uiState.visitReminderEnabled,
                    onCheckedChange = onToggleVisit
                )
                if (uiState.visitReminderEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HourChipRow(
                        selectedHour = uiState.visitReminderHour,
                        options = listOf(18, 19, 20),
                        onSelectHour = onSelectVisitHour
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

                ReminderToggleItem(
                    title = "Pengingat Siapkan Pertanyaan",
                    subtitle = "Muncul H-2 sebelum kontrol bila daftar pertanyaan masih kosong",
                    checked = uiState.prepareReminderEnabled,
                    onCheckedChange = onTogglePrepare
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

                ReminderToggleItem(
                    title = "Pengingat Tenggat Tindak Lanjut",
                    subtitle = "Muncul pada hari tenggat pukul ${formatHour(uiState.followUpReminderHour)} dan sekali pada H+1 bila belum selesai",
                    checked = uiState.followUpReminderEnabled,
                    onCheckedChange = onToggleFollowUp
                )
                if (uiState.followUpReminderEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HourChipRow(
                        selectedHour = uiState.followUpReminderHour,
                        options = listOf(7, 8, 9),
                        onSelectHour = onSelectFollowUpHour
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

                ReminderToggleItem(
                    title = "Pengingat Cadangan Berkala",
                    subtitle = "Ingatkan setiap 30 hari bila ada data baru yang belum dicadangkan",
                    checked = uiState.backupReminderEnabled,
                    onCheckedChange = onToggleBackup
                )
            }

            // Privasi Layar Kunci & Pratinjau Notifikasi
            SectionCard(title = "Privasi Notifikasi di Layar Kunci") {
                ReminderToggleItem(
                    title = "Tampilkan judul kunjungan",
                    subtitle = "Jika mati, layar kunci hanya menampilkan pesan generik tanpa detail medis",
                    checked = uiState.showTitleOnLockScreen,
                    onCheckedChange = onToggleShowTitle
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Pratinjau Tampilan Notifikasi:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Teman Kontrol",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.showTitleOnLockScreen) {
                                    "Besok 09.00: Kontrol Hipertensi Rutin"
                                } else {
                                    "Ada jadwal kunjungan besok pukul 09.00"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Panduan Baterai Agresif (MIUI / Xiaomi)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(warnBg)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = warnFg
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Agar pengingat tepat waktu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = warnFg
                    )
                    Text(
                        text = "Pada beberapa HP (mis. Xiaomi/MIUI), izinkan Teman Kontrol tanpa pembatasan baterai.",
                        style = MaterialTheme.typography.bodySmall,
                        color = warnFg
                    )
                }
                TextButton(onClick = onOpenBatterySettings) {
                    Text("Atur")
                }
            }
        }
    }
}

@Composable
private fun ReminderToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun HourChipRow(
    selectedHour: Int,
    options: List<Int>,
    onSelectHour: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Jam:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        options.forEach { hour ->
            FilterChip(
                selected = selectedHour == hour,
                onClick = { onSelectHour(hour) },
                label = { Text(formatHour(hour)) }
            )
        }
    }
}

private fun formatHour(hour: Int): String = String.format(Locale.US, "%02d.00", hour)
