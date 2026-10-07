package id.temankontrol.app.feature.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*

@Composable
fun NoteScreen(
    onNavigateBack: () -> Unit,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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

    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_note_title),
            message = "Catatan hasil konsultasi ini akan dihapus secara permanen.",
            confirmButtonText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteNote(onSuccess = onNavigateBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    NoteContent(
        uiState = uiState,
        onNavigateBack = handleBack,
        onSummaryChange = viewModel::onSummaryChange,
        onProviderInstructionChange = viewModel::onProviderInstructionChange,
        onAdditionalNotesChange = viewModel::onAdditionalNotesChange,
        onSaveClick = { viewModel.saveNote(onSuccess = onNavigateBack) },
        onDeleteClick = { showDeleteDialog = true }
    )
}

@Composable
fun NoteContent(
    uiState: NoteUiState,
    onNavigateBack: () -> Unit,
    onSummaryChange: (String) -> Unit,
    onProviderInstructionChange: (String) -> Unit,
    onAdditionalNotesChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = "Hasil Konsultasi",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (uiState.isExistingNote) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Catatan",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
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
                if (uiState.generalError != null) {
                    Text(
                        text = uiState.generalError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Section 1: Ringkasan
                LabeledTextField(
                    value = uiState.summary,
                    onValueChange = onSummaryChange,
                    label = stringResource(R.string.label_note_summary),
                    placeholder = stringResource(R.string.hint_note_summary),
                    maxLength = 2000,
                    singleLine = false,
                    minLines = 4,
                    errorMessage = uiState.summaryError
                )

                // Section 2: Arahan Tenaga Kesehatan
                LabeledTextField(
                    value = uiState.providerInstruction,
                    onValueChange = onProviderInstructionChange,
                    label = stringResource(R.string.label_note_provider_instruction),
                    placeholder = stringResource(R.string.hint_note_provider_instruction),
                    maxLength = 2000,
                    singleLine = false,
                    minLines = 4,
                    errorMessage = uiState.providerInstructionError
                )

                // Section 3: Catatan Tambahan
                LabeledTextField(
                    value = uiState.additionalNotes,
                    onValueChange = onAdditionalNotesChange,
                    label = stringResource(R.string.label_note_additional),
                    placeholder = stringResource(R.string.hint_note_additional),
                    maxLength = 2000,
                    singleLine = false,
                    minLines = 3,
                    errorMessage = uiState.additionalNotesError
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSaveClick,
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