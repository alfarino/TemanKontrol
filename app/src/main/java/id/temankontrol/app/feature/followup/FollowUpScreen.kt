package id.temankontrol.app.feature.followup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import id.temankontrol.app.domain.model.FollowUpAction
import id.temankontrol.app.domain.validation.Validation
import id.temankontrol.app.ui.theme.StatusWarningDark
import id.temankontrol.app.ui.theme.StatusWarningLight

@Composable
fun FollowUpScreen(
    onNavigateBack: () -> Unit,
    viewModel: FollowUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var actionToDelete by remember { mutableStateOf<FollowUpAction?>(null) }

    if (actionToDelete != null) {
        ConfirmDialog(
            title = "Hapus tindak lanjut?",
            message = "Tindak lanjut ini akan dihapus secara permanen.",
            confirmButtonText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAction(actionToDelete!!.id)
                actionToDelete = null
            },
            onDismiss = { actionToDelete = null }
        )
    }

    if (uiState.showAddDialog) {
        FollowUpFormDialog(
            text = uiState.inputText,
            dueDate = uiState.inputDueDate,
            isDueDatePast = uiState.isDueDatePast,
            isEditing = uiState.editingAction != null,
            errorMessage = uiState.inputError,
            onTextChanged = viewModel::onTextChanged,
            onDueDateChanged = viewModel::onDueDateChanged,
            onSave = viewModel::saveAction,
            onDismiss = viewModel::closeDialog
        )
    }

    FollowUpContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAddClick = viewModel::openAddDialog,
        onEditClick = viewModel::openEditDialog,
        onDeleteClick = { actionToDelete = it },
        onToggleCompleted = viewModel::toggleCompleted
    )
}

@Composable
fun FollowUpContent(
    uiState: FollowUpUiState,
    onNavigateBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (FollowUpAction) -> Unit,
    onDeleteClick: (FollowUpAction) -> Unit,
    onToggleCompleted: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = "Daftar Tindak Lanjut",
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_follow_up)) },
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
            uiState.actions.isEmpty() -> {
                EmptyView(
                    title = stringResource(R.string.empty_follow_ups_title),
                    actionText = stringResource(R.string.action_add_follow_up),
                    onActionClick = onAddClick,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                val openActions = uiState.actions.filter { !it.isCompleted }
                val completedActions = uiState.actions.filter { it.isCompleted }

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 10.dp,
                        bottom = padding.calculateBottomPadding() + 92.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (openActions.isNotEmpty()) {
                        item {
                            Text(
                                text = "Belum Selesai (${openActions.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(openActions, key = { it.id }) { action ->
                            FollowUpActionCard(
                                action = action,
                                isPastDue = Validation.isDueDatePast(action.dueDate),
                                onToggleCompleted = { onToggleCompleted(action.id, it) },
                                onEditClick = { onEditClick(action) },
                                onDeleteClick = { onDeleteClick(action) }
                            )
                        }
                    }

                    if (completedActions.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Selesai (${completedActions.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(completedActions, key = { it.id }) { action ->
                            FollowUpActionCard(
                                action = action,
                                isPastDue = false,
                                onToggleCompleted = { onToggleCompleted(action.id, it) },
                                onEditClick = { onEditClick(action) },
                                onDeleteClick = { onDeleteClick(action) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUpActionCard(
    action: FollowUpAction,
    isPastDue: Boolean,
    onToggleCompleted: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (action.isCompleted) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (action.isCompleted) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            ChecklistItem(
                title = action.text,
                checked = action.isCompleted,
                onCheckedChange = onToggleCompleted,
                dueDate = action.dueDate,
                isPastDue = isPastDue,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                trailingContent = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opsi tindak lanjut"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ubah") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FollowUpFormDialog(
    text: String,
    dueDate: java.time.LocalDate?,
    isDueDatePast: Boolean,
    isEditing: Boolean,
    errorMessage: String?,
    onTextChanged: (String) -> Unit,
    onDueDateChanged: (java.time.LocalDate?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Ubah Tindak Lanjut" else "Tambah Tindak Lanjut") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LabeledTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    label = "Aktivitas / Rencana Tindak Lanjut",
                    placeholder = "Contoh: Beli obat rutin, jadwalkan kontrol...",
                    maxLength = 250,
                    singleLine = false,
                    minLines = 2,
                    errorMessage = errorMessage
                )

                Spacer(modifier = Modifier.height(12.dp))

                DatePickerField(
                    selectedDate = dueDate,
                    onDateSelected = onDueDateChanged,
                    label = stringResource(R.string.label_due_date)
                )

                if (dueDate != null) {
                    TextButton(
                        onClick = { onDueDateChanged(null) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Hapus Tenggat")
                    }
                }

                if (isDueDatePast) {
                    val isDark = isSystemInDarkTheme()
                    val bg = if (isDark) StatusWarningDark.copy(alpha = 0.2f) else StatusWarningLight.copy(alpha = 0.15f)
                    val fg = if (isDark) StatusWarningDark else StatusWarningLight

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.banner_past_due_date),
                            style = MaterialTheme.typography.bodySmall,
                            color = fg
                        )
                    }
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
