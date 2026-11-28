package id.temankontrol.app.feature.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.domain.model.Attachment

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
        consultationId = viewModel.consultationIdString,
        onNavigateBack = handleBack,
        onSummaryChange = viewModel::onSummaryChange,
        onProviderInstructionChange = viewModel::onProviderInstructionChange,
        onAdditionalNotesChange = viewModel::onAdditionalNotesChange,
        onAddPhoto = viewModel::addPhotoAttachment,
        onAddVoiceNote = viewModel::addVoiceAttachment,
        onDeleteAttachment = viewModel::deleteAttachment,
        onUpdateAttachmentCaption = viewModel::updateAttachmentCaption,
        onSaveClick = { viewModel.saveNote(onSuccess = onNavigateBack) },
        onDeleteClick = { showDeleteDialog = true }
    )
}

@Composable
fun NoteContent(
    uiState: NoteUiState,
    consultationId: String = "default",
    onNavigateBack: () -> Unit,
    onSummaryChange: (String) -> Unit,
    onProviderInstructionChange: (String) -> Unit,
    onAdditionalNotesChange: (String) -> Unit,
    onAddPhoto: (String?, String?) -> Unit,
    onAddVoiceNote: (Long, String?, String?) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onUpdateAttachmentCaption: (String, String?) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.generalError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.generalError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Section 1: Ringkasan
                SectionCard(title = stringResource(R.string.label_note_summary)) {
                    LabeledTextField(
                        value = uiState.summary,
                        onValueChange = onSummaryChange,
                        label = "Ringkasan Pembahasan",
                        placeholder = stringResource(R.string.hint_note_summary),
                        maxLength = 2000,
                        singleLine = false,
                        minLines = 3,
                        errorMessage = uiState.summaryError
                    )
                }

                // Section 2: Arahan Tenaga Kesehatan
                SectionCard(title = stringResource(R.string.label_note_provider_instruction)) {
                    LabeledTextField(
                        value = uiState.providerInstruction,
                        onValueChange = onProviderInstructionChange,
                        label = "Arahan & Jadwal Kontrol Berikutnya",
                        placeholder = stringResource(R.string.hint_note_provider_instruction),
                        maxLength = 2000,
                        singleLine = false,
                        minLines = 3,
                        errorMessage = uiState.providerInstructionError
                    )
                }

                // Section 3: Catatan Tambahan + Lampiran Foto & VN
                SectionCard(title = stringResource(R.string.label_note_additional)) {
                    LabeledTextField(
                        value = uiState.additionalNotes,
                        onValueChange = onAdditionalNotesChange,
                        label = "Penjelasan Tambahan Tenaga Medis",
                        placeholder = stringResource(R.string.hint_note_additional),
                        maxLength = 2000,
                        singleLine = false,
                        minLines = 3,
                        errorMessage = uiState.additionalNotesError
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                    Spacer(modifier = Modifier.height(12.dp))

                    AttachmentSection(
                        attachments = uiState.attachments,
                        consultationId = consultationId,
                        title = "Lampiran Penjelasan Tambahan (Foto & VN)",
                        emptyHint = "Tambahkan foto dokumen/gambar atau rekaman suara penjelasan tambahan dari tenaga medis.",
                        onAddPhoto = onAddPhoto,
                        onAddVoiceNote = onAddVoiceNote,
                        onDeleteAttachment = onDeleteAttachment,
                        onUpdateCaption = onUpdateAttachmentCaption
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onSaveClick,
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
