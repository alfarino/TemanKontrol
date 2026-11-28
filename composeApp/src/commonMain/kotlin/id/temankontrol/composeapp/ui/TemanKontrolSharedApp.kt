package id.temankontrol.composeapp.ui

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.temankontrol.composeapp.data.KmpActionOutcome
import id.temankontrol.composeapp.data.KmpAppStore
import id.temankontrol.composeapp.domain.*
import id.temankontrol.composeapp.platform.*
import id.temankontrol.shared.dto.SyncAttachmentMetaDto
import id.temankontrol.shared.dto.SyncConsultationDto
import id.temankontrol.shared.dto.SyncQuestionDto
import kotlinx.coroutines.launch

object SharedThemeTokens {
    val HeroTealStart = Color(0xFF0B6E72)
    val HeroTealEnd = Color(0xFF14919B)
    val AccentCoral = Color(0xFFF97316)
    val PorcelainMint = Color(0xFFF3F8F8)
    val DeepInk = Color(0xFF141D1E)
    val SubtleBorder = Color(0x24141D1E)
}

sealed interface KmpRoute {
    data object Home : KmpRoute
    data object History : KmpRoute
    data object Settings : KmpRoute
    data object ReminderSettings : KmpRoute
    data object Auth : KmpRoute
    data object AccountSync : KmpRoute
    data object Backup : KmpRoute
    data object About : KmpRoute
    data class ConsultationForm(val editId: String? = null) : KmpRoute
    data class ConsultationDetail(val consultationId: String, val initialStep: Int = 1) : KmpRoute
    data class QuestionManager(val consultationId: String) : KmpRoute
    data class ConsultationMode(val consultationId: String) : KmpRoute
    data class NoteEditor(val consultationId: String) : KmpRoute
    data class FollowUpManager(val consultationId: String) : KmpRoute
    data class SummaryPreview(val consultationId: String) : KmpRoute
}

@Composable
fun TemanKontrolSharedPreviewApp(
    onTriggerTestAlarm: (() -> Unit)? = null
) {
    val store = remember { KmpAppStore(autoStartSyncLoop = true) }
    TemanKontrolCrossPlatformApp(
        store = store,
        biometricAuth = remember { createPlatformBiometricAuth() },
        alarmScheduler = remember { createPlatformAlarmScheduler() },
        mediaActions = remember { createPlatformMediaActions() },
        shareBridge = remember { createPlatformShareBridge() },
        onTriggerTestAlarm = onTriggerTestAlarm
    )
}

