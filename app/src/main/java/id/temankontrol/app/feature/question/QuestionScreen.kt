package id.temankontrol.app.feature.question

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.domain.model.Attachment
import id.temankontrol.app.domain.model.AttachmentKind
import id.temankontrol.app.domain.model.Question
import id.temankontrol.app.domain.model.SuggestionCategory
import id.temankontrol.app.ui.theme.StatusWarningDark
import id.temankontrol.app.ui.theme.StatusWarningLight
import kotlinx.coroutines.launch

@Composable
fun QuestionScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuestionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var questionToDelete by remember { mutableStateOf<Question?>(null) }

    if (questionToDelete != null) {
        val hasAnswer = !questionToDelete!!.answer.isNullOrBlank()
        val hasAttachments = uiState.attachmentsByQuestion[questionToDelete!!.id].orEmpty().isNotEmpty()
        val detailMsg = buildString {
            append("Apakah Anda yakin ingin menghapus pertanyaan ini?")
            if (hasAnswer || hasAttachments) {
                append(" Jawaban dan lampiran yang terhubung juga akan dihapus.")
            }
        }
        ConfirmDialog(
            title = "Hapus pertanyaan?",
            message = detailMsg,
            confirmButtonText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteQuestion(questionToDelete!!.id)
                questionToDelete = null
            },
            onDismiss = { questionToDelete = null }
        )
    }

    if (uiState.showAddDialog) {
        val editingAttachments = uiState.editingQuestion?.let {
            uiState.attachmentsByQuestion[it.id].orEmpty()
        }.orEmpty()

        QuestionFormDialog(
            consultationId = viewModel.consultationIdString,
            text = uiState.inputText,
            isPriority = uiState.inputIsPriority,
            answer = uiState.inputAnswer,
            isEditing = uiState.editingQuestion != null,
            errorMessage = uiState.inputError,
            showClosedHint = uiState.showClosedQuestionHint,
            editingAttachments = editingAttachments,
            onTextChange = viewModel::setInputText,
            onApplyStarter = viewModel::applyStarterChip,
            onDismissClosedHint = viewModel::dismissClosedQuestionHint,
            onPriorityChange = viewModel::setInputPriority,
            onAnswerChange = viewModel::setInputAnswer,
            onOpenSuggestions = viewModel::openSuggestionSheet,
            onAddPhoto = { caption, uri ->
                uiState.editingQuestion?.let { q -> viewModel.addPhotoToQuestion(q.id, caption, uri) }
            },
            onAddVoiceNote = { durationMs, caption, uri ->
                uiState.editingQuestion?.let { q -> viewModel.addVoiceNoteToQuestion(q.id, durationMs, caption, uri) }
            },
            onDeleteAttachment = viewModel::deleteAttachment,
            onUpdateAttachmentCaption = viewModel::updateAttachmentCaption,
            onSave = viewModel::saveQuestion,
            onDismiss = viewModel::closeDialog
        )
    }

    if (uiState.showSuggestionSheet) {
        SuggestionCatalogSheet(
            selectedCategory = uiState.selectedCategory,
            suggestions = uiState.suggestions,
            onSelectCategory = viewModel::selectCategory,
            onSelectSuggestion = viewModel::applySuggestion,
            onDismiss = viewModel::closeSuggestionSheet
        )
    }

    QuestionContent(
        uiState = uiState,
        consultationId = viewModel.consultationIdString,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onAddClick = viewModel::openAddDialog,
        onEditClick = viewModel::openEditDialog,
        onDeleteClick = { questionToDelete = it },
        onToggleDiscussed = { question, discussed ->
            viewModel.toggleDiscussed(question.id, discussed)
            if (discussed && question.answer.isNullOrBlank()) {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Pertanyaan ditandai sudah dibahas.",
                        actionLabel = "Catat Jawaban",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.openEditDialog(question.copy(isDiscussed = true))
                    }
                }
            }
        },
        onToggleAttachmentSection = viewModel::toggleAttachmentSection,
        onAddPhoto = viewModel::addPhotoToQuestion,
        onAddVoiceNote = viewModel::addVoiceNoteToQuestion,
        onDeleteAttachment = viewModel::deleteAttachment,
        onUpdateAttachmentCaption = viewModel::updateAttachmentCaption
    )
}

