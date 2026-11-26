package id.temankontrol.app.feature.settings

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.AppScaffold
import id.temankontrol.app.core.designsystem.component.AppTopBar
import id.temankontrol.app.core.designsystem.component.DateUtils
import id.temankontrol.app.core.designsystem.component.SectionCard
import id.temankontrol.app.domain.model.AppLockTimeout
import id.temankontrol.app.feature.lock.AppLockOverlay
import id.temankontrol.app.ui.theme.HeroTealEndDark
import id.temankontrol.app.ui.theme.HeroTealEndLight
import id.temankontrol.app.ui.theme.HeroTealStartDark
import id.temankontrol.app.ui.theme.HeroTealStartLight

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLockPreview by remember { mutableStateOf(false) }

    val hasDeviceCredential = remember(context) {
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    if (showLockPreview) {
        AppLockOverlay(
            onUnlockClick = { showLockPreview = false }
        )
        return
    }

    SettingsContent(
        uiState = uiState,
        hasDeviceCredential = hasDeviceCredential,
        onNavigateBack = onNavigateBack,
        onNavigateToBackup = onNavigateToBackup,
        onNavigateToReminders = onNavigateToReminders,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateToAccount = onNavigateToAccount,
        onNavigateToLogin = onNavigateToLogin,
        onToggleAppLock = viewModel::setAppLockEnabled,
        onSelectTimeout = viewModel::setAppLockTimeout,
        onToggleSecureScreen = viewModel::setSecureScreen,
        onPreviewLockScreen = { showLockPreview = true }
    )
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    hasDeviceCredential: Boolean = true,
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onSelectTimeout: (AppLockTimeout) -> Unit,
    onToggleSecureScreen: (Boolean) -> Unit,
    onPreviewLockScreen: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val gradientStart = if (isDark) HeroTealStartDark else HeroTealStartLight
    val gradientEnd = if (isDark) HeroTealEndDark else HeroTealEndLight

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "Akun, Cadangan & Pengaturan"
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
            // 0. Kartu Profil, Mode Offline-First & Cloud Sync unand.tech (v3.0)
            val session = uiState.accountSession
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = if (session != null) onNavigateToAccount else onNavigateToLogin)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (session != null) Icons.Default.VerifiedUser else Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session?.displayName ?: "Mode Lokal (Tanpa Akun)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (session != null) {
                                    "${session.email} • Tersambung ke unand.tech"
                                } else {
                                    "Ketuk untuk Masuk / Daftar akun pemulihan cloud (temankontrol.unand.tech)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            // 1. Cadangan & Pemulihan (F-10)
            val backupSubtitle = if (uiState.lastBackupAt == null) {
                "Belum pernah dicadangkan • Lindungi data bila HP rusak"
            } else {
                "Terakhir: ${DateUtils.formatRelativeDateTime(uiState.lastBackupAt)}"
            }
            SettingsNavCard(
                icon = Icons.Default.Backup,
                title = stringResource(R.string.backup_title),
                subtitle = backupSubtitle,
                badgeText = if (uiState.lastBackupAt == null) "Disarankan" else null,
                onClick = onNavigateToBackup
            )

            // 2. Pengingat & Alarm Berbunyi (F-12 v3)
            val activeCount = listOf(
                uiState.reminderSettings.visitReminderEnabled,
                uiState.reminderSettings.prepareReminderEnabled,
                uiState.reminderSettings.followUpReminderEnabled,
                uiState.reminderSettings.backupReminderEnabled
            ).count { it }
            SettingsNavCard(
                icon = Icons.Default.Alarm,
                title = "Pusat Alarm & Pengingat",
                subtitle = "$activeCount pengingat aktif • Mode Alarm ${if (uiState.reminderSettings.alarmSoundModeEnabled) "Berbunyi Nyaring" else "Notifikasi"}",
                onClick = onNavigateToReminders
            )

            // 3. Keamanan & Kunci Aplikasi (F-14)
            SectionCard(
                title = "Keamanan & Kunci Aplikasi"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kunci Aplikasi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (hasDeviceCredential) {
                                "Minta sidik jari atau kunci layar HP sebelum membuka aplikasi"
                            } else {
                                "Tidak tersedia — atur PIN, pola, atau sidik jari di Pengaturan HP terlebih dahulu"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.appLockSettings.enabled && hasDeviceCredential,
                        enabled = hasDeviceCredential,
                        onCheckedChange = onToggleAppLock
                    )
                }

                if (uiState.appLockSettings.enabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Kunci otomatis setelah di latar belakang:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppLockTimeout.entries.forEach { timeout ->
                            FilterChip(
                                selected = uiState.appLockSettings.timeout == timeout,
                                onClick = { onSelectTimeout(timeout) },
                                label = { Text(timeout.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sembunyikan di Recent Apps",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Aktifkan FLAG_SECURE & blokir tangkapan layar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.appLockSettings.secureScreen,
                            onCheckedChange = onToggleSecureScreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onPreviewLockScreen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pratinjau Layar Kunci")
                    }
                }
            }

            // 4. Tentang Aplikasi, Privasi & Skenario Uji (SCR-09)
            SettingsNavCard(
                icon = Icons.Default.Info,
                title = "Tentang Teman Kontrol & Privasi",
                subtitle = "Informasi versi, batas penggunaan, dan penguji skenario data",
                onClick = onNavigateToAbout
            )
        }
    }
}

@Composable
private fun SettingsNavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
