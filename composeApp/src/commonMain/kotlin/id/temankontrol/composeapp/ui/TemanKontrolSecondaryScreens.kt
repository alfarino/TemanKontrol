package id.temankontrol.composeapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import id.temankontrol.composeapp.data.KmpActionOutcome
import id.temankontrol.composeapp.data.KmpAppStore
import id.temankontrol.composeapp.data.SupabaseKmpApiClient
import id.temankontrol.composeapp.domain.*
import id.temankontrol.composeapp.platform.*
import id.temankontrol.shared.dto.SyncQuestionDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KmpQuestionManagerScreen(
    state: KmpLocalDatabaseState,
    consultationId: String,
    store: KmpAppStore,
    mediaActions: PlatformMediaActions,
    onBack: () -> Unit
) {
    val questions = state.questions.filter { it.consultationId == consultationId }
        .sortedWith(compareByDescending<SyncQuestionDto> { it.isPriority }.thenBy { it.createdAtMillis })
    val attachments = state.attachments.filter { it.consultationId == consultationId }

    var editingQuestion by remember { mutableStateOf<SyncQuestionDto?>(null) }
    var showFormSheet by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    if (showSuggestions) {
        ModalBottomSheet(onDismissRequest = { showSuggestions = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Inspirasi Pertanyaan Terbuka", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                KmpSuggestionCategory.entries.forEach { cat ->
                    Text(cat.label, style = MaterialTheme.typography.labelLarge, color = SharedThemeTokens.HeroTealStart, fontWeight = FontWeight.Bold)
                    KmpSuggestionCatalog.all.filter { it.category == cat }.forEach { sug ->
                        OutlinedButton(
                            onClick = {
                                store.saveQuestion(consultationId, null, sug.text, false, null)
                                showSuggestions = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(sug.text, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showFormSheet) {
        var qText by remember(editingQuestion) { mutableStateOf(editingQuestion?.text.orEmpty()) }
        var qPriority by remember(editingQuestion) { mutableStateOf(editingQuestion?.isPriority ?: false) }
        var qAnswer by remember(editingQuestion) { mutableStateOf(editingQuestion?.answer.orEmpty()) }

        ModalBottomSheet(onDismissRequest = { showFormSheet = false; formError = null }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (editingQuestion == null) "Tambah Pertanyaan" else "Ubah Pertanyaan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (formError != null) {
                    Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(value = qText, onValueChange = { qText = it }, label = { Text("Teks Pertanyaan*") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                if (KmpValidation.isClosedQuestion(qText)) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Text("Tips: Kalimat tanya terbuka seperti 'Apa...', 'Bagaimana...' sering memberi penjelasan dokter yang lebih rinci.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(10.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = qPriority, onCheckedChange = { qPriority = it })
                    Text("Tandai sebagai Pertanyaan Prioritas")
                }
                OutlinedTextField(value = qAnswer, onValueChange = { qAnswer = it }, label = { Text("Jawaban / Catatan (Opsional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        when (val res = store.saveQuestion(consultationId, editingQuestion?.id, qText, qPriority, qAnswer)) {
                            is KmpActionOutcome.Ok -> { showFormSheet = false; formError = null }
                            is KmpActionOutcome.Err -> formError = res.message
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Simpan Pertanyaan", fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            KmpTopBar("Kelola Pertanyaan", onBack) {
                TextButton(onClick = { showSuggestions = true }) { Text("Template") }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingQuestion = null; formError = null; showFormSheet = true },
                containerColor = SharedThemeTokens.HeroTealStart,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, contentDescription = "Tambah") }
        },
        containerColor = SharedThemeTokens.PorcelainMint
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (questions.isEmpty()) {
                KmpSectionCard(title = "Belum Ada Pertanyaan") {
                    Text("Gunakan tombol '+' atau 'Template' di pojok kanan atas untuk menyiapkan pertanyaan.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                questions.forEach { q ->
                    val qAtts = attachments.filter { it.questionId == q.id }
                    var expandAtt by remember(q.id) { mutableStateOf(false) }
                    KmpSectionCard(title = if (q.isPriority) "★ Pertanyaan Prioritas" else "Pertanyaan") {
                        Text(q.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        if (!q.answer.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF3F8F8), modifier = Modifier.fillMaxWidth()) {
                                Text("Jawaban: ${q.answer}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editingQuestion = q; formError = null; showFormSheet = true }, modifier = Modifier.weight(1f)) { Text("Ubah") }
                            OutlinedButton(onClick = { expandAtt = !expandAtt }, modifier = Modifier.weight(1f)) {
                                Text(if (qAtts.isEmpty()) "+ Foto / VN" else "Lampiran (${qAtts.size})")
                            }
                            IconButton(onClick = { store.deleteQuestion(q.id) }) { Icon(Icons.Default.Delete, contentDescription = "Hapus") }
                        }
                        AnimatedVisibility(expandAtt) {
                            Spacer(modifier = Modifier.height(8.dp))
                            KmpAttachmentSection(qAtts, consultationId, q.id, "QUESTION", store, mediaActions)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun KmpConsultationModeScreen(
    state: KmpLocalDatabaseState,
    consultationId: String,
    store: KmpAppStore,
    mediaActions: PlatformMediaActions,
    onBack: () -> Unit
) {
    val questions = state.questions.filter { it.consultationId == consultationId }
        .sortedWith(compareByDescending<SyncQuestionDto> { it.isPriority }.thenBy { it.createdAtMillis })
    val attachments = state.attachments.filter { it.consultationId == consultationId }

    var index by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var savedMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { KmpTopBar("Mode Konsultasi Fokus", onBack) }, containerColor = SharedThemeTokens.PorcelainMint) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (questions.isEmpty()) {
                KmpSectionCard(title = "Daftar Pertanyaan Kosong") {
                    Text("Tambahkan minimal 1 pertanyaan terlebih dahulu sebelum masuk ke Mode Konsultasi.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onBack) { Text("Kembali") }
                }
            } else if (finished) {
                KmpSectionCard(title = "Semua Pertanyaan Selesai Dibahas") {
                    Text("Catatan & lampiran Anda telah tersimpan otomatis ke perangkat (dan tersinkron ke cloud bila sedang login).", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("Kembali ke Detail Kunjungan", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val q = questions[index.coerceIn(0, questions.lastIndex)]
                val qAtts = attachments.filter { it.questionId == q.id }
                var answerDraft by remember(q.id, q.answer) { mutableStateOf(q.answer.orEmpty()) }

                Text("Pertanyaan ${index + 1} dari ${questions.size}", style = MaterialTheme.typography.labelLarge, color = SharedThemeTokens.HeroTealStart, fontWeight = FontWeight.Bold)
                KmpSectionCard(title = if (q.isPriority) "★ Prioritas" else "Pertanyaan") {
                    Text(q.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = q.isDiscussed, onCheckedChange = { store.setQuestionDiscussed(q.id, it) })
                        Text("Tandai sudah dibahas")
                    }
                    OutlinedTextField(
                        value = answerDraft,
                        onValueChange = { answerDraft = it },
                        label = { Text("Catat Penjelasan Dokter") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            store.saveQuestionAnswer(q.id, answerDraft)
                            savedMsg = "Jawaban tersimpan."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Simpan Jawaban") }
                    if (savedMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(savedMsg!!, style = MaterialTheme.typography.labelMedium, color = SharedThemeTokens.HeroTealStart)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    KmpAttachmentSection(qAtts, consultationId, q.id, "QUESTION", store, mediaActions)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        enabled = index > 0,
                        onClick = { index--; savedMsg = null },
                        modifier = Modifier.weight(1f)
                    ) { Text("Sebelumnya") }
                    Button(
                        onClick = {
                            if (answerDraft.isNotBlank()) store.saveQuestionAnswer(q.id, answerDraft)
                            savedMsg = null
                            if (index < questions.lastIndex) index++ else finished = true
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (index < questions.lastIndex) "Berikutnya" else "Selesai") }
                }
            }
        }
    }
}

@Composable
fun KmpNoteEditorScreen(
    state: KmpLocalDatabaseState,
    consultationId: String,
    store: KmpAppStore,
    mediaActions: PlatformMediaActions,
    onBack: () -> Unit
) {
    val existing = state.notes.find { it.consultationId == consultationId }
    val additionalAtts = state.attachments.filter { it.consultationId == consultationId && it.target == "ADDITIONAL_NOTE" }

    var summary by remember { mutableStateOf(existing?.summary.orEmpty()) }
    var instruction by remember { mutableStateOf(existing?.providerInstruction.orEmpty()) }
    var additional by remember { mutableStateOf(existing?.additionalNotes.orEmpty()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { KmpTopBar("Catatan Hasil Kunjungan", onBack) }, containerColor = SharedThemeTokens.PorcelainMint) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (errorMsg != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }
            KmpSectionCard(title = "Ringkasan & Instruksi") {
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Ringkasan Hasil Konsultasi") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = instruction, onValueChange = { instruction = it }, label = { Text("Instruksi / Anjuran Dokter") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = additional, onValueChange = { additional = it }, label = { Text("Catatan Tambahan Pribadi") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
            KmpSectionCard(title = "Lampiran Foto & Rekaman Suara") {
                KmpAttachmentSection(additionalAtts, consultationId, null, "ADDITIONAL_NOTE", store, mediaActions)
            }
            Button(
                onClick = {
                    when (val res = store.saveNote(consultationId, summary, instruction, additional)) {
                        is KmpActionOutcome.Ok -> onBack()
                        is KmpActionOutcome.Err -> errorMsg = res.message
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Simpan Catatan", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun KmpFollowUpScreen(
    state: KmpLocalDatabaseState,
    consultationId: String,
    store: KmpAppStore,
    onBack: () -> Unit
) {
    val followUps = state.followUps.filter { it.consultationId == consultationId }
    var text by remember { mutableStateOf("") }
    var errMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { KmpTopBar("Rencana Tindak Lanjut", onBack) }, containerColor = SharedThemeTokens.PorcelainMint) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KmpSectionCard(title = "Tambah Tindak Lanjut") {
                if (errMsg != null) Text(errMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Contoh: Cek lab ulang bulan depan") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        when (val res = store.saveFollowUp(consultationId, null, text, null)) {
                            is KmpActionOutcome.Ok -> { text = ""; errMsg = null }
                            is KmpActionOutcome.Err -> errMsg = res.message
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Tambah Rencana") }
            }
            KmpSectionCard(title = "Daftar Rencana (${followUps.size})") {
                if (followUps.isEmpty()) {
                    Text("Belum ada tindak lanjut tercatat.", style = MaterialTheme.typography.bodySmall)
                } else {
                    followUps.forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = f.isCompleted, onCheckedChange = { store.toggleFollowUpCompleted(f.id) })
                            Text(f.text, textDecoration = if (f.isCompleted) TextDecoration.LineThrough else TextDecoration.None, modifier = Modifier.weight(1f))
                            IconButton(onClick = { store.deleteFollowUp(f.id) }) { Icon(Icons.Default.Delete, contentDescription = "Hapus") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KmpSummaryPreviewScreen(
    state: KmpLocalDatabaseState,
    consultationId: String,
    store: KmpAppStore,
    shareBridge: PlatformShareBridge,
    onBack: () -> Unit
) {
    var includeQ by remember { mutableStateOf(true) }
    var includeAns by remember { mutableStateOf(true) }
    var includeNote by remember { mutableStateOf(true) }
    var includeFollowUp by remember { mutableStateOf(true) }
    val previewText = remember(state, includeQ, includeAns, includeNote, includeFollowUp) {
        store.buildSummaryText(consultationId, includeQ, includeAns, includeNote, includeFollowUp)
    }

    Scaffold(topBar = { KmpTopBar("Pratinjau & Ekspor Ringkasan", onBack) }, containerColor = SharedThemeTokens.PorcelainMint) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = includeQ, onClick = { includeQ = !includeQ }, label = { Text("Tanya") })
                FilterChip(selected = includeAns, onClick = { includeAns = !includeAns }, label = { Text("Jawab") })
                FilterChip(selected = includeNote, onClick = { includeNote = !includeNote }, label = { Text("Catatan") })
                FilterChip(selected = includeFollowUp, onClick = { includeFollowUp = !includeFollowUp }, label = { Text("Aksi") })
            }
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text("Lampiran Foto & Rekaman Suara tetap privat di perangkat/akun Anda dan tidak disertakan ke PDF/teks yang dibagikan.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
            KmpSectionCard(title = "Pratinjau Dokumen") {
                Text(previewText, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { shareBridge.shareText("Ringkasan Teman Kontrol", previewText) },
                    modifier = Modifier.weight(1f)
                ) { Text("Bagikan Teks") }
                Button(
                    onClick = {
                        val bytes = store.buildSummaryPdfBytes(consultationId, includeQ, includeAns, includeNote, includeFollowUp)
                        if (bytes != null) shareBridge.sharePdfBytes("Ringkasan_TemanKontrol.pdf", bytes)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Ekspor PDF") }
            }
        }
    }
}

@Composable
fun KmpAuthScreen(
    store: KmpAppStore,
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(0) } // 0=login, 1=register, 2=reset
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var privacyAccepted by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf(SupabaseKmpApiClient.DEFAULT_BASE_URL) }
    var busy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var infoMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { KmpTopBar(when (mode) { 1 -> "Daftar Akun Cloud"; 2 -> "Reset Kata Sandi"; else -> "Masuk ke Akun Cloud" }, onBack) },
        containerColor = SharedThemeTokens.PorcelainMint
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (mode != 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == 0, onClick = { mode = 0; errorMsg = null }, label = { Text("Masuk") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = mode == 1, onClick = { mode = 1; errorMsg = null }, label = { Text("Daftar Baru") }, modifier = Modifier.weight(1f))
                }
            }
            if (errorMsg != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
            if (infoMsg != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(infoMsg!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
            if (mode == 1) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Tampilan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth())
            if (mode == 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = otp, onValueChange = { otp = it }, label = { Text("Kode OTP (6 digit)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedButton(enabled = !busy && email.isNotBlank(), onClick = {
                        scope.launch {
                            busy = true; errorMsg = null
                            when (val r = store.forgotPassword(email, serverUrl)) {
                                is KmpActionOutcome.Ok -> infoMsg = r.data
                                is KmpActionOutcome.Err -> errorMsg = r.message
                            }
                            busy = false
                        }
                    }) { Text("Kirim OTP") }
                }
            }
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (mode == 2) "Kata Sandi Baru (min. 8)" else "Kata Sandi") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (mode == 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = privacyAccepted, onCheckedChange = { privacyAccepted = it })
                    Text("Saya menyetujui Kebijakan Privasi penyimpanan & sinkronisasi catatan cloud secara aman.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true; errorMsg = null; infoMsg = null
                        when (mode) {
                            0 -> when (val r = store.login(email, password, serverUrl)) {
                                is KmpActionOutcome.Ok -> onAuthSuccess()
                                is KmpActionOutcome.Err -> errorMsg = r.message
                            }
                            1 -> when (val r = store.register(email, password, name, privacyAccepted, serverUrl)) {
                                is KmpActionOutcome.Ok -> onAuthSuccess()
                                is KmpActionOutcome.Err -> errorMsg = r.message
                            }
                            else -> when (val r = store.resetPassword(email, otp, password, serverUrl)) {
                                is KmpActionOutcome.Ok -> { infoMsg = "Kata sandi diperbarui. Silakan masuk."; mode = 0 }
                                is KmpActionOutcome.Err -> errorMsg = r.message
                            }
                        }
                        busy = false
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(when (mode) { 1 -> "Daftar & Amankan"; 2 -> "Simpan Sandi Baru"; else -> "Masuk & Pulihkan" }, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { mode = if (mode == 2) 0 else 2; errorMsg = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (mode == 2) "Kembali ke Masuk" else "Lupa kata sandi?")
            }
        }
    }
}

@Composable
fun KmpAccountSyncScreen(
    state: KmpLocalDatabaseState,
    store: KmpAppStore,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val session = state.accountSession
    var busy by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showConfirmLogout by remember { mutableStateOf(false) }
    var showOfflineLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showConfirmLogout) {
        AlertDialog(
            onDismissRequest = { showConfirmLogout = false },
            title = { Text("Cadangkan & Keluar Akun?") },
            text = { Text("Agar data antar-akun tidak bercampur, semua catatan & lampiran akun ini akan dicadangkan ke Supabase terlebih dahulu, lalu data lokal perangkat dikosongkan.") },
            confirmButton = {
                Button(onClick = {
                    showConfirmLogout = false
                    scope.launch {
                        busy = true; errorMsg = null
                        when (val r = store.logout(forceWithoutBackup = false)) {
                            is KmpActionOutcome.Ok -> statusMsg = "Data dicadangkan & perangkat dikosongkan. Siap masuk ke akun lain."
                            is KmpActionOutcome.Err -> { errorMsg = r.message; showOfflineLogoutDialog = true }
                        }
                        busy = false
                    }
                }) { Text("Cadangkan & Keluar") }
            },
            dismissButton = { TextButton(onClick = { showConfirmLogout = false }) { Text("Batal") } }
        )
    }

    if (showOfflineLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineLogoutDialog = false },
            title = { Text("Pencadangan Cloud Gagal") },
            text = { Text("${errorMsg.orEmpty()}\n\nJika memaksa keluar tanpa mencadangkan, perubahan terbaru akan dihapus dari HP ini.") },
            confirmButton = {
                Button(onClick = {
                    showOfflineLogoutDialog = false
                    scope.launch {
                        busy = true
                        store.logout(forceWithoutBackup = false).also {
                            if (it is KmpActionOutcome.Ok) statusMsg = "Data dicadangkan & keluar berhasil."
                            else if (it is KmpActionOutcome.Err) { errorMsg = it.message; showOfflineLogoutDialog = true }
                        }
                        busy = false
                    }
                }) { Text("Coba Lagi") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOfflineLogoutDialog = false
                        scope.launch {
                            busy = true
                            store.logout(forceWithoutBackup = true)
                            statusMsg = "Keluar & data lokal dikosongkan."
                            busy = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Tetap Keluar") }
            }
        )
    }

    if (showDeleteDialog) {
        var pass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Akun & Data Permanen?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tindakan ini menghapus akun & semua catatan/lampiran secara permanen dari Supabase maupun perangkat.")
                    OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Kata Sandi Konfirmasi") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    enabled = pass.isNotBlank(),
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            busy = true; errorMsg = null
                            when (val r = store.deleteAccountPermanently(pass)) {
                                is KmpActionOutcome.Ok -> onBack()
                                is KmpActionOutcome.Err -> errorMsg = r.message
                            }
                            busy = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus Permanen") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } }
        )
    }

    Scaffold(topBar = { KmpTopBar("Akun & Sinkronisasi Cloud", onBack) }, containerColor = SharedThemeTokens.PorcelainMint) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (statusMsg != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(statusMsg!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
            if (errorMsg != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
            if (session == null) {
                KmpSectionCard(title = "Mode Lokal Aktif") {
                    Text("Masuk atau daftar akun Supabase untuk mencadangkan otomatis seluruh data & Foto/VN antar perangkat.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) { Text("Masuk / Daftar Akun") }
                }
            } else {
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(SharedThemeTokens.HeroTealStart, SharedThemeTokens.HeroTealEnd))).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(session.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${session.email} • Cloud Supabase", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                        Text(
                            text = session.lastSyncAtMillis?.let { "Tersinkron: ${KmpDateUtils.formatDateTime(it)}" } ?: "Belum tersinkron di perangkat ini",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                KmpSectionCard(title = "Sinkronisasi Otomatis & Manual") {
                    Text("Auto-sync aktif (~3 detik setelah perubahan data saat online & saat aplikasi dibuka).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(enabled = !busy, onClick = {
                        scope.launch {
                            busy = true; errorMsg = null
                            when (val r = store.syncNow(false)) {
                                is KmpActionOutcome.Ok -> statusMsg = "Tersinkron (${r.data.acceptedCount} unggah)."
                                is KmpActionOutcome.Err -> errorMsg = r.message
                            }
                            busy = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Sinkronkan Sekarang") }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(enabled = !busy, onClick = {
                        scope.launch {
                            busy = true; errorMsg = null
                            when (val r = store.syncNow(true)) {
                                is KmpActionOutcome.Ok -> statusMsg = "Pemulihan penuh selesai."
                                is KmpActionOutcome.Err -> errorMsg = r.message
                            }
                            busy = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Pulihkan Penuh dari Cloud") }
                }
                KmpSectionCard(title = "Manajemen Akun") {
                    OutlinedButton(enabled = !busy, onClick = { showConfirmLogout = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cadangkan & Keluar / Ganti Akun")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        enabled = !busy,
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Hapus Akun Permanen") }
                }
            }
            OutlinedButton(onClick = onNavigateToBackup, modifier = Modifier.fillMaxWidth()) {
                Text("Buka Cadangan Berkas Lokal")
            }
        }
    }
}

@Composable
fun KmpBackupScreen(
    state: KmpLocalDatabaseState,
    store: KmpAppStore,
    shareBridge: PlatformShareBridge,
    onBack: () -> Unit
) {
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { KmpTopBar("Cadangan Berkas Lokal", onBack) }, containerColor = SharedThemeTokens.PorcelainMint) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (message != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(message!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
            KmpSectionCard(title = "Ringkasan Data Lokal") {
                Text("${state.consultations.size} kunjungan • ${state.questions.size} pertanyaan • ${state.followUps.size} tindak lanjut", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val jsonText = store.exportPortableBackupJson()
                        shareBridge.exportBackupJson("TemanKontrol_Backup.tkbackup.json", jsonText) { ok, info ->
                            message = if (ok) "Cadangan siap disimpan: $info" else info
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ekspor Cadangan (.tkbackup.json)") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        shareBridge.importBackupJson { raw ->
                            if (raw != null) {
                                when (val res = store.restorePortableBackupJson(raw)) {
                                    is KmpActionOutcome.Ok -> message = "Berhasil memulihkan ${res.data} kunjungan."
                                    is KmpActionOutcome.Err -> message = res.message
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pulihkan dari Berkas Cadangan") }
            }
        }
    }
}

@Composable
fun KmpSettingsScreen(
    state: KmpLocalDatabaseState,
    biometricAuth: PlatformBiometricAuth,
    store: KmpAppStore,
    onBack: () -> Unit,
    onNavigate: (KmpRoute) -> Unit
) {
    val session = state.accountSession
    Scaffold(topBar = { KmpTopBar("Pengaturan", onBack) }, containerColor = SharedThemeTokens.PorcelainMint) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().clickable { onNavigate(if (session != null) KmpRoute.AccountSync else KmpRoute.Auth) }
            ) {
                Column(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(SharedThemeTokens.HeroTealStart, SharedThemeTokens.HeroTealEnd))).padding(18.dp)) {
                    Text(session?.displayName ?: "Mode Lokal (Tanpa Akun)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(if (session != null) "${session.email} • Tersambung ke Cloud" else "Ketuk untuk Masuk / Daftar akun cloud", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                }
            }
            KmpSectionCard(title = "Keamanan & Privasi") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kunci Aplikasi (Face ID / Sandi)", fontWeight = FontWeight.SemiBold)
                        Text("Minta verifikasi saat aplikasi dibuka", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.appLockEnabled,
                        enabled = biometricAuth.canAuthenticate(),
                        onCheckedChange = { store.updateAppLock(it, state.appLockTimeoutMinutes) }
                    )
                }
            }
            KmpSectionCard(title = "Menu Lainnya") {
                TextButton(onClick = { onNavigate(KmpRoute.ReminderSettings) }) { Text("Pengaturan Pengingat & Alarm") }
                TextButton(onClick = { onNavigate(KmpRoute.Backup) }) { Text("Cadangan & Pemulihan Lokal") }
                TextButton(onClick = { onNavigate(KmpRoute.About) }) { Text("Tentang Teman Kontrol") }
            }
        }
    }
}

@Composable
fun KmpReminderSettingsScreen(
    state: KmpLocalDatabaseState,
    store: KmpAppStore,
    alarmScheduler: PlatformAlarmScheduler,
    onTriggerTestAlarm: (() -> Unit)?,
    onBack: () -> Unit
) {
    val s = state.reminderSettings
    var testSent by remember { mutableStateOf(false) }

    Scaffold(topBar = { KmpTopBar("Pengaturan Pengingat", onBack) }, containerColor = SharedThemeTokens.PorcelainMint) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KmpSectionCard(title = "Jadwal Alarm Kunjungan") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Pengingat H-1 Malam (${s.visitReminderHour}:00)", modifier = Modifier.weight(1f))
                    Switch(checked = s.visitReminderEnabled, onCheckedChange = { v -> store.updateReminderSettings { it.copy(visitReminderEnabled = v) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("30 Menit Sebelum Kontrol", modifier = Modifier.weight(1f))
                    Switch(checked = s.visitThirtyMinBeforeEnabled, onCheckedChange = { v -> store.updateReminderSettings { it.copy(visitThirtyMinBeforeEnabled = v) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Saat Waktu Kontrol Tiba", modifier = Modifier.weight(1f))
                    Switch(checked = s.visitAtTimeEnabled, onCheckedChange = { v -> store.updateReminderSettings { it.copy(visitAtTimeEnabled = v) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Suara Alarm Khusus", modifier = Modifier.weight(1f))
                    Switch(checked = s.alarmSoundModeEnabled, onCheckedChange = { v -> store.updateReminderSettings { it.copy(alarmSoundModeEnabled = v) } })
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (onTriggerTestAlarm != null) onTriggerTestAlarm()
                        else alarmScheduler.triggerTestAlarm("Uji Alarm Teman Kontrol", "Notifikasi pengingat siap berbunyi di ${platformName()}!")
                        testSent = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Tes Bunyi Alarm Sekarang") }
                if (testSent) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Alarm uji telah dikirim.", style = MaterialTheme.typography.bodySmall, color = SharedThemeTokens.HeroTealStart)
                }
            }
        }
    }
}

@Composable
fun KmpAboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = { KmpTopBar("Tentang Teman Kontrol", onBack) }, containerColor = SharedThemeTokens.PorcelainMint) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KmpSectionCard(title = "Teman Kontrol v3.0 (${platformName()})") {
                Text("Aplikasi pendamping persiapan konsultasi kesehatan berbasis Offline-First dengan sinkronisasi cloud Supabase.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Bukan alat diagnosis medis, bukan layanan telemedicine, dan bukan layanan gawat darurat.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