@Composable
fun QuestionContent(
    uiState: QuestionUiState,
    consultationId: String = "default",
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Question) -> Unit,
    onDeleteClick: (Question) -> Unit,
    onToggleDiscussed: (Question, Boolean) -> Unit,
    onToggleAttachmentSection: (String) -> Unit,
    onAddPhoto: (String, String?, String?) -> Unit,
    onAddVoiceNote: (String, Long, String?, String?) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onUpdateAttachmentCaption: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = "Daftar Pertanyaan",
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_question)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        modifier = modifier
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingView(modifier = Modifier.padding(padding))
            }
            uiState.questions.isEmpty() -> {
                EmptyView(
                    title = stringResource(R.string.empty_questions_title),
                    actionText = stringResource(R.string.action_add_question),
                    onActionClick = onAddClick,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // C-40 Info banner if list is long (> 5 questions)
                    if (uiState.questions.size > 5) {
                        item(key = "long_list_banner") {
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
                                    text = stringResource(R.string.banner_long_question_list),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = fg
                                )
                            }
                        }
                    }

                    items(uiState.questions, key = { it.id }) { q ->
                        val attachments = uiState.attachmentsByQuestion[q.id].orEmpty()
                        val isExpanded = q.id in uiState.expandedAttachmentQuestionIds || attachments.isNotEmpty()
                        val photoCount = attachments.count { it.kind == AttachmentKind.PHOTO }
                        val voiceCount = attachments.count { it.kind == AttachmentKind.VOICE }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = q.isDiscussed,
                                        onCheckedChange = { onToggleDiscussed(q, it) }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = q.text,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                textDecoration = if (q.isDiscussed) TextDecoration.LineThrough else null
                                            ),
                                            color = if (q.isDiscussed) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        if (q.isPriority || photoCount > 0 || voiceCount > 0) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (q.isPriority) PriorityBadge()
                                                CompactAttachmentBadges(photoCount = photoCount, voiceCount = voiceCount)
                                            }
                                        }
                                    }
                                    IconButton(onClick = { onToggleAttachmentSection(q.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.AttachFile,
                                            contentDescription = "Lampiran foto atau VN pertanyaan",
                                            tint = if (attachments.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onEditClick(q) }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Ubah",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteClick(q) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                // Recorded answer ("Jawaban yang Anda catat")
                                if (!q.answer.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Jawaban yang Anda catat",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = q.answer,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 4,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Per-question Photo & Voice Note attachments
                                AnimatedVisibility(visible = isExpanded) {
                                    Column {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        AttachmentSection(
                                            attachments = attachments,
                                            consultationId = consultationId,
                                            compact = true,
                                            emptyHint = "Lampirkan foto atau VN penjelasan untuk pertanyaan ini.",
                                            onAddPhoto = { caption, uri -> onAddPhoto(q.id, caption, uri) },
                                            onAddVoiceNote = { dur, caption, uri -> onAddVoiceNote(q.id, dur, caption, uri) },
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
    }
}

@Composable
private fun QuestionFormDialog(
    consultationId: String,
    text: String,
    isPriority: Boolean,
    answer: String,
    isEditing: Boolean,
    errorMessage: String?,
    showClosedHint: Boolean,
    editingAttachments: List<Attachment>,
    onTextChange: (String) -> Unit,
    onApplyStarter: (String) -> Unit,
    onDismissClosedHint: () -> Unit,
    onPriorityChange: (Boolean) -> Unit,
    onAnswerChange: (String) -> Unit,
    onOpenSuggestions: () -> Unit,
    onAddPhoto: (String?, String?) -> Unit,
    onAddVoiceNote: (Long, String?, String?) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onUpdateAttachmentCaption: (String, String?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val starterPrefixes = remember { listOf("Apa", "Mengapa", "Bagaimana", "Jelaskan", "Apa saja") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Ubah Pertanyaan" else "Tambah Pertanyaan")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Mulai dengan pertanyaan terbuka:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(starterPrefixes) { prefix ->
                        SuggestionChip(
                            onClick = { onApplyStarter(prefix) },
                            label = { Text("$prefix…") }
                        )
                    }
                }

                LabeledTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = "Teks Pertanyaan",
                    placeholder = "Tulis pertanyaan terbuka Anda...",
                    maxLength = 250,
                    singleLine = false,
                    minLines = 2,
                    errorMessage = errorMessage
                )

                // Soft non-blocking hint if question looks like yes/no
                if (showClosedHint) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.hint_closed_question),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = onDismissClosedHint) {
                                    Text("Tetap Gunakan")
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onOpenSuggestions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih dari Saran Pertanyaan Terbuka")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = isPriority,
                        onCheckedChange = onPriorityChange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Tandai sebagai Prioritas",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Pertanyaan prioritas tampil paling atas (tanpa batas jumlah)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LabeledTextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    label = "Jawaban yang Anda catat (Opsional)",
                    placeholder = "Catat penjelasan dari tenaga kesehatan...",
                    maxLength = 1000,
                    singleLine = false,
                    minLines = 2
                )

                if (isEditing) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    AttachmentSection(
                        attachments = editingAttachments,
                        consultationId = consultationId,
                        title = "Lampiran Pertanyaan (Foto & VN)",
                        emptyHint = "Belum ada lampiran foto atau VN pada pertanyaan ini.",
                        onAddPhoto = onAddPhoto,
                        onAddVoiceNote = onAddVoiceNote,
                        onDeleteAttachment = onDeleteAttachment,
                        onUpdateCaption = onUpdateAttachmentCaption
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionCatalogSheet(
    selectedCategory: SuggestionCategory,
    suggestions: List<id.temankontrol.app.domain.model.QuestionSuggestion>,
    onSelectCategory: (SuggestionCategory) -> Unit,
    onSelectSuggestion: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Katalog Pertanyaan Terbuka",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Template pertanyaan terbuka agar tenaga kesehatan memberi penjelasan lengkap",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryScrollableTabRow(
                selectedTabIndex = selectedCategory.ordinal,
                edgePadding = 0.dp
            ) {
                SuggestionCategory.entries.forEach { category ->
                    val title = when (category) {
                        SuggestionCategory.SYMPTOM -> "Gejala"
                        SuggestionCategory.EXAM -> "Pemeriksaan"
                        SuggestionCategory.TREATMENT -> "Perawatan"
                        SuggestionCategory.NEXT_STEP -> "Langkah Berikutnya"
                        SuggestionCategory.UNDERSTANDING -> "Pemahaman"
                    }
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { onSelectCategory(category) },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                suggestions.forEach { sug ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSuggestion(sug.text) }
                    ) {
                        Text(
                            text = sug.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
