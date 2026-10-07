package id.temankontrol.app.feature.consultation.detail

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.domain.model.ConsultationStatus
import id.temankontrol.app.domain.validation.Validation

@Composable
fun ConsultationDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToQuestions: (String) -> Unit,
    onNavigateToNotes: (String) -> Unit,
    onNavigateToFollowUps: (String) -> Unit,
    viewModel: ConsultationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_cancel_consultation_title),
            message = stringResource(R.string.dialog_cancel_consultation_body),
            confirmButtonText = stringResource(R.string.action_mark_cancelled),
            dismissButtonText = stringResource(R.string.action_back),
            isDestructive = false,
            onConfirm = {
                showCancelDialog = false
                viewModel.setStatus(ConsultationStatus.CANCELLED)
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    if (showDeleteDialog) {
        val bodyText = stringResource(
            R.string.dialog_delete_consultation_body,
            uiState.childCounts.questionCount,
            uiState.childCounts.actionCount
        )
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_consultation_title),
            message = bodyText,
            confirmButtonText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteConsultation(onDeleted = onNavigateBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    ConsultationDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEditClick = { onNavigateToEdit(viewModel.consultationIdString) },
        onDeleteClick = { showDeleteDialog = true },
        onManageQuestionsClick = { onNavigateToQuestions(viewModel.consultationIdString) },
        onManageNotesClick = { onNavigateToNotes(viewModel.consultationIdString) },
        onManageFollowUpsClick = { onNavigateToFollowUps(viewModel.consultationIdString) },
        onSetStatus = { status ->
            if (status == ConsultationStatus.CANCELLED) {
                showCancelDialog = true
            } else {
                viewModel.setStatus(status)
            }
        },
        onToggleQuestion = viewModel::toggleQuestionDiscussed,
        onToggleFollowUp = viewModel::toggleFollowUpCompleted
    )
}

@Composable
fun ConsultationDetailContent(
    uiState: ConsultationDetailUiState,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onManageQuestionsClick: () -> Unit,
    onManageNotesClick: () -> Unit,
    onManageFollowUpsClick: () -> Unit,
    onSetStatus: (ConsultationStatus) -> Unit,
    onToggleQuestion: (String, Boolean) -> Unit,
    onToggleFollowUp: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "Detail Kunjungan",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (uiState.consultation != null) {
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Default.Edit, contentDescription = "Ubah")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu Lainnya")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (uiState.consultation.status != ConsultationStatus.COMPLETED) {
                                    DropdownMenuItem(
                                        text = { Text("Tandai Selesai") },
                                        onClick = {
                                            showMenu = false
                                            onSetStatus(ConsultationStatus.COMPLETED)
                                        }
                                    )
                                }
                                if (uiState.consultation.status != ConsultationStatus.SCHEDULED) {
                                    DropdownMenuItem(
                                        text = { Text("Tandai Terjadwal") },
                                        onClick = {
                                            showMenu = false
                                            onSetStatus(ConsultationStatus.SCHEDULED)
                                        }
                                    )
                                }
                                if (uiState.consultation.status != ConsultationStatus.CANCELLED) {
                                    DropdownMenuItem(
                                        text = { Text("Tandai Dibatalkan") },
                                        onClick = {
                                            showMenu = false
                                            onSetStatus(ConsultationStatus.CANCELLED)
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Hapus Kunjungan", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClick()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingView(modifier = Modifier.padding(padding))
            }
            uiState.isNotFound || uiState.consultation == null -> {
                ErrorView(
                    message = stringResource(R.string.error_consultation_not_found),
                    actionText = stringResource(R.string.action_back),
                    onActionClick = onNavigateBack,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                val consultation = uiState.consultation
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Info Kunjungan
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val isPastDue = consultation.status == ConsultationStatus.SCHEDULED &&
                                        consultation.appointmentAt.isBefore(java.time.Instant.now())
                                StatusChip(
                                    status = consultation.status,
                                    isPastDue = isPastDue
                                )
                                Text(
                                    text = DateUtils.formatRelativeDateTime(consultation.appointmentAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = consultation.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = consultation.reason,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!consultation.facilityName.isNullOrBlank() || !consultation.providerName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                if (!consultation.facilityName.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = consultation.facilityName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!consultation.providerName.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = consultation.providerName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 1: Daftar Pertanyaan
                    SectionCard(
                        title = "1. Daftar Pertanyaan",
                        action = {
                            TextButton(onClick = onManageQuestionsClick) {
                                Text("Kelola")
                            }
                        }
                    ) {
                        if (uiState.questions.isEmpty()) {
                            Text(
                                text = stringResource(R.string.empty_questions_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.questions.take(3).forEach { q ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = q.isDiscussed,
                                            onCheckedChange = { onToggleQuestion(q.id, it) }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = q.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (q.isPriority) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            PriorityBadge()
                                        }
                                    }
                                }
                                if (uiState.questions.size > 3) {
                                    Text(
                                        text = "+${uiState.questions.size - 3} pertanyaan lainnya",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Section 2: Catatan Hasil Konsultasi
                    SectionCard(
                        title = "2. Hasil Konsultasi",
                        action = {
                            TextButton(onClick = onManageNotesClick) {
                                Text(if (uiState.note != null) "Ubah" else "Tulis")
                            }
                        }
                    ) {
                        val note = uiState.note
                        if (note == null || !note.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.empty_notes_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!note.summary.isNullOrBlank()) {
                                    Text(
                                        text = "Ringkasan:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = note.summary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (!note.providerInstruction.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Arahan Tenaga Kesehatan:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = note.providerInstruction,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (!note.additionalNotes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Catatan Tambahan:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = note.additionalNotes,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // Section 3: Tindak Lanjut
                    SectionCard(
                        title = "3. Tindak Lanjut",
                        action = {
                            TextButton(onClick = onManageFollowUpsClick) {
                                Text("Kelola")
                            }
                        }
                    ) {
                        if (uiState.followUps.isEmpty()) {
                            Text(
                                text = stringResource(R.string.empty_follow_ups_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.followUps.take(4).forEach { action ->
                                    val isPast = Validation.isDueDatePast(action.dueDate)
                                    ChecklistItem(
                                        title = action.text,
                                        checked = action.isCompleted,
                                        onCheckedChange = { checked ->
                                            onToggleFollowUp(action.id, checked)
                                        },
                                        dueDate = action.dueDate,
                                        isPastDue = isPast
                                    )
                                }
                                if (uiState.followUps.size > 4) {
                                    Text(
                                        text = "+${uiState.followUps.size - 4} tindak lanjut lainnya",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}