@Composable
fun TemanKontrolCrossPlatformApp(
    store: KmpAppStore,
    biometricAuth: PlatformBiometricAuth = remember { createPlatformBiometricAuth() },
    alarmScheduler: PlatformAlarmScheduler = remember { createPlatformAlarmScheduler() },
    mediaActions: PlatformMediaActions = remember { createPlatformMediaActions() },
    shareBridge: PlatformShareBridge = remember { createPlatformShareBridge() },
    isLockedExternal: Boolean = false,
    onUnlockExternal: () -> Unit = {},
    onTriggerTestAlarm: (() -> Unit)? = null
) {
    val state by store.state.collectAsState()
    val backStack = remember { mutableStateListOf<KmpRoute>(KmpRoute.Home) }
    val currentRoute = backStack.lastOrNull() ?: KmpRoute.Home

    var isLocked by remember { mutableStateOf(state.appLockEnabled && biometricAuth.canAuthenticate()) }
    LaunchedEffect(isLockedExternal) {
        if (isLockedExternal && state.appLockEnabled) {
            isLocked = true
        }
    }

    fun navigate(route: KmpRoute) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = SharedThemeTokens.HeroTealStart,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD5EFF0),
            onPrimaryContainer = Color(0xFF073E40),
            secondary = SharedThemeTokens.AccentCoral,
            secondaryContainer = Color(0xFFE5F2F2),
            onSecondaryContainer = SharedThemeTokens.DeepInk,
            background = SharedThemeTokens.PorcelainMint,
            onBackground = SharedThemeTokens.DeepInk,
            surface = Color.White,
            onSurface = SharedThemeTokens.DeepInk,
            surfaceVariant = Color(0xFFE7F0F0),
            onSurfaceVariant = Color(0xFF4B5C5E)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SharedThemeTokens.PorcelainMint
        ) {
            when {
                isLocked -> {
                    KmpLockScreen(
                        onUnlockClick = {
                            biometricAuth.authenticate("Buka Kunci Teman Kontrol") { outcome ->
                                if (outcome == BiometricOutcome.AUTHENTICATED || outcome == BiometricOutcome.UNAVAILABLE) {
                                    isLocked = false
                                    onUnlockExternal()
                                }
                            }
                        }
                    )
                }
                !state.noticeAccepted -> {
                    KmpNoticeScreen(
                        onAccept = { store.acceptNotice() }
                    )
                }
                else -> {
                    when (val r = currentRoute) {
                        KmpRoute.Home -> KmpHomeScreen(
                            state = state,
                            onNavigate = ::navigate
                        )
                        KmpRoute.History -> KmpHistoryScreen(
                            state = state,
                            onBack = ::navigateBack,
                            onOpenDetail = { id -> navigate(KmpRoute.ConsultationDetail(id)) }
                        )
                        is KmpRoute.ConsultationForm -> KmpConsultationFormScreen(
                            state = state,
                            editId = r.editId,
                            onBack = ::navigateBack,
                            onSave = { title, atMillis, reason, fac, prov ->
                                store.saveConsultation(r.editId, title, atMillis, reason, fac, prov)
                            },
                            onSaved = { id ->
                                navigateBack()
                                if (r.editId == null) {
                                    navigate(KmpRoute.ConsultationDetail(id, 1))
                                }
                            }
                        )
                        is KmpRoute.ConsultationDetail -> KmpConsultationDetailScreen(
                            state = state,
                            consultationId = r.consultationId,
                            initialStep = r.initialStep,
                            store = store,
                            mediaActions = mediaActions,
                            onBack = ::navigateBack,
                            onNavigate = ::navigate,
                            onDeleted = { navigateBack() }
                        )
                        is KmpRoute.QuestionManager -> KmpQuestionManagerScreen(
                            state = state,
                            consultationId = r.consultationId,
                            store = store,
                            mediaActions = mediaActions,
                            onBack = ::navigateBack
                        )
                        is KmpRoute.ConsultationMode -> KmpConsultationModeScreen(
                            state = state,
                            consultationId = r.consultationId,
                            store = store,
                            mediaActions = mediaActions,
                            onBack = ::navigateBack
                        )
                        is KmpRoute.NoteEditor -> KmpNoteEditorScreen(
                            state = state,
                            consultationId = r.consultationId,
                            store = store,
                            mediaActions = mediaActions,
                            onBack = ::navigateBack
                        )
                        is KmpRoute.FollowUpManager -> KmpFollowUpScreen(
                            state = state,
                            consultationId = r.consultationId,
                            store = store,
                            onBack = ::navigateBack
                        )
                        is KmpRoute.SummaryPreview -> KmpSummaryPreviewScreen(
                            state = state,
                            consultationId = r.consultationId,
                            store = store,
                            shareBridge = shareBridge,
                            onBack = ::navigateBack
                        )
                        KmpRoute.Settings -> KmpSettingsScreen(
                            state = state,
                            biometricAuth = biometricAuth,
                            store = store,
                            onBack = ::navigateBack,
                            onNavigate = ::navigate
                        )
                        KmpRoute.ReminderSettings -> KmpReminderSettingsScreen(
                            state = state,
                            store = store,
                            alarmScheduler = alarmScheduler,
                            onTriggerTestAlarm = onTriggerTestAlarm,
                            onBack = ::navigateBack
                        )
                        KmpRoute.Auth -> KmpAuthScreen(
                            store = store,
                            onBack = ::navigateBack,
                            onAuthSuccess = {
                                backStack.clear()
                                backStack.add(KmpRoute.Home)
                            }
                        )
                        KmpRoute.AccountSync -> KmpAccountSyncScreen(
                            state = state,
                            store = store,
                            onBack = ::navigateBack,
                            onNavigateToLogin = { navigate(KmpRoute.Auth) },
                            onNavigateToBackup = { navigate(KmpRoute.Backup) }
                        )
                        KmpRoute.Backup -> KmpBackupScreen(
                            state = state,
                            store = store,
                            shareBridge = shareBridge,
                            onBack = ::navigateBack
                        )
                        KmpRoute.About -> KmpAboutScreen(onBack = ::navigateBack)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KmpTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = SharedThemeTokens.DeepInk
        )
    )
}

@Composable
fun KmpSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SharedThemeTokens.SubtleBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SharedThemeTokens.DeepInk,
                        modifier = Modifier.weight(1f)
                    )
                    if (actionText != null && onActionClick != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onActionClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(actionText, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                content()
            }
        }
    }
}

