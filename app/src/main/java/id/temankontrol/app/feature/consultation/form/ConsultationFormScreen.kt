package id.temankontrol.app.feature.consultation.form

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.ui.theme.StatusWarningDark
import id.temankontrol.app.ui.theme.StatusWarningLight

@Composable
fun ConsultationFormScreen(
    onNavigateBack: () -> Unit,
    onSuccess: (String) -> Unit,
    viewModel: ConsultationFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    var pendingSubmitAfterPermission by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        if (pendingSubmitAfterPermission) {
            pendingSubmitAfterPermission = false
            viewModel.submit(
                onSuccess = onSuccess,
                onError = { /* show in snackbar */ }
            )
        }
    }

    val handleBack = {
        if (uiState.isDirty) {
            showUnsavedDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = true, onBack = handleBack)

    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            onConfirmDiscard = {
                showUnsavedDialog = false
                onNavigateBack()
            },
            onDismiss = { showUnsavedDialog = false }
        )
    }

    ConsultationFormContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = handleBack,
        onTitleChange = viewModel::onTitleChange,
        onReasonChange = viewModel::onReasonChange,
        onFacilityChange = viewModel::onFacilityChange,
        onProviderChange = viewModel::onProviderChange,
        onDateChange = viewModel::onDateChange,
        onTimeChange = viewModel::onTimeChange,
        onSubmit = {
            // FR-072: Request POST_NOTIFICATIONS before saving & navigating so alarm scheduling is never raced
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                pendingSubmitAfterPermission = true
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.submit(
                    onSuccess = onSuccess,
                    onError = { /* show in snackbar */ }
                )
            }
        }
    )
}

@Composable
fun ConsultationFormContent(
    uiState: ConsultationFormUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onFacilityChange: (String) -> Unit,
    onProviderChange: (String) -> Unit,
    onDateChange: (java.time.LocalDate) -> Unit,
    onTimeChange: (java.time.LocalTime) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEdit = uiState.id != null
    val pageTitle = if (isEdit) "Ubah Kunjungan" else "Tambah Kunjungan"

    AppScaffold(
        topBar = {
            AppTopBar(
                title = pageTitle,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    ) { padding ->
        if (uiState.isLoading) {
            LoadingView(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Judul
                LabeledTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    label = stringResource(R.string.label_title),
                    placeholder = stringResource(R.string.hint_title),
                    maxLength = 80,
                    errorMessage = uiState.titleError
                )

                // Alasan Kunjungan
                LabeledTextField(
                    value = uiState.reason,
                    onValueChange = onReasonChange,
                    label = stringResource(R.string.label_reason),
                    placeholder = stringResource(R.string.hint_reason),
                    maxLength = 500,
                    singleLine = false,
                    minLines = 3,
                    errorMessage = uiState.reasonError
                )

                // Fasilitas Kesehatan
                LabeledTextField(
                    value = uiState.facilityName,
                    onValueChange = onFacilityChange,
                    label = stringResource(R.string.label_facility),
                    placeholder = stringResource(R.string.hint_facility),
                    maxLength = 100,
                    errorMessage = uiState.facilityError
                )

                // Tenaga Kesehatan
                LabeledTextField(
                    value = uiState.providerName,
                    onValueChange = onProviderChange,
                    label = stringResource(R.string.label_provider),
                    placeholder = stringResource(R.string.hint_provider),
                    maxLength = 100,
                    errorMessage = uiState.providerError
                )

                // Tanggal & Waktu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DatePickerField(
                        selectedDate = uiState.appointmentDate,
                        onDateSelected = onDateChange,
                        label = stringResource(R.string.label_appointment_date),
                        modifier = Modifier.weight(1f)
                    )

                    TimePickerField(
                        selectedTime = uiState.appointmentTime,
                        onTimeSelected = onTimeChange,
                        label = stringResource(R.string.label_appointment_time),
                        modifier = Modifier.weight(1f)
                    )
                }

                // C-42 Warning banner if date is in the past
                if (uiState.isPastDate) {
                    val isDark = isSystemInDarkTheme()
                    val bg = if (isDark) StatusWarningDark.copy(alpha = 0.2f) else StatusWarningLight.copy(alpha = 0.15f)
                    val fg = if (isDark) StatusWarningDark else StatusWarningLight

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.banner_past_consultation_date),
                            style = MaterialTheme.typography.bodySmall,
                            color = fg
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.action_save),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}