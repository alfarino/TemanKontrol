package id.temankontrol.app.feature.question

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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

        QuestionFormBottomSheet(
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
                title = "Kelola Pertanyaan (${uiState.questions.size})",
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
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
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
                        top = padding.calculateTopPadding() + 10.dp,
                        bottom = padding.calculateBottomPadding() + 92.dp
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
                                    .clip(RoundedCornerShape(12.dp))
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
                        QuestionManageCard(
                            question = q,
                            attachments = uiState.attachmentsByQuestion[q.id].orEmpty(),
                            isExpanded = q.id in uiState.expandedAttachmentQuestionIds ||
                                uiState.attachmentsByQuestion[q.id].orEmpty().isNotEmpty(),
                            consultationId = consultationId,
                            onToggleDiscussed = { onToggleDiscussed(q, !q.isDiscussed) },
                            onToggleAttachmentSection = { onToggleAttachmentSection(q.id) },
                            onEditClick = { onEditClick(q) },
                            onDeleteClick = { onDeleteClick(q) },
                            onAddPhoto = { caption, uri -> onAddPhoto(q.id, caption, uri) },
                            onAddVoiceNote = { dur, caption, uri -> onAddVoiceNote(q.id, dur, caption, uri) },
                            onDeleteAttachment = onDeleteAttachment,
                            onUpdateAttachmentCaption = onUpdateAttachmentCaption
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionManageCard(
    question: Question,
    attachments: List<Attachment>,
    isExpanded: Boolean,
    consultationId: String,
    onToggleDiscussed: () -> Unit,
    onToggleAttachmentSection: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddPhoto: (String?, String?) -> Unit,
    onAddVoiceNote: (Long, String?, String?) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onUpdateAttachmentCaption: (String, String?) -> Unit
) {
    var showCardMenu by remember { mutableStateOf(false) }
    val photoCount = attachments.count { it.kind == AttachmentKind.PHOTO }
    val voiceCount = attachments.count { it.kind == AttachmentKind.VOICE }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row: Badges + More Menu (full space for question text below!)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (question.isPriority) {
                            PriorityBadge()
                        }
                        FilterChip(
                            selected = question.isDiscussed,
                            onClick = onToggleDiscussed,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (question.isDiscussed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (question.isDiscussed) "Sudah dibahas" else "Belum dibahas",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showCardMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opsi pertanyaan"
                            )
                        }
                        DropdownMenu(
                            expanded = showCardMenu,
                            onDismissRequest = { showCardMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ubah Pertanyaan / Jawaban") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showCardMenu = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus Pertanyaan", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showCardMenu = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }

                // Full-width Question Text
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (question.isDiscussed) TextDecoration.LineThrough else null
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = if (question.isDiscussed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                // Recorded answer ("Jawaban yang Anda catat")
                if (!question.answer.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
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
                                text = question.answer,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Footer action strip
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onEditClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (question.answer.isNullOrBlank()) "Ubah / Jawaban" else "Ubah",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleAttachmentSection,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (photoCount + voiceCount > 0) {
                                "Lampiran (${photoCount + voiceCount})"
                            } else {
                                "+ Foto / VN"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }

                // Per-question Photo & Voice Note attachments
                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                        Spacer(modifier = Modifier.height(10.dp))
                        AttachmentSection(
                            attachments = attachments,
                            consultationId = consultationId,
                            compact = true,
                            emptyHint = "Lampirkan foto atau VN penjelasan untuk pertanyaan ini.",
                            onAddPhoto = onAddPhoto,
                            onAddVoiceNote = onAddVoiceNote,
                            onDeleteAttachment = onDeleteAttachment,
                            onUpdateCaption = onUpdateAttachmentCaption
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionFormBottomSheet(
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isEditing) "Ubah Pertanyaan" else "Tambah Pertanyaan Baru",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Mulai dengan kata tanya terbuka:",
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
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pilih dari 20 Saran Pertanyaan Terbuka")
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tandai sebagai Prioritas",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tampil paling atas (tanpa batas jumlah)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isPriority,
                        onCheckedChange = onPriorityChange
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
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
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tip: Setelah pertanyaan disimpan, ketuk tombol \"+ Foto / VN\" di kartu pertanyaan untuk menambahkan lampiran.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 24.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.4f).height(48.dp)
                ) {
                    Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectSuggestion(sug.text) }
                    ) {
                        Text(
                            text = sug.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