@Composable
fun KmpLockScreen(onUnlockClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {
        Surface(shape = CircleShape, color = Color(0xFFD5EFF0), modifier = Modifier.size(84.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = SharedThemeTokens.HeroTealStart, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Teman Kontrol Terkunci", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Verifikasi Face ID / Sandi Perangkat untuk melindungi catatan kesehatan pribadi Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onUnlockClick, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Buka Kunci", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun KmpNoticeScreen(onAccept: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(SharedThemeTokens.HeroTealStart, SharedThemeTokens.HeroTealEnd)))
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                    Text("Selamat Datang di Teman Kontrol", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Pendamping persiapan konsultasi kesehatan Anda (${platformName()}).", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                }
            }
            KmpSectionCard(title = "Penting untuk Diketahui") {
                Text("1. Teman Kontrol bukan alat diagnosis medis, bukan layanan telemedicine, dan bukan layanan gawat darurat.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("2. Seluruh data tersimpan offline-first di perangkat Anda dan dapat disinkronkan secara aman ke akun cloud Supabase bila Anda memilih masuk.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("3. Selaras dengan UU PDP No. 27 Tahun 2022, Anda berhak mengekspor cadangan maupun menghapus akun beserta seluruh data secara permanen kapan saja.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAccept,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Saya Mengerti & Lanjutkan", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun KmpHomeScreen(
    state: KmpLocalDatabaseState,
    onNavigate: (KmpRoute) -> Unit
) {
    val now = remember { currentTimeMillis() }
    val scheduled = state.consultations.filter { it.status == "SCHEDULED" }.sortedBy { it.appointmentAtMillis }
    val upcoming = scheduled.firstOrNull { it.appointmentAtMillis >= now - 86_400_000L } ?: scheduled.firstOrNull()
    val openFollowUpsCount = state.followUps.count { !it.isCompleted }

    Scaffold(
        containerColor = SharedThemeTokens.PorcelainMint,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(KmpRoute.ConsultationForm(null)) },
                containerColor = SharedThemeTokens.HeroTealStart,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kunjungan")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.accountSession != null) "Halo, ${state.accountSession.displayName}" else "Teman Kontrol",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Siapkan konsultasi lebih tenang & terstruktur",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFE5F2F2),
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable {
                        onNavigate(if (state.accountSession != null) KmpRoute.AccountSync else KmpRoute.Auth)
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Icon(
                            imageVector = if (state.accountSession != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = SharedThemeTokens.HeroTealStart,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.accountSession != null) "Tersinkron • Cloud" else "Mode Lokal • Login",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (upcoming != null) {
                val qCount = state.questions.count { it.consultationId == upcoming.id }
                val discussed = state.questions.count { it.consultationId == upcoming.id && it.isDiscussed }
                Card(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(KmpRoute.ConsultationDetail(upcoming.id)) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(SharedThemeTokens.HeroTealStart, SharedThemeTokens.HeroTealEnd)))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.2f)) {
                                Text(
                                    text = KmpDateUtils.formatRelative(upcoming.appointmentAtMillis, now),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("$discussed/$qCount terjawab", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                        }
                        Text(upcoming.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(
                            text = "${KmpDateUtils.formatDateTime(upcoming.appointmentAtMillis)}${upcoming.facilityName?.let { " • $it" }.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onNavigate(KmpRoute.ConsultationMode(upcoming.id)) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SharedThemeTokens.HeroTealStart),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Mode Konsultasi", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onNavigate(KmpRoute.QuestionManager(upcoming.id)) },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+ Pertanyaan")
                            }
                        }
                    }
                }
            } else {
                KmpSectionCard(title = "Belum Ada Jadwal Terdekat") {
                    Text("Tambahkan jadwal kontrol kesehatan Anda berikutnya agar pertanyaan dan alarm pengingat siap sebelum masuk ruang periksa.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { onNavigate(KmpRoute.ConsultationForm(null)) }, shape = RoundedCornerShape(12.dp)) {
                        Text("Buat Jadwal Kunjungan")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SharedThemeTokens.SubtleBorder), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("${scheduled.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = SharedThemeTokens.HeroTealStart)
                        Text("Jadwal Aktif", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SharedThemeTokens.SubtleBorder), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("$openFollowUpsCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = SharedThemeTokens.AccentCoral)
                        Text("Tindak Lanjut Aktif", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            KmpSectionCard(title = "Daftar Kunjungan Terjadwal", actionText = "Riwayat", onActionClick = { onNavigate(KmpRoute.History) }) {
                if (scheduled.isEmpty()) {
                    Text("Belum ada jadwal konsultasi aktif.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    scheduled.take(5).forEachIndexed { index, c ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SharedThemeTokens.SubtleBorder)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onNavigate(KmpRoute.ConsultationDetail(c.id)) }.padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(KmpDateUtils.formatDateTime(c.appointmentAtMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onNavigate(KmpRoute.ReminderSettings) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pengingat")
                }
                OutlinedButton(onClick = { onNavigate(KmpRoute.Settings) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pengaturan")
                }
            }
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun KmpHistoryScreen(
    state: KmpLocalDatabaseState,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    var filter by remember { mutableStateOf("ALL") }
    var search by remember { mutableStateOf("") }

    val filtered = state.consultations.filter { c ->
        (filter == "ALL" || c.status == filter) &&
            (search.isBlank() || c.title.contains(search, ignoreCase = true) || c.reason.contains(search, ignoreCase = true))
    }.sortedByDescending { it.appointmentAtMillis }

    Scaffold(topBar = { KmpTopBar("Riwayat Kunjungan", onBack) }, containerColor = SharedThemeTokens.PorcelainMint) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Cari judul atau keluhan…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "Semua", "SCHEDULED" to "Terjadwal", "COMPLETED" to "Selesai", "CANCELLED" to "Batal").forEach { (key, label) ->
                    FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) })
                }
            }
            if (filtered.isEmpty()) {
                KmpSectionCard(title = "Tidak Ada Kunjungan") {
                    Text("Belum ada kunjungan pada filter ini.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                filtered.forEach { c ->
                    KmpSectionCard(title = c.title) {
                        Text(KmpDateUtils.formatDateTime(c.appointmentAtMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Status: ${statusLabel(c.status)}", style = MaterialTheme.typography.labelMedium, color = SharedThemeTokens.HeroTealStart)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { onOpenDetail(c.id) }, shape = RoundedCornerShape(12.dp)) {
                            Text("Lihat Detail")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KmpConsultationFormScreen(
    state: KmpLocalDatabaseState,
    editId: String?,
    onBack: () -> Unit,
    onSave: (title: String, appointmentAtMillis: Long, reason: String, facility: String?, provider: String?) -> KmpActionOutcome<String>,
    onSaved: (String) -> Unit
) {
    val existing = editId?.let { id -> state.consultations.find { it.id == id } }
    val initMillis = existing?.appointmentAtMillis ?: (currentTimeMillis() + 86_400_000L)
    val initParts = remember(initMillis) { KmpDateUtils.toLocalParts(initMillis) }

    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var reason by remember { mutableStateOf(existing?.reason.orEmpty()) }
    var facility by remember { mutableStateOf(existing?.facilityName.orEmpty()) }
    var provider by remember { mutableStateOf(existing?.providerName.orEmpty()) }
    var year by remember { mutableStateOf(initParts.year) }
    var month by remember { mutableStateOf(initParts.month) }
    var day by remember { mutableStateOf(initParts.day) }
    var hour by remember { mutableStateOf(initParts.hour) }
    var minute by remember { mutableStateOf(initParts.minute) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { KmpTopBar(if (editId == null) "Jadwal Kunjungan Baru" else "Ubah Kunjungan", onBack) },
        containerColor = SharedThemeTokens.PorcelainMint
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (errorMsg != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
            KmpSectionCard(title = "Informasi Kunjungan") {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Kunjungan*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Alasan / Keluhan Utama*") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = facility, onValueChange = { facility = it }, label = { Text("Fasilitas Kesehatan (Opsional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Nama Dokter / Nakes (Opsional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            KmpSectionCard(title = "Tanggal & Waktu Kontrol") {
                Text("Terpilih: ${KmpDateUtils.formatDateTime(KmpDateUtils.fromLocalParts(year, month, day, hour, minute))}", fontWeight = FontWeight.Bold, color = SharedThemeTokens.HeroTealStart)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val shifted = KmpDateUtils.toLocalParts(KmpDateUtils.fromLocalParts(year, month, day, hour, minute) + 86_400_000L)
                        year = shifted.year; month = shifted.month; day = shifted.day
                    }, modifier = Modifier.weight(1f)) { Text("+1 Hari") }
                    OutlinedButton(onClick = {
                        val shifted = KmpDateUtils.toLocalParts(KmpDateUtils.fromLocalParts(year, month, day, hour, minute) - 86_400_000L)
                        year = shifted.year; month = shifted.month; day = shifted.day
                    }, modifier = Modifier.weight(1f)) { Text("-1 Hari") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { hour = (hour + 1) % 24 }, modifier = Modifier.weight(1f)) { Text("Jam: ${hour.toString().padStart(2, '0')} (+)") }
                    OutlinedButton(onClick = { minute = (minute + 15) % 60 }, modifier = Modifier.weight(1f)) { Text("Menit: ${minute.toString().padStart(2, '0')} (+)") }
                }
            }
            Button(
                onClick = {
                    val atMillis = KmpDateUtils.fromLocalParts(year, month, day, hour, minute)
                    when (val res = onSave(title, atMillis, reason, facility, provider)) {
                        is KmpActionOutcome.Ok -> onSaved(res.data)
                        is KmpActionOutcome.Err -> errorMsg = res.message
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Simpan Jadwal", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun KmpConsultationDetailScreen(
    state: KmpLocalDatabaseState,
    consultationId: String,
    initialStep: Int,
    store: KmpAppStore,
    mediaActions: PlatformMediaActions,
    onBack: () -> Unit,
    onNavigate: (KmpRoute) -> Unit,
    onDeleted: () -> Unit
) {
    val consultation = state.consultations.find { it.id == consultationId }
    if (consultation == null) {
        Scaffold(topBar = { KmpTopBar("Detail Kunjungan", onBack) }) { p ->
            Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text("Kunjungan tidak ditemukan.")
            }
        }
        return
    }

    var currentStep by remember { mutableStateOf(initialStep.coerceIn(1, 3)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCarryOverDialog by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val questions = state.questions.filter { it.consultationId == consultationId }
        .sortedWith(compareByDescending<SyncQuestionDto> { it.isPriority }.thenBy { it.createdAtMillis })
    val note = state.notes.find { it.consultationId == consultationId }
    val followUps = state.followUps.filter { it.consultationId == consultationId }
    val attachments = state.attachments.filter { it.consultationId == consultationId }
    val additionalNoteAttachments = attachments.filter { it.target == "ADDITIONAL_NOTE" }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Kunjungan?") },
            text = { Text("Seluruh pertanyaan, catatan, tindak lanjut, dan lampiran pada kunjungan ini akan ikut terhapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        store.deleteConsultation(consultationId)
                        onDeleted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } }
        )
    }

    if (showCarryOverDialog) {
        val targets = state.consultations.filter { it.id != consultationId && it.status == "SCHEDULED" }
        AlertDialog(
            onDismissRequest = { showCarryOverDialog = false },
            title = { Text("Bawa Pertanyaan Belum Terjawab") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (targets.isEmpty()) {
                        Text("Belum ada jadwal kunjungan berikutnya yang berstatus Terjadwal.")
                    } else {
                        Text("Pilih jadwal tujuan untuk menyalin pertanyaan yang belum dibahas:")
                        targets.forEach { target ->
                            OutlinedButton(
                                onClick = {
                                    val moved = store.carryOverUnfinishedQuestions(consultationId, target.id)
                                    showCarryOverDialog = false
                                    infoMessage = "$moved pertanyaan disalin ke ${target.title}."
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(target.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCarryOverDialog = false }) { Text("Tutup") } }
        )
    }

    Scaffold(
        topBar = {
            KmpTopBar(
                title = "Detail Kunjungan",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onNavigate(KmpRoute.ConsultationForm(consultationId)) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Ubah")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus")
                    }
                }
            )
        },
        containerColor = SharedThemeTokens.PorcelainMint
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (infoMessage != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(infoMessage!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }

            // Header Ringkas v3.0
            ElevatedCard(shape = RoundedCornerShape(18.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SharedThemeTokens.SubtleBorder)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(statusLabel(consultation.status), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(KmpDateUtils.formatDateTime(consultation.appointmentAtMillis), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(consultation.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(consultation.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!consultation.facilityName.isNullOrBlank() || !consultation.providerName.isNullOrBlank()) {
                            Text(
                                listOfNotNull(consultation.facilityName, consultation.providerName).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = SharedThemeTokens.HeroTealStart,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 3-Step Journey Bar
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SharedThemeTokens.SubtleBorder), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1 to "Persiapan", 2 to "Saat Kontrol", 3 to "Hasil & Aksi").forEach { (step, label) ->
                        val selected = currentStep == step
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) SharedThemeTokens.HeroTealStart else Color.Transparent,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { currentStep = step }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                            ) {
                                Text("$step. $label", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (selected) Color.White else SharedThemeTokens.DeepInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            when (currentStep) {
                1 -> {
                    val isScheduled = consultation.status == "SCHEDULED"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isScheduled) MaterialTheme.colorScheme.primaryContainer else Color(0xFFE7F0F0),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isScheduled) "Pengingat alarm aktif sebelum kontrol. Susun pertanyaan penting Anda terlebih dahulu."
                            else "Status kunjungan: ${statusLabel(consultation.status)}. Anda tetap dapat meninjau pertanyaan persiapan.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    KmpSectionCard(
                        title = "Daftar Pertanyaan (${questions.size})",
                        actionText = "Kelola",
                        onActionClick = { onNavigate(KmpRoute.QuestionManager(consultationId)) }
                    ) {
                        if (questions.isEmpty()) {
                            Text("Belum ada pertanyaan. Ketuk 'Kelola' untuk menambahkan dari template atau tulis sendiri.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            questions.forEachIndexed { idx, q ->
                                val qAtts = attachments.filter { it.questionId == q.id }
                                var expandAtt by remember(q.id) { mutableStateOf(false) }
                                if (idx > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SharedThemeTokens.SubtleBorder)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${idx + 1}.", fontWeight = FontWeight.Bold, color = SharedThemeTokens.HeroTealStart)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(q.text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (q.isPriority) {
                                            AssistChip(onClick = { store.toggleQuestionPriority(q.id) }, label = { Text("Prioritas") })
                                        }
                                        TextButton(onClick = { expandAtt = !expandAtt }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                            Text(if (qAtts.isEmpty()) "+ Foto / VN" else "Lampiran (${qAtts.size})")
                                        }
                                    }
                                    AnimatedVisibility(expandAtt) {
                                        KmpAttachmentSection(
                                            attachments = qAtts,
                                            consultationId = consultationId,
                                            questionId = q.id,
                                            target = "QUESTION",
                                            store = store,
                                            mediaActions = mediaActions
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { currentStep = 2 },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Lanjut ke Tahap 2: Saat Kontrol", fontWeight = FontWeight.Bold)
                    }
                }
                2 -> {
                    ElevatedCard(shape = RoundedCornerShape(18.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFD5EFF0)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Mode Konsultasi Fokus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Tampilkan satu per satu pertanyaan dengan tombol rekam jawaban suara & foto langsung di ruang dokter.", style = MaterialTheme.typography.bodySmall)
                                Button(onClick = { onNavigate(KmpRoute.ConsultationMode(consultationId)) }, shape = RoundedCornerShape(12.dp)) {
                                    Text("Mulai Mode Konsultasi", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    KmpSectionCard(title = "Catat Jawaban Cepat") {
                        if (questions.isEmpty()) {
                            Text("Belum ada pertanyaan. Tambahkan dulu di Tahap 1.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            questions.forEachIndexed { idx, q ->
                                var draftAnswer by remember(q.id, q.answer) { mutableStateOf(q.answer.orEmpty()) }
                                var editing by remember(q.id) { mutableStateOf(false) }
                                val qAtts = attachments.filter { it.questionId == q.id }
                                if (idx > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SharedThemeTokens.SubtleBorder)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = q.isDiscussed, onCheckedChange = { store.setQuestionDiscussed(q.id, it) })
                                        Text(q.text, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    }
                                    if (!q.answer.isNullOrBlank() && !editing) {
                                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF3F8F8), modifier = Modifier.fillMaxWidth()) {
                                            Text("Jawaban: ${q.answer}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                                        }
                                    }
                                    if (editing) {
                                        OutlinedTextField(value = draftAnswer, onValueChange = { draftAnswer = it }, label = { Text("Jawaban / Penjelasan Dokter") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = {
                                                store.saveQuestionAnswer(q.id, draftAnswer)
                                                editing = false
                                            }) { Text("Simpan") }
                                            TextButton(onClick = { editing = false }) { Text("Batal") }
                                        }
                                    } else {
                                        TextButton(onClick = { editing = true }) {
                                            Text(if (q.answer.isNullOrBlank()) "+ Catat Jawaban" else "Ubah Jawaban")
                                        }
                                    }
                                    KmpAttachmentSection(
                                        attachments = qAtts,
                                        consultationId = consultationId,
                                        questionId = q.id,
                                        target = "QUESTION",
                                        store = store,
                                        mediaActions = mediaActions
                                    )
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { currentStep = 1 }, modifier = Modifier.weight(1f)) { Text("Tahap 1") }
                        Button(onClick = { currentStep = 3 }, modifier = Modifier.weight(1f)) { Text("Tahap 3: Hasil") }
                    }
                }
                else -> {
                    KmpSectionCard(title = "Catatan Hasil Kunjungan", actionText = "Ubah", onActionClick = { onNavigate(KmpRoute.NoteEditor(consultationId)) }) {
                        if (note == null || (note.summary.isNullOrBlank() && note.providerInstruction.isNullOrBlank() && note.additionalNotes.isNullOrBlank())) {
                            Text("Belum ada catatan ringkasan dokter.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            if (!note.summary.isNullOrBlank()) Text("Ringkasan: ${note.summary}", style = MaterialTheme.typography.bodySmall)
                            if (!note.providerInstruction.isNullOrBlank()) Text("Instruksi: ${note.providerInstruction}", style = MaterialTheme.typography.bodySmall)
                            if (!note.additionalNotes.isNullOrBlank()) Text("Catatan Tambahan: ${note.additionalNotes}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        KmpAttachmentSection(
                            attachments = additionalNoteAttachments,
                            consultationId = consultationId,
                            questionId = null,
                            target = "ADDITIONAL_NOTE",
                            store = store,
                            mediaActions = mediaActions
                        )
                    }

                    KmpSectionCard(title = "Tindak Lanjut (${followUps.size})", actionText = "Kelola", onActionClick = { onNavigate(KmpRoute.FollowUpManager(consultationId)) }) {
                        if (followUps.isEmpty()) {
                            OutlinedButton(onClick = { onNavigate(KmpRoute.FollowUpManager(consultationId)) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Tambah Rencana Tindak Lanjut")
                            }
                        } else {
                            followUps.forEach { f ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Checkbox(checked = f.isCompleted, onCheckedChange = { store.toggleFollowUpCompleted(f.id) })
                                    Text(
                                        text = f.text,
                                        textDecoration = if (f.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    KmpSectionCard(title = "Tindakan Akhir") {
                        if (consultation.status != "COMPLETED") {
                            Button(
                                onClick = {
                                    store.setConsultationStatus(consultationId, "COMPLETED")
                                    infoMessage = "Kunjungan ditandai selesai."
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Tandai Kunjungan Selesai") }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedButton(
                            onClick = { onNavigate(KmpRoute.SummaryPreview(consultationId)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Bagikan Ringkasan / Ekspor PDF") }
                        val unfinished = questions.count { !it.isDiscussed }
                        if (unfinished > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { showCarryOverDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Bawa $unfinished Pertanyaan Belum Terjawab")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = { currentStep = 2 }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Kembali ke Tahap 2")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KmpAttachmentSection(
    attachments: List<SyncAttachmentMetaDto>,
    consultationId: String,
    questionId: String?,
    target: String,
    store: KmpAppStore,
    mediaActions: PlatformMediaActions
) {
    var isRecording by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var mediaMsg by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    mediaActions.capturePhoto { picked ->
                        if (picked != null) store.addAttachment(consultationId, questionId, target, "PHOTO", picked)
                        else mediaMsg = "Tidak ada foto diambil."
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kamera", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = {
                    mediaActions.pickPhoto { picked ->
                        if (picked != null) store.addAttachment(consultationId, questionId, target, "PHOTO", picked)
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Galeri", style = MaterialTheme.typography.labelSmall)
            }
            if (!isRecording) {
                OutlinedButton(
                    onClick = {
                        val started = mediaActions.startVoiceRecording { err -> mediaMsg = err }
                        if (started) isRecording = true
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rekam VN", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Button(
                    onClick = {
                        isRecording = false
                        mediaActions.stopVoiceRecording { picked ->
                            if (picked != null) store.addAttachment(consultationId, questionId, target, "VOICE", picked)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop VN", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (mediaMsg != null) {
            Text(mediaMsg!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        attachments.forEach { att ->
            var captionText by remember(att.id, att.caption) { mutableStateOf(att.caption.orEmpty()) }
            var editCaption by remember(att.id) { mutableStateOf(false) }

            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF3F8F8), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (att.kind == "VOICE") Icons.Default.Mic else Icons.Default.Image,
                            contentDescription = null,
                            tint = SharedThemeTokens.HeroTealStart,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(att.caption ?: att.fileName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${KmpDateUtils.formatFileSize(att.sizeBytes)}${if (att.kind == "VOICE") " • ${KmpDateUtils.formatDurationMs(att.durationMs)}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (att.kind == "VOICE") {
                            IconButton(onClick = {
                                if (playingId == att.id) {
                                    mediaActions.stopVoice()
                                    playingId = null
                                } else {
                                    val bytes = store.readAttachmentBytes(att)
                                    if (bytes != null) {
                                        playingId = att.id
                                        mediaActions.playVoice(bytes, att.fileName) { playingId = null }
                                    } else {
                                        mediaMsg = "File audio sedang dipulihkan dari cloud."
                                    }
                                }
                            }) {
                                Icon(if (playingId == att.id) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = "Putar")
                            }
                        }
                        IconButton(onClick = { editCaption = !editCaption }) {
                            Icon(Icons.Default.Edit, contentDescription = "Judul Lampiran", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { store.deleteAttachment(att.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                        }
                    }
                    if (editCaption) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = captionText, onValueChange = { captionText = it }, label = { Text("Keterangan") }, singleLine = true, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                store.updateAttachmentCaption(att.id, captionText)
                                editCaption = false
                            }) { Text("Simpan") }
                        }
                    }
                }
            }
        }
    }
}

fun statusLabel(status: String): String = when (status) {
    "COMPLETED" -> "Selesai"
    "CANCELLED" -> "Dibatalkan"
    else -> "Terjadwal"
}
