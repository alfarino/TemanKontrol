package id.temankontrol.composeapp.data

import id.temankontrol.composeapp.domain.*
import id.temankontrol.composeapp.platform.*
import id.temankontrol.shared.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface KmpActionOutcome<out T> {
    data class Ok<T>(val data: T) : KmpActionOutcome<T>
    data class Err(val message: String) : KmpActionOutcome<Nothing>
}

@Serializable
data class KmpPortableBackup(
    val formatVersion: Int = 1,
    val exportedAtMillis: Long,
    val consultations: List<SyncConsultationDto>,
    val questions: List<SyncQuestionDto>,
    val notes: List<SyncNoteDto>,
    val followUps: List<SyncFollowUpDto>,
    val attachments: List<SyncAttachmentMetaDto>,
    val reminderSettings: SyncReminderSettingsDto
)

/**
 * Store Offline-First Multiplatform lengkap untuk Teman Kontrol (iOS & Desktop/Shared KMP):
 * - Menyimpan seluruh data ke JSON lokal (Documents/temankontrol_store.json di iPhone).
 * - Terhubung penuh ke Supabase Auth, Sync RPC (LWW), Storage Foto/VN, dan Edge Function delete-account.
 * - Mendukung Auto-Sync ~3 detik setelah perubahan data, foreground sync, serta backup wajib & isolasi data saat ganti akun.
 */
