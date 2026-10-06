package id.temankontrol.app.feature.question

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.domain.model.Question
import id.temankontrol.app.domain.model.SuggestionCategory
import id.temankontrol.app.ui.theme.StatusWarningDark
import id.temankontrol.app.ui.theme.StatusWarningLight

@Composable
fun QuestionScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuestionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var questionToDelete by remember { mutableStateOf<Question?>(null) }

    if (questionToDelete != null) {
        ConfirmDialog(
            title = "Hapus pertanyaan?",
            message = "Apakah Anda yakin ingin menghapus pertanyaan ini?",
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
        QuestionFormDialog(
            text = uiState.inputText,
            isPriority = uiState.inputIsPriority,
            priorityCount = uiState.priorityCount,
            isEditing = uiState.editingQuestion != null,
            editingIsPriority = uiState.editingQuestion?.isPriority == true,
            errorMessage = uiState.inputError,
            onTextChange = viewModel::setInputText,
            onPriorityChange = viewModel::setInputPriority,
            onOpenSuggestions = viewModel::openSuggestionSheet,
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
        onNavigateBack = onNavigateBack,
        onAddClick = viewModel::openAddDialog,
        onEditClick = viewModel::openEditDialog,
        onDeleteClick = { questionToDelete = it },
        onToggleDiscussed = viewModel::toggleDiscussed
    )
}

@Composable
fun QuestionContent(
    uiState: QuestionUiState,
    onNavigateBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Question) -> Unit,
    onDeleteClick: (Question) -> Unit,
    onToggleDiscussed: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = "Daftar Pertanyaan",
                onNavigateBack = onNavigateBack
            )
        },
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
                    // C-40 Warning banner if list is long (> 5 questions)
                    if (uiState.questions.size > 5) {
                        item {
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
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Checkbox(
                                    checked = q.isDiscussed,
                                    onCheckedChange = { onToggleDiscussed(q.id, it) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = q.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (q.isPriority) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        PriorityBadge()
                                    }
                                }
                                IconButton(onClick = { onEditClick(q) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Ubah", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onDeleteClick(q) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
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
    text: String,
    isPriority: Boolean,
    priorityCount: Int,
    isEditing: Boolean,
    editingIsPriority: Boolean,
    errorMessage: String?,
    onTextChange: (String) -> Unit,
    onPriorityChange: (Boolean) -> Unit,
    onOpenSuggestions: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val canSetPriority = isPriority || (!editingIsPriority && priorityCount < 3) || (isEditing && editingIsPriority)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Ubah Pertanyaan" else "Tambah Pertanyaan")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LabeledTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = "Teks Pertanyaan",
                    placeholder = "Tulis pertanyaan Anda...",
                    maxLength = 250,
                    singleLine = false,
                    minLines = 2,
                    errorMessage = errorMessage
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = isPriority,
                        onCheckedChange = onPriorityChange,
                        enabled = canSetPriority
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tandai sebagai Prioritas (${priorityCount}/3)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenSuggestions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih dari Saran Pertanyaan")
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
                text = "Katalog Saran Pertanyaan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pilih template pertanyaan untuk disalin ke formulir",
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
                    .heightIn(max = 350.dp)
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
