package id.temankontrol.app.feature.consultation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import id.temankontrol.app.domain.model.Question
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
        onSaveInlineAnswer = { qId, answer ->
            viewModel.saveInlineAnswer(qId, answer)
            scope.launch { snackbarHostState.showSnackbar("Jawaban disimpan.") }
        },
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
    modifier: Modifier = Modifier,
    onSaveInlineAnswer: (String, String?) -> Unit = { _, _ -> }
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
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null) },
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
                val initialStep = remember(consultation.id.value, consultation.status) {
                    when (consultation.status) {
                        ConsultationStatus.COMPLETED -> 2
                        ConsultationStatus.CANCELLED -> 0
                        ConsultationStatus.SCHEDULED -> 0
                    }
                }
                var selectedStep by remember(consultation.id.value) { mutableIntStateOf(initialStep) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Header Info Kunjungan + Ringkasan Progres
                    ConsultationDetailHeaderCard(
                        uiState = uiState
                    )

                    // 2. Guided 3-Step Journey Stepper (FR-077, §32.5.B.2)
                    ConsultationJourneyStepper(
                        selectedStep = selectedStep,
                        questionCount = uiState.questions.size,
                        discussedCount = uiState.questions.count { it.isDiscussed },
                        completedFollowUpCount = uiState.followUps.count { it.isCompleted },
                        totalFollowUpCount = uiState.followUps.size,
                        onSelectStep = { selectedStep = it }
                    )

                    // 3. Step Workspace Content
                    when (selectedStep) {
                        0 -> StepOnePreparationSection(
                            uiState = uiState,
                            onManageQuestionsClick = onManageQuestionsClick,
                            onToggleQuestion = onToggleQuestion,
                            onNextStep = { selectedStep = 1 }
                        )

                        1 -> StepTwoInRoomSection(
                            uiState = uiState,
                            onStartConsultationMode = onStartConsultationMode,
                            onManageQuestionsClick = onManageQuestionsClick,
                            onManageNotesClick = onManageNotesClick,
                            onToggleQuestion = onToggleQuestion,
                            onSaveInlineAnswer = onSaveInlineAnswer,
                            onNextStep = { selectedStep = 2 }
                        )

                        else -> StepThreePostVisitSection(
                            uiState = uiState,
                            onManageNotesClick = onManageNotesClick,
                            onManageFollowUpsClick = onManageFollowUpsClick,
                            onToggleFollowUp = onToggleFollowUp,
                            onOpenSummary = onOpenSummary,
                            onOpenCarryOver = onOpenCarryOver,
                            onMarkCompleted = { onSetStatus(ConsultationStatus.COMPLETED) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ConsultationDetailHeaderCard(
    uiState: ConsultationDetailUiState
) {
    val consultation = uiState.consultation ?: return
    val discussedCount = uiState.questions.count { it.isDiscussed }
    val completedFollowUps = uiState.followUps.count { it.isCompleted }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
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
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = consultation.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = consultation.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!consultation.facilityName.isNullOrBlank() || !consultation.providerName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!consultation.facilityName.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = consultation.facilityName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!consultation.providerName.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = consultation.providerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricPill(
                    label = "Pertanyaan",
                    value = "${uiState.questions.size} siap"
                )
                MetricPill(
                    label = "Dibahas",
                    value = "$discussedCount/${uiState.questions.size}"
                )
                MetricPill(
                    label = "Tindak Lanjut",
                    value = "$completedFollowUps/${uiState.followUps.size}"
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConsultationJourneyStepper(
    selectedStep: Int,
    questionCount: Int,
    discussedCount: Int,
    completedFollowUpCount: Int,
    totalFollowUpCount: Int,
    onSelectStep: (Int) -> Unit
) {
    val steps = listOf(
        Triple(0, "1. Persiapan", "$questionCount pertanyaan"),
        Triple(1, "2. Saat Kontrol", "$discussedCount dibahas"),
        Triple(2, "3. Hasil & Aksi", "$completedFollowUpCount/$totalFollowUpCount selesai")
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        steps.forEach { (index, title, subtitle) ->
            val isSelected = selectedStep == index
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                tonalElevation = if (isSelected) 4.dp else 1.dp,
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectStep(index) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StepOnePreparationSection(
    uiState: ConsultationDetailUiState,
    onManageQuestionsClick: () -> Unit,
    onToggleQuestion: (String, Boolean) -> Unit,
    onNextStep: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Alarm status info banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AlarmOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alarm Pengingat Kunjungan Aktif",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Terjadwal otomatis pada H-1, 2 jam sebelum, 30 menit sebelum, dan tepat saat jam kunjungan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        SectionCard(
            title = "Daftar Pertanyaan Terbuka (${uiState.questions.size})",
            action = {
                TextButton(onClick = onManageQuestionsClick) {
                    Text("Kelola / + Saran")
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
                    uiState.questions.forEach { q ->
                        val qAttachments = uiState.attachmentsByQuestion[q.id].orEmpty()
                        val photoCount = qAttachments.count { it.kind == AttachmentKind.PHOTO }
                        val voiceCount = qAttachments.count { it.kind == AttachmentKind.VOICE }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Checkbox(
                                    checked = q.isDiscussed,
                                    onCheckedChange = { onToggleQuestion(q.id, it) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = q.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (q.isPriority || photoCount > 0 || voiceCount > 0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (q.isPriority) PriorityBadge()
                                            CompactAttachmentBadges(photoCount, voiceCount)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onManageQuestionsClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambah Pertanyaan / Lampirkan Foto & VN")
            }
        }

        OutlinedButton(
            onClick = onNextStep,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lanjut ke Tahap 2: Saat Kontrol (Ruang Periksa)")
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StepTwoInRoomSection(
    uiState: ConsultationDetailUiState,
    onStartConsultationMode: () -> Unit,
    onManageQuestionsClick: () -> Unit,
    onManageNotesClick: () -> Unit,
    onToggleQuestion: (String, Boolean) -> Unit,
    onSaveInlineAnswer: (String, String?) -> Unit,
    onNextStep: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Focus Consultation Mode Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Mode Konsultasi Fokus (Layar Penuh)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Tampilkan satu pertanyaan berukuran besar sekaligus saat sedang berdiskusi dengan tenaga kesehatan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Button(
                    onClick = if (uiState.questions.isNotEmpty()) onStartConsultationMode else onManageQuestionsClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.questions.isNotEmpty()) {
                            "Buka Mode Konsultasi (${uiState.questions.size} Pertanyaan)"
                        } else {
                            "Siapkan Pertanyaan Terlebih Dahulu"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Inline Question Discussion & Answer Recording
        SectionCard(
            title = "Catat Jawaban Langsung per Pertanyaan",
            action = {
                TextButton(onClick = onManageQuestionsClick) {
                    Text("+ Foto / VN")
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.questions.forEach { q ->
                        val qAttachments = uiState.attachmentsByQuestion[q.id].orEmpty()
                        InlineQuestionAnswerCard(
                            question = q,
                            photoCount = qAttachments.count { it.kind == AttachmentKind.PHOTO },
                            voiceCount = qAttachments.count { it.kind == AttachmentKind.VOICE },
                            onToggleDiscussed = { onToggleQuestion(q.id, it) },
                            onSaveAnswer = { answer -> onSaveInlineAnswer(q.id, answer) }
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onManageNotesClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Catatan & VN")
            }

            Button(
                onClick = onNextStep,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Tahap 3: Hasil")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun InlineQuestionAnswerCard(
    question: Question,
    photoCount: Int,
    voiceCount: Int,
    onToggleDiscussed: (Boolean) -> Unit,
    onSaveAnswer: (String?) -> Unit
) {
    var isEditingAnswer by remember(question.id) { mutableStateOf(false) }
    var draftAnswer by remember(question.id, question.answer) { mutableStateOf(question.answer.orEmpty()) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = question.isDiscussed,
                    onCheckedChange = onToggleDiscussed
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (question.isPriority || photoCount > 0 || voiceCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (question.isPriority) PriorityBadge()
                            CompactAttachmentBadges(photoCount, voiceCount)
                        }
                    }
                }
                TextButton(onClick = { isEditingAnswer = !isEditingAnswer }) {
                    Text(if (question.answer.isNullOrBlank()) "Catat" else "Ubah")
                }
            }

            if (isEditingAnswer) {
                OutlinedTextField(
                    value = draftAnswer,
                    onValueChange = { if (it.length <= 500) draftAnswer = it },
                    label = { Text("Jawaban yang Anda catat") },
                    placeholder = { Text("Tulis penjelasan dari tenaga kesehatan…") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        draftAnswer = question.answer.orEmpty()
                        isEditingAnswer = false
                    }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(onClick = {
                        onSaveAnswer(draftAnswer)
                        isEditingAnswer = false
                    }) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            } else if (!question.answer.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Jawaban dicatat: ${question.answer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepThreePostVisitSection(
    uiState: ConsultationDetailUiState,
    onManageNotesClick: () -> Unit,
    onManageFollowUpsClick: () -> Unit,
    onToggleFollowUp: (String, Boolean) -> Unit,
    onOpenSummary: () -> Unit,
    onOpenCarryOver: () -> Unit,
    onMarkCompleted: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Section Hasil Konsultasi (+ Lampiran Penjelasan Tambahan)
        SectionCard(
            title = "Hasil Konsultasi & Catatan Tambahan",
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
                            text = "Ringkasan Pembahasan:",
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

        // Section Tindak Lanjut
        SectionCard(
            title = "Checklist Tindak Lanjut (${uiState.followUps.count { it.isCompleted }}/${uiState.followUps.size})",
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
                    uiState.followUps.forEach { action ->
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
                }
            }
        }

        // Actions: Share PDF, Carry Over Unanswered Questions, Mark Completed
        Button(
            onClick = onOpenSummary,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Bagikan Ringkasan / Ekspor PDF", fontWeight = FontWeight.Bold)
        }

        if (uiState.unansweredQuestions.isNotEmpty()) {
            OutlinedButton(
                onClick = onOpenCarryOver,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bawa ${uiState.unansweredQuestions.size} Pertanyaan Belum Terjawab")
            }
        }

        if (uiState.consultation?.status == ConsultationStatus.SCHEDULED) {
            FilledTonalButton(
                onClick = onMarkCompleted,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tandai Kunjungan Selesai", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
