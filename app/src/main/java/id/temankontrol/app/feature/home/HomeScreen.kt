package id.temankontrol.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.R
import id.temankontrol.app.core.designsystem.component.*
import id.temankontrol.app.domain.model.ConsultationSummary
import id.temankontrol.app.domain.validation.Validation
import id.temankontrol.app.ui.theme.*
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToQuestions: (String) -> Unit = {},
    onNavigateToConsultationMode: (String) -> Unit = {},
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
        onRemindersClick = onNavigateToReminders,
        onBackupClick = onNavigateToBackup,
        onManageQuestionsClick = onNavigateToQuestions,
        onStartConsultationMode = onNavigateToConsultationMode,
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
    onRemindersClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onManageQuestionsClick: (String) -> Unit = {},
    onStartConsultationMode: (String) -> Unit = {},
    onToggleFollowUp: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        val hour = LocalTime.now().hour
        when {
            hour in 4..10 -> "Selamat Pagi"
            hour in 11..14 -> "Selamat Siang"
            hour in 15..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    AppScaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_consultation)) },
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
            uiState.isTotalEmpty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardGreetingHeader(
                        greeting = greeting,
                        accountSession = uiState.accountSession,
                        onCloudSyncPillClick = onSettingsClick
                    )
                    BentoQuickActionsGrid(
                        onCreateSchedule = onCreateClick,
                        onPrepareQuestions = onCreateClick,
                        onOpenAlarmCenter = onRemindersClick,
                        onOpenBackupSync = onBackupClick
                    )
                    EmptyView(
                        title = stringResource(R.string.empty_home_title),
                        actionText = stringResource(R.string.action_add_consultation),
                        onActionClick = onCreateClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            else -> {
                val heroConsultation = uiState.upcomingConsultations.firstOrNull()
                val remainingConsultations = if (uiState.upcomingConsultations.size > 1) {
                    uiState.upcomingConsultations.drop(1)
                } else {
                    emptyList()
                }

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 12.dp,
                        bottom = padding.calculateBottomPadding() + 92.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Top Greeting & Cloud Sync Pill (FR-076)
                    item {
                        DashboardGreetingHeader(
                            greeting = greeting,
                            accountSession = uiState.accountSession,
                            onCloudSyncPillClick = onSettingsClick
                        )
                    }

                    // 2. Conditional Past-Due Warning Banner
                    if (uiState.pastDueCount > 0) {
                        item {
                            PastDueBanner(
                                count = uiState.pastDueCount,
                                onClick = onPastDueBannerClick
                            )
                        }
                    }

                    // 3. Hero Card "Kunjungan Terdekat" with Countdown & Readiness Bar (FR-076)
                    item {
                        if (heroConsultation != null) {
                            HeroUpcomingConsultationCard(
                                summary = heroConsultation,
                                onCardClick = { onConsultationClick(heroConsultation.id.value) },
                                onStartConsultationMode = { onStartConsultationMode(heroConsultation.id.value) },
                                onAddQuestionClick = { onManageQuestionsClick(heroConsultation.id.value) }
                            )
                        } else {
                            NoUpcomingHeroCard(onCreateClick = onCreateClick)
                        }
                    }

                    // 4. Bento 2x2 Quick Actions Grid (FR-076)
                    item {
                        BentoQuickActionsGrid(
                            onCreateSchedule = onCreateClick,
                            onPrepareQuestions = {
                                if (heroConsultation != null) {
                                    onManageQuestionsClick(heroConsultation.id.value)
                                } else {
                                    onCreateClick()
                                }
                            },
                            onOpenAlarmCenter = onRemindersClick,
                            onOpenBackupSync = onBackupClick
                        )
                    }

                    // 5. Additional Upcoming Consultations (if > 1)
                    if (remainingConsultations.isNotEmpty()) {
                        item {
                            SectionCard(
                                title = "Jadwal Berikutnya",
                                action = {
                                    TextButton(onClick = onSeeAllConsultationsClick) {
                                        Text(stringResource(R.string.action_see_all))
                                    }
                                }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    remainingConsultations.forEach { summary ->
                                        ConsultationCard(
                                            summary = summary,
                                            onClick = { onConsultationClick(summary.id.value) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Tindak Lanjut Prioritas Card
                    item {
                        SectionCard(
                            title = "Tindak Lanjut Prioritas",
                            action = if (uiState.openFollowUps.isNotEmpty()) {
                                {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${uiState.openFollowUps.size} terbuka",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            } else null
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
private fun DashboardGreetingHeader(
    greeting: String,
    accountSession: id.temankontrol.app.domain.model.AccountSession?,
    onCloudSyncPillClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (accountSession != null) "$greeting, ${accountSession.displayName}" else greeting,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Pendamping Kontrol Kesehatan Anda",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onCloudSyncPillClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (accountSession != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (accountSession != null) "Tersinkron • Cloud" else "Mode Lokal • Login",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun HeroUpcomingConsultationCard(
    summary: ConsultationSummary,
    onCardClick: () -> Unit,
    onStartConsultationMode: () -> Unit,
    onAddQuestionClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val gradientStart = if (isDark) HeroTealStartDark else HeroTealStartLight
    val gradientEnd = if (isDark) HeroTealEndDark else HeroTealEndLight
    val accentCoral = if (isDark) AccentCoralDark else AccentCoralLight

    val countdownLabel = remember(summary.appointmentAt) {
        val now = Instant.now()
        val diffHours = Duration.between(now, summary.appointmentAt).toHours()
        when {
            summary.isPastDue || diffHours < 0 -> "Perlu Diperbarui"
            diffHours < 1 -> "Kurang dari 1 Jam Lagi"
            diffHours < 24 -> "Dalam $diffHours Jam Lagi"
            else -> {
                val days = (diffHours / 24).coerceAtLeast(1)
                if (days == 1L) "Besok" else "$days Hari Lagi"
            }
        }
    }

    // Readiness score: 40% for schedule + alarm active, +60% scaled by prepared questions (target >= 3)
    val questionReadiness = (summary.questionCount.coerceAtMost(3) / 3f) * 0.6f
    val readinessFraction = (0.4f + questionReadiness).coerceIn(0.2f, 1f)
    val readinessPercent = (readinessFraction * 100).toInt()

    Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top badges: KUNJUNGAN TERDEKAT + Live Countdown Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "KUNJUNGAN TERDEKAT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentCoral
                ) {
                    Text(
                        text = countdownLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Title & Date/Facility
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(DateUtils.formatRelativeDateTime(summary.appointmentAt))
                        if (!summary.facilityName.isNullOrBlank()) {
                            append(" • ")
                            append(summary.facilityName)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.90f)
                )
            }

            // Readiness Progress Bar Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.18f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (summary.questionCount > 0) {
                                "Kesiapan Kontrol: ${summary.questionCount} Pertanyaan • Alarm Aktif"
                            } else {
                                "Kesiapan Kontrol: Belum ada pertanyaan disiapkan"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$readinessPercent%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    LinearProgressIndicator(
                        progress = { readinessFraction },
                        color = accentCoral,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }

            // Direct Action Buttons inside Hero Card
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onStartConsultationMode,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = gradientStart
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mode Konsultasi",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                FilledTonalButton(
                    onClick = onAddQuestionClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.20f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.QuestionAnswer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ Pertanyaan",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun NoUpcomingHeroCard(
    onCreateClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val gradientStart = if (isDark) HeroTealStartDark else HeroTealStartLight
    val gradientEnd = if (isDark) HeroTealEndDark else HeroTealEndLight

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Siapkan Kunjungan Berikutnya",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Buat jadwal kontrol, pilih pertanyaan terbuka, dan aktifkan alarm pengingat agar tidak ada yang terlewat saat bertemu dokter.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = gradientStart
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Buat Jadwal Kontrol Baru", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BentoQuickActionsGrid(
    onCreateSchedule: () -> Unit,
    onPrepareQuestions: () -> Unit,
    onOpenAlarmCenter: () -> Unit,
    onOpenBackupSync: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val coralBg = if (isDark) AccentCoralContainerDark else AccentCoralContainerLight
    val coralFg = if (isDark) AccentCoralDark else AccentCoralLight

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Aksi Cepat",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BentoActionTile(
                icon = Icons.Default.AddCircleOutline,
                title = "Buat Jadwal",
                subtitle = "Kontrol baru",
                iconBg = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onCreateSchedule,
                modifier = Modifier.weight(1f)
            )
            BentoActionTile(
                icon = Icons.Default.TipsAndUpdates,
                title = "Pertanyaan",
                subtitle = "20 saran terbuka",
                iconBg = coralBg,
                iconTint = coralFg,
                onClick = onPrepareQuestions,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BentoActionTile(
                icon = Icons.Default.Alarm,
                title = "Alarm & Tes",
                subtitle = "Pengingat nyaring",
                iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = onOpenAlarmCenter,
                modifier = Modifier.weight(1f)
            )
            BentoActionTile(
                icon = Icons.Default.Backup,
                title = "Cadangan",
                subtitle = ".tkbackup & Akun",
                iconBg = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.secondary,
                onClick = onOpenBackupSync,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BentoActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(14.dp)
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
                text = "Perlu diperbarui ($count kunjungan)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            Text(
                text = "Ada jadwal kunjungan yang telah lewat. Ketuk untuk menandai selesai atau memperbarui status.",
                style = MaterialTheme.typography.bodySmall,
                color = fg
            )
        }
    }
}
