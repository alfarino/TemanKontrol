package id.temankontrol.app.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.domain.model.StatusFilter

@Composable
fun HistoryScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryContent(
        uiState = uiState,
        onFilterSelected = viewModel::setFilter,
        onSearchQueryChange = viewModel::setSearchQuery,
        onConsultationClick = onNavigateToDetail,
        onCreateClick = onNavigateToCreate
    )
}

@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    onFilterSelected: (StatusFilter) -> Unit,
    onConsultationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit = {},
    onCreateClick: () -> Unit = {}
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = "Jadwal & Riwayat Kunjungan"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_consultation))
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar (FR-078)
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Cari judul kunjungan atau fasilitas kesehatan…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Bersihkan pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(StatusFilter.entries) { filter ->
                    val filterLabel = when (filter) {
                        StatusFilter.ALL -> stringResource(R.string.filter_all)
                        StatusFilter.SCHEDULED -> stringResource(R.string.status_scheduled)
                        StatusFilter.COMPLETED -> stringResource(R.string.status_completed)
                        StatusFilter.CANCELLED -> stringResource(R.string.status_cancelled)
                    }
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filterLabel) }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    LoadingView(modifier = Modifier.fillMaxSize())
                }
                uiState.consultations.isEmpty() -> {
                    if (uiState.searchQuery.isNotBlank()) {
                        EmptyView(
                            title = "Tidak ditemukan kunjungan untuk \"${uiState.searchQuery}\".",
                            actionText = "Reset Pencarian",
                            onActionClick = { onSearchQueryChange("") },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (uiState.filter == StatusFilter.ALL) {
                        EmptyView(
                            title = stringResource(R.string.empty_history_title),
                            actionText = stringResource(R.string.action_add_consultation),
                            onActionClick = onCreateClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        EmptyView(
                            title = stringResource(R.string.empty_filter_title),
                            actionText = stringResource(R.string.action_show_all),
                            onActionClick = { onFilterSelected(StatusFilter.ALL) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.consultations, key = { it.id.value }) { summary ->
                            ConsultationCard(
                                summary = summary,
                                onClick = { onConsultationClick(summary.id.value) }
                            )
                        }
                    }
                }
            }
        }
    }
}
