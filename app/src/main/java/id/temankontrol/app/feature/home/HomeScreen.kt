package id.temankontrol.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import id.temankontrol.app.domain.validation.Validation
import id.temankontrol.app.ui.theme.StatusWarningDark
import id.temankontrol.app.ui.theme.StatusWarningLight

@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onConsultationClick = onNavigateToDetail,
        onCreateClick = onNavigateToCreate,
        onSeeAllConsultationsClick = onNavigateToHistory,
        onPastDueBannerClick = onNavigateToHistory,
        onSettingsClick = onNavigateToSettings,
        onToggleFollowUp = viewModel::toggleActionCompletion
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onConsultationClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onSeeAllConsultationsClick: () -> Unit,
    onPastDueBannerClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onToggleFollowUp: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_consultation)) },
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
            uiState.isTotalEmpty -> {
                EmptyView(
                    title = stringResource(R.string.empty_home_title),
                    actionText = stringResource(R.string.action_add_consultation),
                    onActionClick = onCreateClick,
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
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Section 3 (Kondisional): Banner Lewat Jadwal
                    if (uiState.pastDueCount > 0) {
                        item {
                            PastDueBanner(
                                count = uiState.pastDueCount,
                                onClick = onPastDueBannerClick
                            )
                        }
                    }

                    // Section 1: Kunjungan Mendatang
                    item {
                        SectionCard(
                            title = "Kunjungan Mendatang",
                            action = if (uiState.upcomingConsultations.isNotEmpty()) {
                                {
                                    TextButton(onClick = onSeeAllConsultationsClick) {
                                        Text(stringResource(R.string.action_see_all))
                                    }
                                }
                            } else null
                        ) {
                            if (uiState.upcomingConsultations.isEmpty()) {
                                Text(
                                    text = "Tidak ada kunjungan mendatang.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    uiState.upcomingConsultations.forEach { summary ->
                                        ConsultationCard(
                                            summary = summary,
                                            onClick = { onConsultationClick(summary.id.value) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Tindak Lanjut Terbuka
                    item {
                        SectionCard(
                            title = "Tindak Lanjut Terbuka"
                        ) {
                            if (uiState.openFollowUps.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.empty_follow_ups_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.openFollowUps.forEach { item ->
                                        val isPast = Validation.isDueDatePast(item.action.dueDate)
                                        ChecklistItem(
                                            title = item.action.text,
                                            checked = item.action.isCompleted,
                                            onCheckedChange = { checked ->
                                                onToggleFollowUp(item.action.id, checked)
                                            },
                                            subtitle = item.consultationTitle,
                                            dueDate = item.action.dueDate,
                                            isPastDue = isPast
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
private fun PastDueBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) StatusWarningDark.copy(alpha = 0.2f) else StatusWarningLight.copy(alpha = 0.15f)
    val fg = if (isDark) StatusWarningDark else StatusWarningLight

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Perlu diperbarui",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            Text(
                text = "$count kunjungan telah melewati tanggal jadwal. Buka riwayat untuk memperbarui status.",
                style = MaterialTheme.typography.bodySmall,
                color = fg
            )
        }
    }
}
