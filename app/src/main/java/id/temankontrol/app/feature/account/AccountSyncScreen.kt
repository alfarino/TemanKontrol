package id.temankontrol.app.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.core.designsystem.component.AppScaffold
import id.temankontrol.app.core.designsystem.component.AppTopBar
import id.temankontrol.app.core.designsystem.component.DateUtils
import id.temankontrol.app.core.designsystem.component.SectionCard
import id.temankontrol.app.ui.theme.HeroTealEndDark
import id.temankontrol.app.ui.theme.HeroTealEndLight
import id.temankontrol.app.ui.theme.HeroTealStartDark
import id.temankontrol.app.ui.theme.HeroTealStartLight
import java.time.Instant

@Composable
fun AccountSyncScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToBackup: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteAccountPasswordDialog(
            onConfirm = { password ->
                showDeleteDialog = false
                viewModel.deleteAccountPermanently(password) {
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    AccountSyncContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToLogin = onNavigateToLogin,
        onNavigateToBackup = onNavigateToBackup,
        onSyncNow = { viewModel.syncNow(forceFullRestore = false) },
        onFullRestore = { viewModel.syncNow(forceFullRestore = true) },
        onLogout = { viewModel.logout(onNavigateBack) },
        onRequestDeleteAccount = { showDeleteDialog = true }
    )
}

@Composable
fun AccountSyncContent(
    uiState: AccountUiState,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onSyncNow: () -> Unit,
    onFullRestore: () -> Unit,
    onLogout: () -> Unit,
    onRequestDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = uiState.session
    val isDark = isSystemInDarkTheme()
    val gradientStart = if (isDark) HeroTealStartDark else HeroTealStartLight
    val gradientEnd = if (isDark) HeroTealEndDark else HeroTealEndLight

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "Akun & Sinkronisasi Cloud",
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
            if (session == null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Anda Menggunakan Mode Lokal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Masuk atau daftar akun unand.tech untuk mencadangkan otomatis seluruh catatan, pertanyaan, foto, dan rekaman suara Anda agar pulih saat ganti HP atau pindah ke iPhone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                        Button(
                            onClick = onNavigateToLogin,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = gradientStart
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Masuk / Daftar Akun", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = session.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "Server: ${session.serverBaseUrl}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.22f))

                        val syncTimeText = session.lastSyncAtMillis?.let {
                            "Tersinkron terakhir: ${DateUtils.formatRelativeDateTime(Instant.ofEpochMilli(it))}"
                        } ?: "Belum pernah disinkronkan pada perangkat ini"

                        Text(
                            text = syncTimeText,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                if (uiState.isBusy) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.busyMessage ?: "Menyinkronkan…",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (uiState.statusMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                SectionCard(title = "Sinkronisasi & Pemulihan Cloud") {
                    Button(
                        enabled = !uiState.isBusy,
                        onClick = onSyncNow,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sinkronkan Sekarang", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        enabled = !uiState.isBusy,
                        onClick = onFullRestore,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pulihkan Penuh dari Server (termasuk Foto & VN)")
                    }
                }

                SectionCard(title = "Manajemen Akun & Privasi (UU PDP)") {
                    OutlinedButton(
                        enabled = !uiState.isBusy,
                        onClick = onLogout,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keluar dari Akun (Data Lokal Tetap Disimpan)")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        enabled = !uiState.isBusy,
                        onClick = onRequestDeleteAccount,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hapus Akun & Seluruh Data Permanen", fontWeight = FontWeight.Bold)
                    }
                }
            }

            OutlinedButton(
                onClick = onNavigateToBackup,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka Cadangan Berkas Lokal (.tkbackup)")
            }
        }
    }
}

@Composable
private fun DeleteAccountPasswordDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Akun & Data Permanen?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tindakan ini akan menghapus permanen akun Anda, seluruh jadwal, pertanyaan, catatan, dan file Foto/VN di server Ubuntu maupun di perangkat ini. Tindakan tidak dapat dibatalkan.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Masukkan Kata Sandi Anda") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = password.isNotBlank(),
                onClick = { onConfirm(password) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Hapus Permanen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