class KmpAppStore(
    private val localStore: PlatformLocalStore = createPlatformLocalStore(),
    private val apiClient: SupabaseKmpApiClient = SupabaseKmpApiClient(),
    private val alarmScheduler: PlatformAlarmScheduler = createPlatformAlarmScheduler(),
    private val pdfExporter: PlatformPdfExporter = createPlatformPdfExporter(),
    private val autoStartSyncLoop: Boolean = true
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncMutex = Mutex()
    private val uploadedAttachmentIdsInSession = mutableSetOf<String>()
    private var lastForegroundSyncCheckMillis = 0L

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<KmpLocalDatabaseState> = _state.asStateFlow()

    init {
        if (autoStartSyncLoop) {
            startAutoSyncObserver()
        }
    }

    private fun loadInitialState(): KmpLocalDatabaseState {
        val raw = runCatching { localStore.loadStateJson() }.getOrNull()
        val parsed = if (!raw.isNullOrBlank()) {
            runCatching { json.decodeFromString<KmpLocalDatabaseState>(raw) }.getOrNull()
        } else null

        if (parsed != null) return parsed

        // Seed 1 sample consultation for first-time empty offline exploration if never opened
        val now = currentTimeMillis()
        val sampleId = "c-sample-1"
        val seeded = KmpLocalDatabaseState(
            consultations = listOf(
                SyncConsultationDto(
                    id = sampleId,
                    title = "Kontrol Poliklinik Penyakit Dalam",
                    appointmentAtMillis = now + 2 * 86_400_000L,
                    reason = "Evaluasi hasil lab rutin & penyesuaian dosis obat harian.",
                    facilityName = "RSUP / Klinik Utama",
                    providerName = "dr. Spesialis Penyakit Dalam",
                    status = "SCHEDULED",
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            ),
            questions = listOf(
                SyncQuestionDto(
                    id = "q-sample-1",
                    consultationId = sampleId,
                    text = "Apakah dosis obat pagi dan malam masih perlu disesuaikan berdasarkan hasil lab terbaru?",
                    isPriority = true,
                    isDiscussed = false,
                    createdAtMillis = now,
                    updatedAtMillis = now
                ),
                SyncQuestionDto(
                    id = "q-sample-2",
                    consultationId = sampleId,
                    text = "Tanda-tanda apa yang perlu saya perhatikan di rumah dan kapan harus segera kontrol?",
                    isPriority = false,
                    isDiscussed = false,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
        )
        localStore.saveStateJson(json.encodeToString(seeded))
        return seeded
    }

    private fun mutateState(
        markDirtyForCloud: Boolean = true,
        rescheduleAlarms: Boolean = false,
        transform: (KmpLocalDatabaseState) -> KmpLocalDatabaseState
    ) {
        val now = currentTimeMillis()
        val updatedBase = transform(_state.value)
        val updated = if (markDirtyForCloud) {
            updatedBase.copy(
                hasUnsyncedCloudChanges = true,
                lastDataChangeMillis = now
            )
        } else {
            updatedBase
        }
        _state.value = updated
        localStore.saveStateJson(json.encodeToString(updated))
        if (rescheduleAlarms) {
            KmpReminderScheduler.rescheduleAll(alarmScheduler, updated)
        }
    }

    @OptIn(FlowPreview::class)
    private fun startAutoSyncObserver() {
        scope.launch {
            _state.map { it.lastDataChangeMillis }
                .distinctUntilChanged()
                .debounce(3_000L)
                .collect { changeTs ->
                    if (changeTs <= 0L) return@collect
                    val cur = _state.value
                    if (cur.accountSession != null && cur.hasUnsyncedCloudChanges) {
                        syncNow(forceFullRestore = false)
                    }
                }
        }
    }

    fun onAppForeground() {
        val now = currentTimeMillis()
        if (now - lastForegroundSyncCheckMillis < 60_000L) return
        lastForegroundSyncCheckMillis = now
        scope.launch {
            syncIfPendingOrStale(maxStaleMillis = 5 * 60_000L)
        }
    }

    // --- Notice & Security Settings ---

    fun acceptNotice() {
        mutateState(markDirtyForCloud = false) { it.copy(noticeAccepted = true) }
    }

    fun updateAppLock(enabled: Boolean, timeoutMinutes: Int) {
        mutateState(markDirtyForCloud = false) {
            it.copy(appLockEnabled = enabled, appLockTimeoutMinutes = timeoutMinutes)
        }
    }

    fun updateReminderSettings(transform: (SyncReminderSettingsDto) -> SyncReminderSettingsDto) {
        val now = currentTimeMillis()
        mutateState(markDirtyForCloud = true, rescheduleAlarms = true) { cur ->
            cur.copy(reminderSettings = transform(cur.reminderSettings).copy(updatedAtMillis = now))
        }
    }

    // --- Consultations CRUD ---

    fun saveConsultation(
        existingId: String?,
        title: String,
        appointmentAtMillis: Long,
        reason: String,
        facilityName: String?,
        providerName: String?
    ): KmpActionOutcome<String> {
        val err = KmpValidation.validateConsultation(title, reason, facilityName, providerName)
        if (err != null) return KmpActionOutcome.Err(err)

        val now = currentTimeMillis()
        val id = existingId ?: generateUuid()
        val cleanTitle = KmpValidation.normalizeLine(title)
        val cleanReason = reason.trim()
        val cleanFacility = facilityName?.trim()?.takeIf { it.isNotEmpty() }
        val cleanProvider = providerName?.trim()?.takeIf { it.isNotEmpty() }

        mutateState(markDirtyForCloud = true, rescheduleAlarms = true) { cur ->
            val prev = cur.consultations.find { it.id == id }
            val nextItem = if (prev != null) {
                prev.copy(
                    title = cleanTitle,
                    appointmentAtMillis = appointmentAtMillis,
                    reason = cleanReason,
                    facilityName = cleanFacility,
                    providerName = cleanProvider,
                    updatedAtMillis = now
                )
            } else {
                SyncConsultationDto(
                    id = id,
                    title = cleanTitle,
                    appointmentAtMillis = appointmentAtMillis,
                    reason = cleanReason,
                    facilityName = cleanFacility,
                    providerName = cleanProvider,
                    status = "SCHEDULED",
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            }
            val list = cur.consultations.filterNot { it.id == id } + nextItem
            cur.copy(consultations = list.sortedBy { it.appointmentAtMillis })
        }
        return KmpActionOutcome.Ok(id)
    }

    fun setConsultationStatus(id: String, status: String) {
        val now = currentTimeMillis()
        mutateState(markDirtyForCloud = true, rescheduleAlarms = true) { cur ->
            cur.copy(
                consultations = cur.consultations.map { c ->
                    if (c.id == id) c.copy(status = status, updatedAtMillis = now) else c
                }
            )
        }
    }

    fun deleteConsultation(id: String) {
        val now = currentTimeMillis()
        localStore.deleteConsultationFiles(id)
        mutateState(markDirtyForCloud = true, rescheduleAlarms = true) { cur ->
            val newTombstones = cur.tombstones.filterNot { it.entityId == id } +
                KmpSyncTombstone(KmpSyncEntityType.CONSULTATION, id, now)
            cur.copy(
                consultations = cur.consultations.filterNot { it.id == id },
                questions = cur.questions.filterNot { it.consultationId == id },
                notes = cur.notes.filterNot { it.consultationId == id },
                followUps = cur.followUps.filterNot { it.consultationId == id },
                attachments = cur.attachments.filterNot { it.consultationId == id },
                tombstones = newTombstones
            )
        }
    }

    // --- Questions CRUD ---

    fun saveQuestion(
        consultationId: String,
        existingQuestionId: String?,
        text: String,
        isPriority: Boolean,
        answer: String?
    ): KmpActionOutcome<String> {
        val clean = KmpValidation.normalizeLine(text)
        if (clean.isEmpty()) return KmpActionOutcome.Err("Pertanyaan wajib diisi.")
        if (clean.length > KmpValidation.MAX_QUESTION) {
            return KmpActionOutcome.Err("Pertanyaan maksimal ${KmpValidation.MAX_QUESTION} karakter.")
        }
        val cleanAns = answer?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanAns != null && cleanAns.length > KmpValidation.MAX_ANSWER) {
            return KmpActionOutcome.Err("Jawaban maksimal ${KmpValidation.MAX_ANSWER} karakter.")
        }

        val norm = KmpValidation.normalizeDuplicate(clean)
        val duplicate = _state.value.questions.any {
            it.consultationId == consultationId &&
                it.id != existingQuestionId &&
                KmpValidation.normalizeDuplicate(it.text) == norm
        }
        if (duplicate) return KmpActionOutcome.Err("Pertanyaan yang sama sudah ada pada kunjungan ini.")

        val now = currentTimeMillis()
        val id = existingQuestionId ?: generateUuid()
        mutateState(markDirtyForCloud = true) { cur ->
            val prev = cur.questions.find { it.id == id }
            val next = if (prev != null) {
                prev.copy(
                    text = clean,
                    isPriority = isPriority,
                    answer = cleanAns,
                    answerUpdatedAtMillis = if (cleanAns != prev.answer) now else prev.answerUpdatedAtMillis,
                    updatedAtMillis = now
                )
            } else {
                SyncQuestionDto(
                    id = id,
                    consultationId = consultationId,
                    text = clean,
                    isPriority = isPriority,
                    isDiscussed = false,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    answer = cleanAns,
                    answerUpdatedAtMillis = if (cleanAns != null) now else null
                )
            }
            cur.copy(questions = cur.questions.filterNot { it.id == id } + next)
        }
        return KmpActionOutcome.Ok(id)
    }

    fun toggleQuestionPriority(questionId: String) {
        val now = currentTimeMillis()
        mutateState(markDirtyForCloud = true) { cur ->
            cur.copy(questions = cur.questions.map { q ->
                if (q.id == questionId) q.copy(isPriority = !q.isPriority, updatedAtMillis = now) else q
            })
        }
    }

    fun setQuestionDiscussed(questionId: String, discussed: Boolean) {
        val now = currentTimeMillis()
        mutateState(markDirtyForCloud = true) { cur ->
            cur.copy(questions = cur.questions.map { q ->
                if (q.id == questionId) q.copy(isDiscussed = discussed, updatedAtMillis = now) else q
            })
        }
    }

    fun saveQuestionAnswer(questionId: String, answer: String?): KmpActionOutcome<Unit> {
        val clean = answer?.trim()?.takeIf { it.isNotEmpty() }
        if (clean != null && clean.length > KmpValidation.MAX_ANSWER) {
            return KmpActionOutcome.Err("Jawaban maksimal ${KmpValidation.MAX_ANSWER} karakter.")
        }
        val now = currentTimeMillis()
        mutateState(markDirtyForCloud = true) { cur ->
            cur.copy(questions = cur.questions.map { q ->
                if (q.id == questionId) {
                    q.copy(
                        answer = clean,
                        answerUpdatedAtMillis = now,
                        isDiscussed = if (clean != null) true else q.isDiscussed,
                        updatedAtMillis = now
                    )
                } else q
            })
        }
        return KmpActionOutcome.Ok(Unit)
    }

    fun deleteQuestion(questionId: String) {
        val now = currentTimeMillis()
        mutateState(markDirtyForCloud = true) { cur ->
            val relatedAtts = cur.attachments.filter { it.questionId == questionId }
            relatedAtts.forEach { localStore.deleteAttachmentFile(it.consultationId, it.fileName) }
            val newTombstones = cur.tombstones +
                KmpSyncTombstone(KmpSyncEntityType.QUESTION, questionId, now) +
                relatedAtts.map { KmpSyncTombstone(KmpSyncEntityType.ATTACHMENT, it.id, now) }
            cur.copy(
                questions = cur.questions.filterNot { it.id == questionId },
                attachments = cur.attachments.filterNot { it.questionId == questionId },
                tombstones = newTombstones
            )
        }
    }

    fun carryOverUnfinishedQuestions(fromConsultationId: String, toConsultationId: String): Int {
        val now = currentTimeMillis()
        val cur = _state.value
        val existingDestNorms = cur.questions
            .filter { it.consultationId == toConsultationId }
            .map { KmpValidation.normalizeDuplicate(it.text) }
            .toMutableSet()
        val candidates = cur.questions.filter { it.consultationId == fromConsultationId && !it.isDiscussed }
        val toAdd = mutableListOf<SyncQuestionDto>()
        candidates.forEach { q ->
            val norm = KmpValidation.normalizeDuplicate(q.text)
            if (existingDestNorms.add(norm)) {
                toAdd.add(
                    SyncQuestionDto(
                        id = generateUuid(),
                        consultationId = toConsultationId,
                        text = q.text,
                        isPriority = q.isPriority,
                        isDiscussed = false,
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                )
            }
        }
        if (toAdd.isNotEmpty()) {
            mutateState(markDirtyForCloud = true) { it.copy(questions = it.questions + toAdd) }
        }
        return toAdd.size
    }

    // --- Notes CRUD ---

    fun saveNote(
        consultationId: String,
        summary: String?,
        providerInstruction: String?,
        additionalNotes: String?
    ): KmpActionOutcome<Unit> {
        val s = summary?.trim()?.takeIf { it.isNotEmpty() }
        val p = providerInstruction?.trim()?.takeIf { it.isNotEmpty() }
        val a = additionalNotes?.trim()?.takeIf { it.isNotEmpty() }
        if ((s?.length ?: 0) > KmpValidation.MAX_NOTE ||
            (p?.length ?: 0) > KmpValidation.MAX_NOTE ||
            (a?.length ?: 0) > KmpValidation.MAX_NOTE
        ) {
            return KmpActionOutcome.Err("Setiap kolom catatan maksimal ${KmpValidation.MAX_NOTE} karakter.")
        }
        val now = currentTimeMillis()
        mutateState(markDirtyForCloud = true) { cur ->
            val next = SyncNoteDto(
                consultationId = consultationId,
                summary = s,
                providerInstruction = p,
                additionalNotes = a,
                updatedAtMillis = now
            )
            cur.copy(notes = cur.notes.filterNot { it.consultationId == consultationId } + next)
        }
        return KmpActionOutcome.Ok(Unit)
    }

    // --- Follow-Ups CRUD ---

    fun saveFollowUp(
        consultationId: String,
        existingId: String?,
        text: String,
        dueDateEpochDay: Long?
    ): KmpActionOutcome<String> {
        val clean = KmpValidation.normalizeLine(text)
        if (clean.isEmpty()) return KmpActionOutcome.Err("Rencana tindak lanjut wajib diisi.")
        if (clean.length > KmpValidation.MAX_FOLLOW_UP) {
            return KmpActionOutcome.Err("Tindak lanjut maksimal ${KmpValidation.MAX_FOLLOW_UP} karakter.")
        }
        val now = currentTimeMillis()
        val id = existingId ?: generateUuid()
        mutateState(markDirtyForCloud = true) { cur ->
            val prev = cur.followUps.find { it.id == id }
            val next = if (prev != null) {
                prev.copy(text = clean, dueDateEpochDay = dueDateEpochDay, updatedAtMillis = now)
            } else {
                SyncFollowUpDto(
                    id = id,
                    consultationId = consultationId,
                    text = clean,
                    dueDateEpochDay = dueDateEpochDay,
                    isCompleted = false,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            }
            cur.copy(followUps = cur.followUps.filterNot { it.id == id } + next)
        }
        return KmpActionOutcome.Ok(id)
    }

    fun toggleFollowUpCompleted(id: String) {
        val now = currentTimeMillis()
        mutateState(markDirtyForCloud = true) { cur ->
            cur.copy(followUps = cur.followUps.map { f ->
                if (f.id == id) {
                    val done = !f.isCompleted
                    f.copy(
                        isCompleted = done,
                        completedAtMillis = if (done) now else null,
                        updatedAtMillis = now
                    )
                } else f
            })
        }
    }

    fun deleteFollowUp(id: String) {
        val now = currentTimeMillis()
        mutateState(markDirtyForCloud = true) { cur ->
            cur.copy(
                followUps = cur.followUps.filterNot { it.id == id },
                tombstones = cur.tombstones + KmpSyncTombstone(KmpSyncEntityType.FOLLOW_UP, id, now)
            )
        }
    }

    // --- Attachments ---

    fun addAttachment(
        consultationId: String,
        questionId: String?,
        target: String, // "QUESTION" or "ADDITIONAL_NOTE"
        kind: String, // "PHOTO" or "VOICE"
        picked: PickedMediaFile
    ): String {
        val id = generateUuid()
        val now = currentTimeMillis()
        val storedFileName = "${id}_${picked.fileName.replace(" ", "_")}"
        localStore.writeAttachmentBytes(consultationId, storedFileName, picked.bytes)
        val meta = SyncAttachmentMetaDto(
            id = id,
            consultationId = consultationId,
            questionId = questionId,
            target = target,
            kind = kind,
            fileName = storedFileName,
            mimeType = picked.mimeType,
            sizeBytes = picked.bytes.size.toLong(),
            durationMs = picked.durationMs,
            caption = null,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        mutateState(markDirtyForCloud = true) { cur ->
            cur.copy(attachments = listOf(meta) + cur.attachments)
        }
        return id
    }

    fun updateAttachmentCaption(attachmentId: String, caption: String?) {
        val now = currentTimeMillis()
        val clean = caption?.trim()?.takeIf { it.isNotEmpty() }
        mutateState(markDirtyForCloud = true) { cur ->
            cur.copy(attachments = cur.attachments.map { a ->
                if (a.id == attachmentId) a.copy(caption = clean, updatedAtMillis = now) else a
            })
        }
    }

    fun deleteAttachment(attachmentId: String) {
        val now = currentTimeMillis()
        val target = _state.value.attachments.find { it.id == attachmentId }
        if (target != null) {
            localStore.deleteAttachmentFile(target.consultationId, target.fileName)
        }
        mutateState(markDirtyForCloud = true) { cur ->
            cur.copy(
                attachments = cur.attachments.filterNot { it.id == attachmentId },
                tombstones = cur.tombstones + KmpSyncTombstone(KmpSyncEntityType.ATTACHMENT, attachmentId, now)
            )
        }
    }

    fun readAttachmentBytes(att: SyncAttachmentMetaDto): ByteArray? =
        localStore.readAttachmentBytes(att.consultationId, att.fileName)

    // --- Summary & Portable Backup ---

    fun buildSummaryText(
        consultationId: String,
        includeQuestions: Boolean,
        includeAnswers: Boolean,
        includeNote: Boolean,
        includeFollowUps: Boolean
    ): String {
        val st = _state.value
        val c = st.consultations.find { it.id == consultationId } ?: return ""
        val questions = st.questions.filter { it.consultationId == consultationId }
        val note = st.notes.find { it.consultationId == consultationId }
        val followUps = st.followUps.filter { it.consultationId == consultationId }

        return buildString {
            appendLine("RINGKASAN KONSULTASI — TEMAN KONTROL")
            appendLine("Judul: ${c.title}")
            appendLine("Jadwal: ${KmpDateUtils.formatDateTime(c.appointmentAtMillis)}")
            if (!c.facilityName.isNullOrBlank()) appendLine("Fasilitas: ${c.facilityName}")
            if (!c.providerName.isNullOrBlank()) appendLine("Tenaga Kesehatan: ${c.providerName}")
            appendLine("Alasan: ${c.reason}")

            if (includeQuestions && questions.isNotEmpty()) {
                appendLine()
                appendLine("PERTANYAAN:")
                questions.forEachIndexed { idx, q ->
                    val mark = if (q.isDiscussed) "[Terbahas]" else "[Belum]"
                    appendLine("${idx + 1}. $mark ${q.text}")
                    if (includeAnswers && !q.answer.isNullOrBlank()) {
                        appendLine("   Jawaban: ${q.answer}")
                    }
                }
            }
            if (includeNote && note != null &&
                (!note.summary.isNullOrBlank() || !note.providerInstruction.isNullOrBlank() || !note.additionalNotes.isNullOrBlank())
            ) {
                appendLine()
                appendLine("CATATAN HASIL:")
                if (!note.summary.isNullOrBlank()) appendLine("- Ringkasan: ${note.summary}")
                if (!note.providerInstruction.isNullOrBlank()) appendLine("- Instruksi: ${note.providerInstruction}")
                if (!note.additionalNotes.isNullOrBlank()) appendLine("- Catatan Tambahan: ${note.additionalNotes}")
            }
            if (includeFollowUps && followUps.isNotEmpty()) {
                appendLine()
                appendLine("RENCANA TINDAK LANJUT:")
                followUps.forEach { f ->
                    val status = if (f.isCompleted) "[Selesai]" else "[Aktif]"
                    val due = f.dueDateEpochDay?.let { " (Target: ${KmpDateUtils.formatEpochDay(it)})" }.orEmpty()
                    appendLine("- $status ${f.text}$due")
                }
            }
        }.trim()
    }

    fun buildSummaryPdfBytes(
        consultationId: String,
        includeQuestions: Boolean,
        includeAnswers: Boolean,
        includeNote: Boolean,
        includeFollowUps: Boolean
    ): ByteArray? {
        val st = _state.value
        val c = st.consultations.find { it.id == consultationId } ?: return null
        val questions = st.questions.filter { it.consultationId == consultationId }
        val note = st.notes.find { it.consultationId == consultationId }
        val followUps = st.followUps.filter { it.consultationId == consultationId }

        val lines = mutableListOf<String>()
        if (includeQuestions) {
            questions.forEachIndexed { idx, q ->
                lines += "${idx + 1}. ${q.text}"
                if (includeAnswers && !q.answer.isNullOrBlank()) {
                    lines += "   Jawaban: ${q.answer}"
                }
            }
        }
        val noteBlock = buildString {
            if (includeNote && note != null) {
                if (!note.summary.isNullOrBlank()) appendLine("Ringkasan: ${note.summary}")
                if (!note.providerInstruction.isNullOrBlank()) appendLine("Instruksi: ${note.providerInstruction}")
                if (!note.additionalNotes.isNullOrBlank()) appendLine("Catatan: ${note.additionalNotes}")
            }
            if (includeFollowUps && followUps.isNotEmpty()) {
                appendLine("Tindak Lanjut:")
                followUps.forEach { f ->
                    appendLine("- ${if (f.isCompleted) "[Selesai] " else ""}${f.text}")
                }
            }
        }.trim().takeIf { it.isNotEmpty() }

        return pdfExporter.exportConsultationSummary(c, lines, noteBlock)
    }

    fun exportPortableBackupJson(): String {
        val cur = _state.value
        val now = currentTimeMillis()
        val payload = KmpPortableBackup(
            exportedAtMillis = now,
            consultations = cur.consultations,
            questions = cur.questions,
            notes = cur.notes,
            followUps = cur.followUps,
            attachments = cur.attachments,
            reminderSettings = cur.reminderSettings
        )
        mutateState(markDirtyForCloud = false) { it.copy(lastBackupAtMillis = now) }
        return json.encodeToString(payload)
    }

    fun restorePortableBackupJson(rawJson: String): KmpActionOutcome<Int> {
        return try {
            val backup = json.decodeFromString<KmpPortableBackup>(rawJson)
            mutateState(markDirtyForCloud = true, rescheduleAlarms = true) { cur ->
                cur.copy(
                    consultations = backup.consultations,
                    questions = backup.questions,
                    notes = backup.notes,
                    followUps = backup.followUps,
                    attachments = backup.attachments,
                    reminderSettings = backup.reminderSettings
                )
            }
            KmpActionOutcome.Ok(backup.consultations.size)
        } catch (_: Exception) {
            KmpActionOutcome.Err("Berkas cadangan tidak valid atau rusak.")
        }
    }

    // --- Cloud Auth, Sync, Account Switch Protection & Auto-Sync ---

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        privacyAccepted: Boolean,
        serverBaseUrl: String = SupabaseKmpApiClient.DEFAULT_BASE_URL
    ): KmpActionOutcome<SyncPushResponse> = syncMutex.withLock {
        try {
            val auth = apiClient.register(RegisterRequest(email, password, displayName, privacyAccepted), serverBaseUrl)
            val session = auth.toSession(serverBaseUrl)
            prepareLocalStateForAccount(session.userId)
            mutateState(markDirtyForCloud = false) { it.copy(accountSession = session) }
            uploadedAttachmentIdsInSession.clear()
            syncNowInternal(session, forceFullPull = true)
        } catch (e: KmpCloudApiException) {
            KmpActionOutcome.Err(e.message)
        } catch (_: Exception) {
            KmpActionOutcome.Err("Gagal menghubungi server Supabase.")
        }
    }

    suspend fun login(
        email: String,
        password: String,
        serverBaseUrl: String = SupabaseKmpApiClient.DEFAULT_BASE_URL
    ): KmpActionOutcome<SyncPushResponse> = syncMutex.withLock {
        try {
            val auth = apiClient.login(LoginRequest(email, password), serverBaseUrl)
            val session = auth.toSession(serverBaseUrl)
            prepareLocalStateForAccount(session.userId)
            mutateState(markDirtyForCloud = false) { it.copy(accountSession = session) }
            uploadedAttachmentIdsInSession.clear()
            syncNowInternal(session, forceFullPull = true)
        } catch (e: KmpCloudApiException) {
            KmpActionOutcome.Err(e.message)
        } catch (_: Exception) {
            KmpActionOutcome.Err("Gagal menghubungi server Supabase.")
        }
    }

    suspend fun forgotPassword(email: String, serverBaseUrl: String): KmpActionOutcome<String> {
        return try {
            val res = apiClient.forgotPassword(email, serverBaseUrl)
            KmpActionOutcome.Ok(res.message)
        } catch (e: KmpCloudApiException) {
            KmpActionOutcome.Err(e.message)
        } catch (_: Exception) {
            KmpActionOutcome.Err("Gagal menghubungi server untuk kode OTP.")
        }
    }

    suspend fun resetPassword(
        email: String,
        otpCode: String,
        newPassword: String,
        serverBaseUrl: String
    ): KmpActionOutcome<Unit> {
        return try {
            apiClient.resetPassword(ResetPasswordRequest(email, otpCode, newPassword), serverBaseUrl)
            KmpActionOutcome.Ok(Unit)
        } catch (e: KmpCloudApiException) {
            KmpActionOutcome.Err(e.message)
        } catch (_: Exception) {
            KmpActionOutcome.Err("Gagal mereset kata sandi.")
        }
    }

    suspend fun syncNow(forceFullRestore: Boolean = false): KmpActionOutcome<SyncPushResponse> = syncMutex.withLock {
        val session = _state.value.accountSession
            ?: return@withLock KmpActionOutcome.Err("Belum masuk ke akun.")
        if (forceFullRestore) {
            uploadedAttachmentIdsInSession.clear()
        }
        return@withLock try {
            syncNowWithAutoRefresh(session, forceFullRestore)
        } catch (e: KmpCloudApiException) {
            KmpActionOutcome.Err(e.message)
        } catch (_: Exception) {
            KmpActionOutcome.Err("Tidak dapat terhubung ke server. Data lokal Anda tetap aman.")
        }
    }

    suspend fun syncIfPendingOrStale(maxStaleMillis: Long = 5 * 60_000L): KmpActionOutcome<SyncPushResponse>? {
        val st = _state.value
        val session = st.accountSession ?: return null
        val isStale = (currentTimeMillis() - (session.lastSyncAtMillis ?: 0L)) >= maxStaleMillis
        if (!st.hasUnsyncedCloudChanges && !isStale) return null
        return syncNow(forceFullRestore = false)
    }

    suspend fun logout(forceWithoutBackup: Boolean = false): KmpActionOutcome<Unit> = syncMutex.withLock {
        val session = _state.value.accountSession
        if (session != null && !forceWithoutBackup) {
            val backup = try {
                syncNowWithAutoRefresh(session, forceFullPull = false)
            } catch (e: KmpCloudApiException) {
                KmpActionOutcome.Err("Gagal mencadangkan data akun ke cloud sebelum keluar (${e.message}). Data lokal Anda tetap aman.")
            } catch (_: Exception) {
                KmpActionOutcome.Err("Tidak dapat terhubung ke cloud untuk mencadangkan data sebelum keluar. Periksa internet Anda agar tidak ada data yang hilang.")
            }
            if (backup is KmpActionOutcome.Err) {
                return@withLock backup
            }
        }
        if (session != null) {
            apiClient.logout(session.accessToken, session.serverBaseUrl)
        }
        wipeAllLocalAccountState()
        return@withLock KmpActionOutcome.Ok(Unit)
    }

    suspend fun deleteAccountPermanently(password: String): KmpActionOutcome<Unit> = syncMutex.withLock {
        val session = _state.value.accountSession
            ?: return@withLock KmpActionOutcome.Err("Belum masuk ke akun.")
        return@withLock try {
            try {
                apiClient.deleteAccount(session.accessToken, password, session.serverBaseUrl)
            } catch (e: KmpCloudApiException) {
                if (e.statusCode != 401) throw e
                val refreshed = apiClient.refresh(session.refreshToken, session.serverBaseUrl).toSession(session.serverBaseUrl)
                    .copy(lastSyncAtMillis = session.lastSyncAtMillis)
                mutateState(markDirtyForCloud = false) { it.copy(accountSession = refreshed) }
                apiClient.deleteAccount(refreshed.accessToken, password, refreshed.serverBaseUrl)
            }
            wipeAllLocalAccountState()
            KmpActionOutcome.Ok(Unit)
        } catch (e: KmpCloudApiException) {
            KmpActionOutcome.Err(e.message)
        } catch (_: Exception) {
            KmpActionOutcome.Err("Gagal menghubungi server untuk menghapus akun.")
        }
    }

    private fun prepareLocalStateForAccount(userId: String) {
        val prevOwner = _state.value.localDataOwnerUserId
        if (prevOwner != null && prevOwner != userId) {
            wipeAllLocalAccountState()
        }
        mutateState(markDirtyForCloud = false) { it.copy(localDataOwnerUserId = userId) }
    }

    private fun wipeAllLocalAccountState() {
        uploadedAttachmentIdsInSession.clear()
        _state.value.consultations.forEach { c ->
            listOf("h1", "30m", "at", "custom", "prep").forEach { alarmScheduler.cancelAlarm("visit_${c.id}_$it") }
        }
        localStore.wipeAllAttachments()
        mutateState(markDirtyForCloud = false, rescheduleAlarms = false) { cur ->
            KmpLocalDatabaseState(
                noticeAccepted = cur.noticeAccepted,
                appLockEnabled = cur.appLockEnabled,
                appLockTimeoutMinutes = cur.appLockTimeoutMinutes,
                reminderSettings = defaultReminderSettingsDto(0L)
            )
        }
    }

    private suspend fun syncNowWithAutoRefresh(
        session: KmpAccountSession,
        forceFullPull: Boolean
    ): KmpActionOutcome<SyncPushResponse> {
        return try {
            syncNowInternal(session, forceFullPull)
        } catch (e: KmpCloudApiException) {
            if (e.statusCode != 401) throw e
            val refreshed = apiClient.refresh(session.refreshToken, session.serverBaseUrl).toSession(session.serverBaseUrl)
                .copy(lastSyncAtMillis = session.lastSyncAtMillis)
            mutateState(markDirtyForCloud = false) { it.copy(accountSession = refreshed) }
            syncNowInternal(refreshed, forceFullPull)
        }
    }

    private suspend fun syncNowInternal(
        session: KmpAccountSession,
        forceFullPull: Boolean
    ): KmpActionOutcome<SyncPushResponse> {
        val st = _state.value
        val snapshotChangeMillis = st.lastDataChangeMillis
        val lastSync = if (forceFullPull) 0L else (session.lastSyncAtMillis ?: 0L)

        val pushConsultations = st.consultations + st.tombstones
            .filter { it.entityType == KmpSyncEntityType.CONSULTATION }
            .map { SyncConsultationDto(it.entityId, "", 0L, "", status = "CANCELLED", createdAtMillis = it.deletedAtMillis, updatedAtMillis = it.deletedAtMillis, deletedAtMillis = it.deletedAtMillis) }
        val pushQuestions = st.questions + st.tombstones
            .filter { it.entityType == KmpSyncEntityType.QUESTION }
            .map { SyncQuestionDto(it.entityId, "", "", false, false, it.deletedAtMillis, it.deletedAtMillis, deletedAtMillis = it.deletedAtMillis) }
        val pushNotes = st.notes + st.tombstones
            .filter { it.entityType == KmpSyncEntityType.NOTE }
            .map { SyncNoteDto(it.entityId, updatedAtMillis = it.deletedAtMillis, deletedAtMillis = it.deletedAtMillis) }
        val pushFollowUps = st.followUps + st.tombstones
            .filter { it.entityType == KmpSyncEntityType.FOLLOW_UP }
            .map { SyncFollowUpDto(it.entityId, "", "", isCompleted = false, createdAtMillis = it.deletedAtMillis, updatedAtMillis = it.deletedAtMillis, deletedAtMillis = it.deletedAtMillis) }
        val pushAttachments = st.attachments + st.tombstones
            .filter { it.entityType == KmpSyncEntityType.ATTACHMENT }
            .map { SyncAttachmentMetaDto(it.entityId, "", target = "ADDITIONAL_NOTE", kind = "PHOTO", fileName = "deleted", mimeType = "application/octet-stream", sizeBytes = 0L, createdAtMillis = it.deletedAtMillis, updatedAtMillis = it.deletedAtMillis, deletedAtMillis = it.deletedAtMillis) }

        val pushRes = apiClient.pushSync(
            accessToken = session.accessToken,
            request = SyncPushRequest(
                consultations = pushConsultations,
                questions = pushQuestions,
                notes = pushNotes,
                followUps = pushFollowUps,
                attachments = pushAttachments,
                reminderSettings = st.reminderSettings.copy(updatedAtMillis = currentTimeMillis())
            ),
            baseUrl = session.serverBaseUrl
        )

        st.attachments.forEach { att ->
            if (!forceFullPull && uploadedAttachmentIdsInSession.contains(att.id)) return@forEach
            val bytes = localStore.readAttachmentBytes(att.consultationId, att.fileName)
            if (bytes != null && bytes.isNotEmpty()) {
                apiClient.uploadAttachmentFile(session.accessToken, session.userId, att.id, bytes, att.mimeType, session.serverBaseUrl)
                uploadedAttachmentIdsInSession.add(att.id)
            }
        }

        val pulled = apiClient.pullSync(session.accessToken, lastSync, session.serverBaseUrl)

        pulled.attachments.filter { it.deletedAtMillis == null }.forEach { remoteAtt ->
            val existingLocal = localStore.readAttachmentBytes(remoteAtt.consultationId, remoteAtt.fileName)
            if (existingLocal == null) {
                val downloaded = apiClient.downloadAttachmentFile(session.accessToken, session.userId, remoteAtt.id, session.serverBaseUrl)
                if (downloaded != null) {
                    localStore.writeAttachmentBytes(remoteAtt.consultationId, remoteAtt.fileName, downloaded)
                }
            }
        }

        val syncTs = maxOf(pushRes.serverTimestampMillis, pulled.serverTimestampMillis)
        val sentTombstoneIds = st.tombstones.map { it.entityId }.toSet()

        mutateState(markDirtyForCloud = false, rescheduleAlarms = true) { cur ->
            val mergedConsultations = mergeById(
                cur.consultations,
                pulled.consultations,
                idOf = { it.id },
                updatedOf = { it.updatedAtMillis },
                deletedOf = { it.deletedAtMillis },
                onDelete = { localStore.deleteConsultationFiles(it) }
            ).sortedBy { it.appointmentAtMillis }

            val mergedQuestions = mergeById(
                cur.questions,
                pulled.questions,
                idOf = { it.id },
                updatedOf = { it.updatedAtMillis },
                deletedOf = { it.deletedAtMillis }
            )

            val mergedNotes = mergeById(
                cur.notes,
                pulled.notes,
                idOf = { it.consultationId },
                updatedOf = { it.updatedAtMillis },
                deletedOf = { it.deletedAtMillis }
            )

            val mergedFollowUps = mergeById(
                cur.followUps,
                pulled.followUps,
                idOf = { it.id },
                updatedOf = { it.updatedAtMillis },
                deletedOf = { it.deletedAtMillis }
            )

            val mergedAttachments = mergeById(
                cur.attachments,
                pulled.attachments,
                idOf = { it.id },
                updatedOf = { it.updatedAtMillis },
                deletedOf = { it.deletedAtMillis }
            )

            val nextSettings = pulled.reminderSettings?.let { remote ->
                if (remote.updatedAtMillis > cur.reminderSettings.updatedAtMillis) remote else cur.reminderSettings
            } ?: cur.reminderSettings

            val stillDirty = cur.lastDataChangeMillis > snapshotChangeMillis
            cur.copy(
                consultations = mergedConsultations,
                questions = mergedQuestions,
                notes = mergedNotes,
                followUps = mergedFollowUps,
                attachments = mergedAttachments,
                reminderSettings = nextSettings,
                tombstones = cur.tombstones.filterNot { it.entityId in sentTombstoneIds },
                accountSession = cur.accountSession?.copy(lastSyncAtMillis = syncTs) ?: session.copy(lastSyncAtMillis = syncTs),
                localDataOwnerUserId = session.userId,
                hasUnsyncedCloudChanges = stillDirty
            )
        }
        return KmpActionOutcome.Ok(pushRes)
    }

    private fun <T> mergeById(
        localList: List<T>,
        remoteList: List<T>,
        idOf: (T) -> String,
        updatedOf: (T) -> Long,
        deletedOf: (T) -> Long?,
        onDelete: ((String) -> Unit)? = null
    ): List<T> {
        val map = localList.associateBy(idOf).toMutableMap()
        remoteList.forEach { remote ->
            val id = idOf(remote)
            val existing = map[id]
            val del = deletedOf(remote)
            if (del != null) {
                if (existing == null || del >= updatedOf(existing)) {
                    map.remove(id)
                    onDelete?.invoke(id)
                }
            } else if (existing == null || updatedOf(remote) > updatedOf(existing)) {
                map[id] = remote
            }
        }
        return map.values.toList()
    }

    private fun AuthResponse.toSession(baseUrl: String): KmpAccountSession = KmpAccountSession(
        userId = user.userId,
        email = user.email,
        displayName = user.displayName,
        accessToken = accessToken,
        refreshToken = refreshToken,
        lastSyncAtMillis = null,
        serverBaseUrl = baseUrl
    )
}
