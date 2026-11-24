package id.temankontrol.app.feature.consultation.detail

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.domain.model.AttachmentKind
import id.temankontrol.app.domain.model.ConsultationStatus
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.launch

@Composable
fun ConsultationDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToCreateConsultation: () -> Unit,
    onNavigateToQuestions: (String) -> Unit,
    onNavigateToNotes: (String) -> Unit,
    onNavigateToFollowUps: (String) -> Unit,
    onNavigateToConsultationMode: (String) -> Unit,
    onNavigateToSummary: (String) -> Unit,
    viewModel: ConsultationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showCarryOverDialog by remember { mutableStateOf(false) }

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

    if (showCarryOverDialog && uiState.unansweredQuestions.isNotEmpty()) {
        CarryOverQuestionsDialog(
            unansweredQuestions = uiState.unansweredQuestions,
            scheduledTargets = uiState.otherScheduledConsultations,
            onCopyToConsultation = { targetId, selectedIds ->
                showCarryOverDialog = false
                viewModel.copyQuestionsToConsultation(targetId, selectedIds) { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            },
            onCreateNewConsultation = {
                showCarryOverDialog = false
                onNavigateToCreateConsultation()
            },
            onDismiss = { showCarryOverDialog = false }
        )
    }

    ConsultationDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onEditClick = { onNavigateToEdit(viewModel.consultationIdString) },
        onDeleteClick = { showDeleteDialog = true },
        onManageQuestionsClick = { onNavigateToQuestions(viewModel.consultationIdString) },
        onManageNotesClick = { onNavigateToNotes(viewModel.consultationIdString) },
        onManageFollowUpsClick = { onNavigateToFollowUps(viewModel.consultationIdString) },
        onStartConsultationMode = { onNavigateToConsultationMode(viewModel.consultationIdString) },
        onOpenSummary = { onNavigateToSummary(viewModel.consultationIdString) },
        onOpenCarryOver = { showCarryOverDialog = true },
        onSetStatus = { status ->
            when (status) {
                ConsultationStatus.CANCELLED -> showCancelDialog = true
                ConsultationStatus.COMPLETED -> {
                    viewModel.setStatus(status)
                    if (uiState.unansweredQuestions.isNotEmpty()) {
                        showCarryOverDialog = true
                    }
                }
                else -> viewModel.setStatus(status)
            }
        },
        onToggleQuestion = viewModel::toggleQuestionDiscussed,
        onToggleFollowUp = viewModel::toggleFollowUpCompleted
    )
}

@Composable
fun ConsultationDetailContent(
    uiState: ConsultationDetailUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onManageQuestionsClick: () -> Unit,
    onManageNotesClick: () -> Unit,
    onManageFollowUpsClick: () -> Unit,
    onStartConsultationMode: () -> Unit,
    onOpenSummary: () -> Unit,
    onOpenCarryOver: () -> Unit,
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
                        IconButton(onClick = onOpenSummary) {
                            Icon(Icons.Default.Share, contentDescription = "Ringkasan & Bagikan")
                        }
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
                                DropdownMenuItem(
                                    text = { Text("Ringkasan & Bagikan") },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onOpenSummary()
                                    }
                                )
                                if (uiState.unansweredQuestions.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Bawa Pertanyaan ke Kunjungan Lain") },
                                        leadingIcon = { Icon(Icons.Default.Forward, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            onOpenCarryOver()
                                        }
                                    )
                                }
                                HorizontalDivider()
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
        snackbarHostState = snackbarHostState,
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
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
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

                    // Mode Konsultasi CTA (F-15)
                    if (uiState.questions.isNotEmpty()) {
                        Button(
                            onClick = onStartConsultationMode,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mulai Mode Konsultasi (${uiState.questions.size} Pertanyaan)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Section 1: Daftar Pertanyaan (dengan Jawaban & Lampiran per pertanyaan)
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
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                uiState.questions.take(4).forEach { q ->
                                    val qAttachments = uiState.attachmentsByQuestion[q.id].orEmpty()
                                    val photoCount = qAttachments.count { it.kind == AttachmentKind.PHOTO }
                                    val voiceCount = qAttachments.count { it.kind == AttachmentKind.VOICE }

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Checkbox(
                                                checked = q.isDiscussed,
                                                onCheckedChange = { onToggleQuestion(q.id, it) }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = q.text,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                if (q.isPriority || photoCount > 0 || voiceCount > 0) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        if (q.isPriority) PriorityBadge()
                                                        CompactAttachmentBadges(photoCount, voiceCount)
                                                    }
                                                }
                                            }
                                        }
                                        if (!q.answer.isNullOrBlank()) {
                                            Text(
                                                text = "Jawaban dicatat: ${q.answer}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 48.dp, top = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (uiState.questions.size > 4) {
                                    Text(
                                        text = "+${uiState.questions.size - 4} pertanyaan lainnya",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Section 2: Catatan Hasil Konsultasi (+ Lampiran Penjelasan Tambahan)
                    SectionCard(
                        title = "2. Hasil Konsultasi",
                        action = {
                            TextButton(onClick = onManageNotesClick) {
                                Text(if (uiState.note != null || uiState.additionalNoteAttachments.isNotEmpty()) "Ubah" else "Tulis")
                            }
                        }
                    ) {
                        val note = uiState.note
                        val extraAttachments = uiState.additionalNoteAttachments
                        if ((note == null || !note.isNotEmpty()) && extraAttachments.isEmpty()) {
                            Text(
                                text = stringResource(R.string.empty_notes_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (note != null && !note.summary.isNullOrBlank()) {
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
                                if (note != null && !note.providerInstruction.isNullOrBlank()) {
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
                                if (note != null && !note.additionalNotes.isNullOrBlank()) {
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
                                if (extraAttachments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Lampiran Penjelasan Tambahan:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    CompactAttachmentBadges(
                                        photoCount = extraAttachments.count { it.kind == AttachmentKind.PHOTO },
                                        voiceCount = extraAttachments.count { it.kind == AttachmentKind.VOICE }
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
