package id.temankontrol.app.feature.consultation.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.domain.model.Attachment
import id.temankontrol.app.domain.model.AttachmentKind
import id.temankontrol.app.domain.model.AttachmentTarget
import id.temankontrol.app.domain.model.ConsultationStatus
import id.temankontrol.app.domain.model.Question
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.launch
import java.time.Instant

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
        consultationId = viewModel.consultationIdString,
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
        onAddQuestionPhoto = { qId, caption, uri ->
            viewModel.addPhotoAttachment(qId, AttachmentTarget.QUESTION, caption, uri)
        },
        onAddQuestionVoice = { qId, dur, caption, uri ->
            viewModel.addVoiceAttachment(qId, AttachmentTarget.QUESTION, dur, caption, uri)
        },
        onAddNotePhoto = { caption, uri ->
            viewModel.addPhotoAttachment(null, AttachmentTarget.ADDITIONAL_NOTE, caption, uri)
        },
        onAddNoteVoice = { dur, caption, uri ->
            viewModel.addVoiceAttachment(null, AttachmentTarget.ADDITIONAL_NOTE, dur, caption, uri)
        },
        onDeleteAttachment = viewModel::deleteAttachment,
        onUpdateAttachmentCaption = viewModel::updateAttachmentCaption,
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
    consultationId: String = "default",
    onSaveInlineAnswer: (String, String?) -> Unit = { _, _ -> },
    onAddQuestionPhoto: (String, String?, String?) -> Unit = { _, _, _ -> },
    onAddQuestionVoice: (String, Long, String?, String?) -> Unit = { _, _, _, _ -> },
    onAddNotePhoto: (String?, String?) -> Unit = { _, _ -> },
    onAddNoteVoice: (Long, String?, String?) -> Unit = { _, _, _ -> },
    onDeleteAttachment: (Attachment) -> Unit = {},
    onUpdateAttachmentCaption: (String, String?) -> Unit = { _, _ -> }
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
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Header Info Kunjungan (Ringkas & Bersih)
                    ConsultationDetailHeaderCard(uiState = uiState)

                    // 2. Guided 3-Step Journey Stepper (Anti-terpotong & Informatif)
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
                            consultationId = consultationId,
                            onManageQuestionsClick = onManageQuestionsClick,
                            onAddQuestionPhoto = onAddQuestionPhoto,
                            onAddQuestionVoice = onAddQuestionVoice,
                            onDeleteAttachment = onDeleteAttachment,
                            onUpdateAttachmentCaption = onUpdateAttachmentCaption,
                            onNextStep = { selectedStep = 1 }
                        )

                        1 -> StepTwoInRoomSection(
                            uiState = uiState,
                            consultationId = consultationId,
                            onStartConsultationMode = onStartConsultationMode,
                            onManageQuestionsClick = onManageQuestionsClick,
                            onToggleQuestion = onToggleQuestion,
                            onSaveInlineAnswer = onSaveInlineAnswer,
                            onAddQuestionPhoto = onAddQuestionPhoto,
                            onAddQuestionVoice = onAddQuestionVoice,
                            onDeleteAttachment = onDeleteAttachment,
                            onUpdateAttachmentCaption = onUpdateAttachmentCaption,
                            onPreviousStep = { selectedStep = 0 },
                            onNextStep = { selectedStep = 2 }
                        )

                        else -> StepThreePostVisitSection(
                            uiState = uiState,
                            consultationId = consultationId,
                            onManageNotesClick = onManageNotesClick,
                            onManageFollowUpsClick = onManageFollowUpsClick,
                            onToggleFollowUp = onToggleFollowUp,
                            onAddNotePhoto = onAddNotePhoto,
                            onAddNoteVoice = onAddNoteVoice,
                            onDeleteAttachment = onDeleteAttachment,
                            onUpdateAttachmentCaption = onUpdateAttachmentCaption,
                            onOpenSummary = onOpenSummary,
                            onOpenCarryOver = onOpenCarryOver,
                            onMarkCompleted = { onSetStatus(ConsultationStatus.COMPLETED) },
                            onPreviousStep = { selectedStep = 1 }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
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
    val isPastDue = consultation.status == ConsultationStatus.SCHEDULED &&
        consultation.appointmentAt.isBefore(Instant.now())

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusChip(
                        status = consultation.status,
                        isPastDue = isPastDue
                    )
                    Text(
                        text = DateUtils.formatRelativeDateTime(consultation.appointmentAt),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = consultation.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (consultation.reason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = consultation.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!consultation.facilityName.isNullOrBlank() || !consultation.providerName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!consultation.facilityName.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = consultation.facilityName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!consultation.providerName.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = consultation.providerName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
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
        Triple(0, "Persiapan", if (questionCount == 0) "Belum ada" else "$questionCount siap"),
        Triple(1, "Saat Kontrol", if (questionCount == 0) "0 tanya" else "$discussedCount/$questionCount bahas"),
        Triple(2, "Hasil & Aksi", "$completedFollowUpCount/$totalFollowUpCount aksi")
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
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                tonalElevation = if (isSelected) 3.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectStep(index) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.primaryContainer
                            )
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
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
    consultationId: String,
    onManageQuestionsClick: () -> Unit,
    onAddQuestionPhoto: (String, String?, String?) -> Unit,
    onAddQuestionVoice: (String, Long, String?, String?) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onUpdateAttachmentCaption: (String, String?) -> Unit,
    onNextStep: () -> Unit
) {
    val consultation = uiState.consultation ?: return
    val isFutureScheduled = consultation.status == ConsultationStatus.SCHEDULED &&
        consultation.appointmentAt.isAfter(Instant.now())
    var expandedQuestionIds by remember { mutableStateOf(emptySet<String>()) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Dynamic Alarm / Status Context Banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = when {
                isFutureScheduled -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                consultation.status == ConsultationStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    imageVector = when {
                        isFutureScheduled -> Icons.Default.AlarmOn
                        consultation.status == ConsultationStatus.COMPLETED -> Icons.Default.CheckCircle
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isFutureScheduled -> "Siapkan Pertanyaan Sebelum Bertemu Dokter"
                            consultation.status == ConsultationStatus.COMPLETED -> "Kunjungan Sudah Selesai"
                            consultation.status == ConsultationStatus.CANCELLED -> "Kunjungan Dibatalkan"
                            else -> "Jadwal Sudah Lewat Waktu"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            isFutureScheduled -> "Alarm terjadwal sesuai setelan (H-1, 2 jam, 30 menit, & jam kontrol). Susun pertanyaan terbuka dan lampirkan foto/VN pendukung."
                            consultation.status == ConsultationStatus.COMPLETED -> "Anda dapat membuka Tahap 3: Hasil & Aksi untuk melihat ringkasan catatan dan tindak lanjut."
                            else -> "Atur ulang jadwal atau perbarui status kunjungan bila kontrol sudah terlaksana."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SectionCard(
            title = "Daftar Pertanyaan (${uiState.questions.size})",
            action = if (uiState.questions.isNotEmpty()) {
                {
                    FilledTonalButton(
                        onClick = onManageQuestionsClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kelola")
                    }
                }
            } else null
        ) {
            if (uiState.questions.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_questions_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onManageQuestionsClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Pertanyaan Pertama")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.questions.forEachIndexed { idx, q ->
                        val qAttachments = uiState.attachmentsByQuestion[q.id].orEmpty()
                        val photoCount = qAttachments.count { it.kind == AttachmentKind.PHOTO }
                        val voiceCount = qAttachments.count { it.kind == AttachmentKind.VOICE }
                        val isExpanded = q.id in expandedQuestionIds

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (q.isPriority) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (q.isPriority) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = q.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (q.isPriority || photoCount > 0 || voiceCount > 0) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (q.isPriority) PriorityBadge()
                                                CompactAttachmentBadges(photoCount, voiceCount)
                                            }
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = {
                                            expandedQuestionIds = if (isExpanded) {
                                                expandedQuestionIds - q.id
                                            } else {
                                                expandedQuestionIds + q.id
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.AttachFile,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isExpanded) "Tutup Lampiran" else {
                                                val total = qAttachments.size
                                                if (total > 0) "Lihat Lampiran ($total)" else "+ Foto / VN"
                                            },
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AttachmentSection(
                                            attachments = qAttachments,
                                            consultationId = consultationId,
                                            compact = true,
                                            emptyHint = "Lampirkan foto hasil lab/obat atau rekaman suara untuk pertanyaan ini.",
                                            onAddPhoto = { caption, uri -> onAddQuestionPhoto(q.id, caption, uri) },
                                            onAddVoiceNote = { dur, caption, uri -> onAddQuestionVoice(q.id, dur, caption, uri) },
                                            onDeleteAttachment = onDeleteAttachment,
                                            onUpdateCaption = onUpdateAttachmentCaption
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onNextStep,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Lanjut ke Tahap 2: Saat Kontrol", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StepTwoInRoomSection(
    uiState: ConsultationDetailUiState,
    consultationId: String,
    onStartConsultationMode: () -> Unit,
    onManageQuestionsClick: () -> Unit,
    onToggleQuestion: (String, Boolean) -> Unit,
    onSaveInlineAnswer: (String, String?) -> Unit,
    onAddQuestionPhoto: (String, String?, String?) -> Unit,
    onAddQuestionVoice: (String, Long, String?, String?) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onUpdateAttachmentCaption: (String, String?) -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Focus Consultation Mode Hero Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mode Konsultasi Fokus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "Cocok dipakai di ruang periksa: tampilkan satu pertanyaan berukuran besar sekaligus agar mudah dibaca bersama tenaga kesehatan.",
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
                            "Buka Layar Fokus (${uiState.questions.size} Pertanyaan)"
                        } else {
                            "Tambah Pertanyaan Terlebih Dahulu"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Inline Question Discussion, Answer Recording & Per-Question Attachments
        SectionCard(
            title = "Cek & Catat Jawaban Cepat (${uiState.questions.count { it.isDiscussed }}/${uiState.questions.size})",
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.questions.forEach { q ->
                        val qAttachments = uiState.attachmentsByQuestion[q.id].orEmpty()
                        InlineQuestionAnswerCard(
                            question = q,
                            attachments = qAttachments,
                            consultationId = consultationId,
                            onToggleDiscussed = { onToggleQuestion(q.id, it) },
                            onSaveAnswer = { answer -> onSaveInlineAnswer(q.id, answer) },
                            onAddPhoto = { caption, uri -> onAddQuestionPhoto(q.id, caption, uri) },
                            onAddVoice = { dur, caption, uri -> onAddQuestionVoice(q.id, dur, caption, uri) },
                            onDeleteAttachment = onDeleteAttachment,
                            onUpdateAttachmentCaption = onUpdateAttachmentCaption
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
                onClick = onPreviousStep,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tahap 1")
            }

            Button(
                onClick = onNextStep,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
            ) {
                Text("Tahap 3: Hasil & Aksi", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun InlineQuestionAnswerCard(
    question: Question,
    attachments: List<Attachment>,
    consultationId: String,
    onToggleDiscussed: (Boolean) -> Unit,
    onSaveAnswer: (String?) -> Unit,
    onAddPhoto: (String?, String?) -> Unit,
    onAddVoice: (Long, String?, String?) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onUpdateAttachmentCaption: (String, String?) -> Unit
) {
    var isEditingAnswer by remember(question.id) { mutableStateOf(false) }
    var showAttachments by remember(question.id) { mutableStateOf(false) }
    var draftAnswer by remember(question.id, question.answer) { mutableStateOf(question.answer.orEmpty()) }
    val photoCount = attachments.count { it.kind == AttachmentKind.PHOTO }
    val voiceCount = attachments.count { it.kind == AttachmentKind.VOICE }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = question.isDiscussed,
                    onCheckedChange = onToggleDiscussed
                )
                Column(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (question.isDiscussed) TextDecoration.LineThrough else null
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = if (question.isDiscussed) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (question.isPriority || photoCount > 0 || voiceCount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (question.isPriority) PriorityBadge()
                            CompactAttachmentBadges(photoCount, voiceCount)
                        }
                    }
                }
            }

            if (!isEditingAnswer && !question.answer.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Jawaban yang Anda catat:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = question.answer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (isEditingAnswer) {
                OutlinedTextField(
                    value = draftAnswer,
                    onValueChange = { if (it.length <= 1000) draftAnswer = it },
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
            }

            // Tidy card action row: Catat Jawaban + Lampiran Foto/VN
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { isEditingAnswer = !isEditingAnswer },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (question.answer.isNullOrBlank()) "Catat Jawaban" else "Ubah Jawaban",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = { showAttachments = !showAttachments },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (showAttachments) Icons.Default.ExpandLess else Icons.Default.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (attachments.isNotEmpty()) "Foto / VN (${attachments.size})" else "+ Foto / VN",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }

            AnimatedVisibility(visible = showAttachments) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                    Spacer(modifier = Modifier.height(8.dp))
                    AttachmentSection(
                        attachments = attachments,
                        consultationId = consultationId,
                        compact = true,
                        emptyHint = "Tambahkan foto atau rekam jawaban suara untuk pertanyaan ini.",
                        onAddPhoto = onAddPhoto,
                        onAddVoiceNote = onAddVoice,
                        onDeleteAttachment = onDeleteAttachment,
                        onUpdateCaption = onUpdateAttachmentCaption
                    )
                }
            }
        }
    }
}

@Composable
private fun StepThreePostVisitSection(
    uiState: ConsultationDetailUiState,
    consultationId: String,
    onManageNotesClick: () -> Unit,
    onManageFollowUpsClick: () -> Unit,
    onToggleFollowUp: (String, Boolean) -> Unit,
    onAddNotePhoto: (String?, String?) -> Unit,
    onAddNoteVoice: (Long, String?, String?) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onUpdateAttachmentCaption: (String, String?) -> Unit,
    onOpenSummary: () -> Unit,
    onOpenCarryOver: () -> Unit,
    onMarkCompleted: () -> Unit,
    onPreviousStep: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Section Hasil Konsultasi (+ Inline Playable/Viewable Lampiran Penjelasan Tambahan)
        SectionCard(
            title = "Hasil & Catatan Konsultasi",
            action = {
                FilledTonalButton(
                    onClick = onManageNotesClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (uiState.note != null && uiState.note.isNotEmpty()) "Ubah" else "Tulis")
                }
            }
        ) {
            val note = uiState.note
            val extraAttachments = uiState.additionalNoteAttachments
            if (note == null || !note.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.empty_notes_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!note.summary.isNullOrBlank()) {
                        NoteSummaryBlock(
                            label = "Ringkasan Pembahasan",
                            body = note.summary
                        )
                    }
                    if (!note.providerInstruction.isNullOrBlank()) {
                        NoteSummaryBlock(
                            label = "Arahan Tenaga Kesehatan",
                            body = note.providerInstruction
                        )
                    }
                    if (!note.additionalNotes.isNullOrBlank()) {
                        NoteSummaryBlock(
                            label = "Catatan Tambahan",
                            body = note.additionalNotes
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            Spacer(modifier = Modifier.height(12.dp))

            AttachmentSection(
                attachments = extraAttachments,
                consultationId = consultationId,
                title = "Lampiran Penjelasan Tambahan (Foto & VN)",
                emptyHint = "Tambahkan foto dokumen atau rekaman suara penjelasan tambahan dari tenaga medis.",
                onAddPhoto = onAddNotePhoto,
                onAddVoiceNote = onAddNoteVoice,
                onDeleteAttachment = onDeleteAttachment,
                onUpdateCaption = onUpdateAttachmentCaption
            )
        }

        // Section Tindak Lanjut
        SectionCard(
            title = "Tindak Lanjut (${uiState.followUps.count { it.isCompleted }}/${uiState.followUps.size})",
            action = {
                FilledTonalButton(
                    onClick = onManageFollowUpsClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
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
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onManageFollowUpsClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Rencana Tindak Lanjut")
                }
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

        // Action Card: Share PDF, Carry Over Unanswered Questions, Mark Completed, Back Step
        SectionCard(title = "Tindakan Akhir") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (uiState.consultation?.status == ConsultationStatus.SCHEDULED) {
                    Button(
                        onClick = onMarkCompleted,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tandai Kunjungan Selesai", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onOpenSummary,
                    colors = if (uiState.consultation?.status == ConsultationStatus.SCHEDULED) {
                        ButtonDefaults.filledTonalButtonColors()
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan Ringkasan / Ekspor PDF", fontWeight = FontWeight.Bold)
                }

                if (uiState.unansweredQuestions.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onOpenCarryOver,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bawa ${uiState.unansweredQuestions.size} Pertanyaan Belum Terjawab")
                    }
                }

                OutlinedButton(
                    onClick = onPreviousStep,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kembali ke Tahap 2: Saat Kontrol")
                }
            }
        }
    }
}

@Composable
private fun NoteSummaryBlock(
    label: String,
    body: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
