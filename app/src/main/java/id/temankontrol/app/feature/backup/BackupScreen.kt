package id.temankontrol.app.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.ui.theme.StatusCompletedDark
import id.temankontrol.app.ui.theme.StatusCompletedLight
import id.temankontrol.app.ui.theme.StatusWarningDark
import id.temankontrol.app.ui.theme.StatusWarningLight
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }

    val defaultBackupName = remember {
        "TemanKontrol-${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.tkbackup"
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUri(uri.toString())
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "cadangan.tkbackup"
            viewModel.inspectRestoreUri(uri.toString(), name)
        }
    }

    if (showConfirmRestoreDialog && uiState.restorePreview != null) {
        ConfirmDialog(
            title = "Ganti data saat ini?",
            message = "Data di HP ini akan diganti dengan isi berkas cadangan (${uiState.restorePreview!!.fileName}). Tindakan ini tidak dapat dibatalkan.",
            confirmButtonText = "Ganti Data",
            isDestructive = true,
            onConfirm = {
                showConfirmRestoreDialog = false
                viewModel.confirmRestore()
            },
            onDismiss = { showConfirmRestoreDialog = false }
        )
    }

    BackupContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleEncrypt = viewModel::setEncryptWithPassword,
        onPasswordChange = viewModel::setPassword,
        onConfirmPasswordChange = viewModel::setConfirmPassword,
        onStartExport = {
            if (viewModel.validateBeforeExport()) {
                createBackupLauncher.launch(defaultBackupName)
            }
        },
        onPickRestoreFile = {
            openBackupLauncher.launch(arrayOf("*/*"))
        },
        onRestorePasswordChange = viewModel::setRestorePassword,
        onInspectWithPassword = {
            uiState.restorePreview?.let { preview ->
                viewModel.inspectRestoreUri(preview.uriString, preview.fileName, uiState.restorePassword)
            }
        },
        onConfirmRestoreClick = { showConfirmRestoreDialog = true },
        onDismissRestorePreview = viewModel::dismissRestorePreview
    )
}

@Composable
fun BackupContent(
    uiState: BackupUiState,
    onNavigateBack: () -> Unit,
    onToggleEncrypt: (Boolean) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onStartExport: () -> Unit,
    onPickRestoreFile: () -> Unit,
    onRestorePasswordChange: (String) -> Unit,
    onInspectWithPassword: () -> Unit,
    onConfirmRestoreClick: () -> Unit,
    onDismissRestorePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.backup_title),
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tanpa akun & tanpa internet: ekspor seluruh data menjadi satu berkas .tkbackup lalu simpan ke Google Drive, komputer, atau flashdisk Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            SectionCard(title = "Ringkasan Data Saat Ini") {
                val lastBackupLabel = if (uiState.summary.lastBackupAt == null) {
                    "Belum pernah dicadangkan"
                } else {
                    DateUtils.formatRelativeDateTime(uiState.summary.lastBackupAt)
                }
                Text(
                    text = "Terakhir dicadangkan: $lastBackupLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (uiState.summary.lastBackupAt == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SummaryStatPill("Kunjungan", "${uiState.summary.consultationCount}")
                    SummaryStatPill("Pertanyaan", "${uiState.summary.questionCount}")
                    SummaryStatPill("Lampiran", "${uiState.summary.attachmentCount}")
                    SummaryStatPill("Ukuran", formatSizeKb(uiState.summary.estimatedSizeBytes))
                }
            }

            SectionCard(title = "Buat Cadangan Baru") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lindungi dengan Kata Sandi (AES-256)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Disarankan bila berkas akan disimpan di cloud/Drive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.encryptWithPassword,
                        onCheckedChange = onToggleEncrypt
                    )
                }

                if (uiState.encryptWithPassword) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = { Text("Kata Sandi Cadangan (min. 8 karakter)") },
                        isError = uiState.passwordError != null,
                        supportingText = uiState.passwordError?.let { { Text(it) } },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Tampilkan kata sandi"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = { Text("Ulangi Kata Sandi") },
                        isError = uiState.confirmError != null,
                        supportingText = uiState.confirmError?.let { { Text(it) } },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.hint_backup_password_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = onStartExport,
                    enabled = !uiState.isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buat Cadangan (.tkbackup)")
                }

                if (uiState.exportErrorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.exportErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (uiState.exportSuccessFileName != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val isDark = isSystemInDarkTheme()
                    val okBg = if (isDark) StatusCompletedDark.copy(alpha = 0.16f) else StatusCompletedLight.copy(alpha = 0.12f)
                    val okFg = if (isDark) StatusCompletedDark else StatusCompletedLight
                    val warnBg = if (isDark) StatusWarningDark.copy(alpha = 0.18f) else StatusWarningLight.copy(alpha = 0.14f)
                    val warnFg = if (isDark) StatusWarningDark else StatusWarningLight

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(okBg)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = okFg, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cadangan tersimpan: ${uiState.exportSuccessFileName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = okFg
                            )
                        }
                        if (uiState.exportSavedLocallyWarning) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(warnBg)
                                    .padding(10.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = warnFg, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.hint_backup_local_warning),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = warnFg
                                )
                            }
                        }
                    }
                }
            }

            SectionCard(title = "Pulihkan di HP Ini / HP Baru") {
                Text(
                    text = "Pilih berkas .tkbackup dari penyimpanan atau Google Drive untuk memulihkan data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onPickRestoreFile,
                    enabled = !uiState.isProcessing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pilih Berkas .tkbackup")
                }

                if (uiState.restoreErrorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.restoreErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (uiState.restorePreview != null) {
                    val preview = uiState.restorePreview
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Berkas: ${preview.fileName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (preview.requiresPasswordPrompt) {
                                Text(
                                    text = "Cadangan ini terenkripsi. Masukkan kata sandi untuk melihat isi dan memulihkan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = uiState.restorePassword,
                                    onValueChange = onRestorePasswordChange,
                                    label = { Text("Kata Sandi Cadangan") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = onDismissRestorePreview,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.action_cancel))
                                    }
                                    Button(
                                        onClick = onInspectWithPassword,
                                        enabled = uiState.restorePassword.isNotEmpty() && !uiState.isProcessing,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Buka Pratinjau")
                                    }
                                }
                            } else {
                                val dateStr = preview.createdAt?.let { DateUtils.formatRelativeDateTime(it) } ?: "-"
                                Text(
                                    text = "Dibuat: $dateStr (v${preview.appVersion})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Isi: ${preview.consultationCount} kunjungan · ${preview.questionCount} pertanyaan · ${preview.attachmentCount} lampiran",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = onDismissRestorePreview,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.action_cancel))
                                    }
                                    Button(
                                        onClick = onConfirmRestoreClick,
                                        enabled = !uiState.isProcessing,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Pulihkan Data")
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.restoreSuccessMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = uiState.restoreSuccessMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
