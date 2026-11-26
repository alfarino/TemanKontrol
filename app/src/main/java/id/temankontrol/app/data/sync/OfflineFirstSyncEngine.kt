package id.temankontrol.app.data.sync

import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.data.local.AppDatabase
import id.temankontrol.app.data.local.entity.*
import id.temankontrol.app.data.media.MediaAttachmentManager
import id.temankontrol.app.data.remote.CloudApiException
import id.temankontrol.app.data.remote.TemanKontrolApiClient
import id.temankontrol.app.domain.model.AccountSession
import id.temankontrol.app.domain.model.ReminderSettings
import id.temankontrol.app.domain.model.SyncEntityType
import id.temankontrol.app.domain.model.SyncSummaryResult
import id.temankontrol.app.domain.repository.PreferencesRepository
import id.temankontrol.app.domain.validation.Validation
import id.temankontrol.shared.dto.*
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class LocalSyncSnapshot(
    val consultations: List<SyncConsultationDto>,
    val questions: List<SyncQuestionDto>,
    val notes: List<SyncNoteDto>,
    val followUps: List<SyncFollowUpDto>,
    val attachments: List<SyncAttachmentMetaDto>
)

interface LocalSyncStorage {
    suspend fun readSnapshot(): LocalSyncSnapshot
    suspend fun applyPulledChanges(pulled: SyncPullResponse): Int
    suspend fun readAttachmentBytes(consultationId: String, fileName: String, uriString: String?): ByteArray?
    suspend fun writeDownloadedAttachment(meta: SyncAttachmentMetaDto, bytes: ByteArray): String
    suspend fun wipeAllLocalUserData()
}

@Singleton
class RoomLocalSyncStorage @Inject constructor(
    private val database: AppDatabase,
    private val mediaManager: MediaAttachmentManager
) : LocalSyncStorage {

    override suspend fun readSnapshot(): LocalSyncSnapshot {
        val consultations = database.consultationDao().getAll().map { c ->
            SyncConsultationDto(
                id = c.id,
                title = c.title,
                appointmentAtMillis = c.appointmentAt,
                reason = c.reason,
                facilityName = c.facilityName,
                providerName = c.providerName,
                status = c.status,
                createdAtMillis = c.createdAt,
                updatedAtMillis = c.updatedAt
            )
        }
        val questions = database.questionDao().getAll().map { q ->
            SyncQuestionDto(
                id = q.id,
                consultationId = q.consultationId,
                text = q.text,
                isPriority = q.isPriority,
                isDiscussed = q.isDiscussed,
                createdAtMillis = q.createdAt,
                updatedAtMillis = q.updatedAt,
                answer = q.answer,
                answerUpdatedAtMillis = q.answerUpdatedAt
            )
        }
        val notes = database.noteDao().getAll().map { n ->
            SyncNoteDto(
                consultationId = n.consultationId,
                summary = n.summary,
                providerInstruction = n.providerInstruction,
                additionalNotes = n.additionalNotes,
                updatedAtMillis = n.updatedAt
            )
        }
        val followUps = database.followUpDao().getAll().map { f ->
            SyncFollowUpDto(
                id = f.id,
                consultationId = f.consultationId,
                text = f.text,
                dueDateEpochDay = f.dueDate,
                isCompleted = f.isCompleted,
                completedAtMillis = f.completedAt,
                createdAtMillis = f.createdAt,
                updatedAtMillis = f.updatedAt
            )
        }
        val attachments = database.attachmentDao().getAll().map { a ->
            SyncAttachmentMetaDto(
                id = a.id,
                consultationId = a.consultationId,
                questionId = a.questionId,
                target = a.target,
                kind = a.kind,
                fileName = a.fileName,
                mimeType = a.mimeType,
                sizeBytes = a.sizeBytes,
                durationMs = a.durationMs,
                caption = a.caption,
                createdAtMillis = a.createdAt,
                updatedAtMillis = a.createdAt
            )
        }
        return LocalSyncSnapshot(consultations, questions, notes, followUps, attachments)
    }

    override suspend fun applyPulledChanges(pulled: SyncPullResponse): Int {
        var applied = 0
        val cDao = database.consultationDao()
        pulled.consultations.forEach { dto ->
            val existing = cDao.getById(dto.id)
            if (dto.deletedAtMillis != null) {
                if (existing == null || dto.deletedAtMillis!! >= existing.updatedAt) {
                    if (existing != null) {
                        cDao.delete(dto.id)
                        mediaManager.deleteConsultationFolder(dto.id)
                        applied++
                    }
                }
            } else if (existing == null || dto.updatedAtMillis > existing.updatedAt) {
                cDao.upsertSync(
                    ConsultationEntity(
                        id = dto.id,
                        title = dto.title,
                        appointmentAt = dto.appointmentAtMillis,
                        reason = dto.reason,
                        facilityName = dto.facilityName,
                        providerName = dto.providerName,
                        status = dto.status,
                        createdAt = dto.createdAtMillis,
                        updatedAt = dto.updatedAtMillis
                    )
                )
                applied++
            }
        }

        val qDao = database.questionDao()
        pulled.questions.forEach { dto ->
            val existing = qDao.getById(dto.id)
            if (dto.deletedAtMillis != null) {
                if (existing != null && dto.deletedAtMillis!! >= existing.updatedAt) {
                    qDao.delete(dto.id)
                    applied++
                }
            } else if (existing == null || dto.updatedAtMillis > existing.updatedAt) {
                qDao.upsertSync(
                    QuestionEntity(
                        id = dto.id,
                        consultationId = dto.consultationId,
                        text = dto.text,
                        textNormalized = Validation.normalizeQuestionForDuplicateCheck(dto.text),
                        isPriority = dto.isPriority,
                        isDiscussed = dto.isDiscussed,
                        createdAt = dto.createdAtMillis,
                        updatedAt = dto.updatedAtMillis,
                        answer = dto.answer,
                        answerUpdatedAt = dto.answerUpdatedAtMillis
                    )
                )
                applied++
            }
        }

        val nDao = database.noteDao()
        pulled.notes.forEach { dto ->
            val existing = nDao.getByConsultationId(dto.consultationId)
            if (dto.deletedAtMillis != null) {
                if (existing != null && dto.deletedAtMillis!! >= existing.updatedAt) {
                    nDao.delete(dto.consultationId)
                    applied++
                }
            } else if (existing == null || dto.updatedAtMillis > existing.updatedAt) {
                nDao.upsert(
                    ConsultationNoteEntity(
                        consultationId = dto.consultationId,
                        summary = dto.summary,
                        providerInstruction = dto.providerInstruction,
                        additionalNotes = dto.additionalNotes,
                        updatedAt = dto.updatedAtMillis
                    )
                )
                applied++
            }
        }

        val fDao = database.followUpDao()
        pulled.followUps.forEach { dto ->
            val existing = fDao.getById(dto.id)
            if (dto.deletedAtMillis != null) {
                if (existing != null && dto.deletedAtMillis!! >= existing.updatedAt) {
                    fDao.delete(dto.id)
                    applied++
                }
            } else if (existing == null || dto.updatedAtMillis > existing.updatedAt) {
                fDao.upsertSync(
                    FollowUpActionEntity(
                        id = dto.id,
                        consultationId = dto.consultationId,
                        text = dto.text,
                        dueDate = dto.dueDateEpochDay,
                        isCompleted = dto.isCompleted,
                        completedAt = dto.completedAtMillis,
                        createdAt = dto.createdAtMillis,
                        updatedAt = dto.updatedAtMillis
                    )
                )
                applied++
            }
        }

        val aDao = database.attachmentDao()
        pulled.attachments.forEach { dto ->
            val existing = aDao.getById(dto.id)
            if (dto.deletedAtMillis != null) {
                if (existing != null && dto.deletedAtMillis!! >= existing.createdAt) {
                    aDao.delete(dto.id)
                    mediaManager.deleteFile(existing.consultationId, existing.fileName)
                    applied++
                }
            } else if (existing == null || dto.updatedAtMillis > existing.createdAt) {
                val targetFile = resolveAttachmentFile(dto.consultationId, dto.fileName)
                val uriStr = if (targetFile.exists()) android.net.Uri.fromFile(targetFile).toString() else existing?.uriString
                aDao.upsertSync(
                    AttachmentEntity(
                        id = dto.id,
                        consultationId = dto.consultationId,
                        questionId = dto.questionId,
                        target = dto.target,
                        kind = dto.kind,
                        fileName = dto.fileName,
                        mimeType = dto.mimeType,
                        sizeBytes = dto.sizeBytes,
                        durationMs = dto.durationMs,
                        caption = dto.caption,
                        uriString = uriStr,
                        createdAt = dto.createdAtMillis
                    )
                )
                applied++
            }
        }

        return applied
    }

    override suspend fun readAttachmentBytes(consultationId: String, fileName: String, uriString: String?): ByteArray? {
        val byPath = resolveAttachmentFile(consultationId, fileName)
        if (byPath.exists() && byPath.length() > 0L) return byPath.readBytes()
        if (!uriString.isNullOrBlank()) {
            val uriFile = runCatching { android.net.Uri.parse(uriString).path?.let(::File) }.getOrNull()
            if (uriFile != null && uriFile.exists() && uriFile.length() > 0L) return uriFile.readBytes()
        }
        return null
    }

    override suspend fun writeDownloadedAttachment(meta: SyncAttachmentMetaDto, bytes: ByteArray): String {
        val targetFile = resolveAttachmentFile(meta.consultationId, meta.fileName)
        targetFile.parentFile?.mkdirs()
        targetFile.writeBytes(bytes)
        val uriStr = android.net.Uri.fromFile(targetFile).toString()
        database.attachmentDao().upsertSync(
            AttachmentEntity(
                id = meta.id,
                consultationId = meta.consultationId,
                questionId = meta.questionId,
                target = meta.target,
                kind = meta.kind,
                fileName = meta.fileName,
                mimeType = meta.mimeType,
                sizeBytes = bytes.size.toLong(),
                durationMs = meta.durationMs,
                caption = meta.caption,
                uriString = uriStr,
                createdAt = meta.createdAtMillis
            )
        )
        return uriStr
    }

    override suspend fun wipeAllLocalUserData() {
        database.consultationDao().deleteAll()
        database.reminderLogDao().deleteOlderThan(Long.MAX_VALUE)
        mediaManager.attachmentsRootDir().deleteRecursively()
    }

    private fun resolveAttachmentFile(consultationId: String, fileName: String): File {
        val folder = File(mediaManager.attachmentsRootDir(), consultationId)
        return File(folder, fileName)
    }
}

@Singleton
class OfflineFirstSyncEngine @Inject constructor(
    private val apiClient: TemanKontrolApiClient,
    private val storage: LocalSyncStorage,
    private val preferencesRepository: PreferencesRepository
) {

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        privacyAccepted: Boolean,
        serverBaseUrl: String = TemanKontrolApiClient.DEFAULT_BASE_URL
    ): AppResult<SyncSummaryResult> {
        return try {
            val auth = apiClient.register(
                RegisterRequest(email, password, displayName, privacyAccepted),
                baseUrl = serverBaseUrl
            )
            val session = auth.toAccountSession(serverBaseUrl)
            preferencesRepository.saveAccountSession(session)
            // Merge guest local data & sync immediately
            syncNowInternal(session, forceFullPull = true)
        } catch (e: CloudApiException) {
            AppResult.Failure(AppError.Validation("auth", e.message))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    suspend fun login(
        email: String,
        password: String,
        serverBaseUrl: String = TemanKontrolApiClient.DEFAULT_BASE_URL
    ): AppResult<SyncSummaryResult> {
        return try {
            val auth = apiClient.login(
                LoginRequest(email, password, deviceName = "TemanKontrol Mobile"),
                baseUrl = serverBaseUrl
            )
            val session = auth.toAccountSession(serverBaseUrl)
            preferencesRepository.saveAccountSession(session)
            // Merge existing guest records and pull full cloud archive (since=0)
            syncNowInternal(session, forceFullPull = true)
        } catch (e: CloudApiException) {
            AppResult.Failure(AppError.Validation("auth", e.message))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    suspend fun syncNow(forceFullRestore: Boolean = false): AppResult<SyncSummaryResult> {
        val session = preferencesRepository.accountSession.first()
            ?: return AppResult.Failure(AppError.Validation("auth", "Belum masuk ke akun."))
        return try {
            syncNowInternal(session, forceFullPull = forceFullRestore)
        } catch (e: CloudApiException) {
            if (e.statusCode == 401) {
                return tryRefreshAndRetry(session, forceFullRestore)
            }
            AppResult.Failure(AppError.Validation("sync", e.message))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Validation("sync", "Tidak dapat terhubung ke server. Perubahan Anda tetap aman tersimpan lokal."))
        }
    }

    suspend fun logout(): AppResult<Unit> {
        val session = preferencesRepository.accountSession.first()
        if (session != null) {
            apiClient.logout(session.refreshToken, session.serverBaseUrl)
        }
        preferencesRepository.saveAccountSession(null)
        return AppResult.Success(Unit)
    }

    suspend fun deleteAccountPermanently(password: String): AppResult<Unit> {
        val session = preferencesRepository.accountSession.first()
            ?: return AppResult.Failure(AppError.Validation("auth", "Belum masuk ke akun."))
        return try {
            withAutoRefresh(session) { active ->
                apiClient.deleteAccount(active.accessToken, password, active.serverBaseUrl)
            }
            storage.wipeAllLocalUserData()
            preferencesRepository.saveAccountSession(null)
            AppResult.Success(Unit)
        } catch (e: CloudApiException) {
            AppResult.Failure(AppError.Validation("delete", e.message))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Validation("delete", "Gagal menghubungi server untuk menghapus akun."))
        }
    }

    private suspend fun tryRefreshAndRetry(
        session: AccountSession,
        forceFullPull: Boolean
    ): AppResult<SyncSummaryResult> {
        return try {
            val refreshedAuth = apiClient.refresh(session.refreshToken, session.serverBaseUrl)
            val newSession = refreshedAuth.toAccountSession(session.serverBaseUrl)
                .copy(lastSyncAtMillis = session.lastSyncAtMillis)
            preferencesRepository.saveAccountSession(newSession)
            syncNowInternal(newSession, forceFullPull)
        } catch (e: CloudApiException) {
            AppResult.Failure(AppError.Validation("auth", e.message))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Validation("sync", "Sesi tidak dapat diperbarui saat offline."))
        }
    }

    private suspend fun <T> withAutoRefresh(
        session: AccountSession,
        block: suspend (AccountSession) -> T
    ): T {
        return try {
            block(session)
        } catch (e: CloudApiException) {
            if (e.statusCode != 401) throw e
            val refreshed = apiClient.refresh(session.refreshToken, session.serverBaseUrl)
            val nextSession = refreshed.toAccountSession(session.serverBaseUrl)
                .copy(lastSyncAtMillis = session.lastSyncAtMillis)
            preferencesRepository.saveAccountSession(nextSession)
            block(nextSession)
        }
    }

    private suspend fun syncNowInternal(
        session: AccountSession,
        forceFullPull: Boolean
    ): AppResult<SyncSummaryResult> {
        val local = storage.readSnapshot()
        val tombstones = preferencesRepository.pendingTombstones.first()
        val settings = preferencesRepository.reminderSettings.first()
        val lastSync = if (forceFullPull) 0L else (session.lastSyncAtMillis ?: 0L)

        // Merge live local rows + pending delete tombstones
        val pushConsultations = local.consultations + tombstones
            .filter { it.entityType == SyncEntityType.CONSULTATION }
            .map {
                SyncConsultationDto(
                    id = it.entityId,
                    title = "",
                    appointmentAtMillis = 0L,
                    reason = "",
                    status = "CANCELLED",
                    createdAtMillis = it.deletedAtMillis,
                    updatedAtMillis = it.deletedAtMillis,
                    deletedAtMillis = it.deletedAtMillis
                )
            }

        val pushQuestions = local.questions + tombstones
            .filter { it.entityType == SyncEntityType.QUESTION }
            .map {
                SyncQuestionDto(
                    id = it.entityId,
                    consultationId = "",
                    text = "",
                    isPriority = false,
                    isDiscussed = false,
                    createdAtMillis = it.deletedAtMillis,
                    updatedAtMillis = it.deletedAtMillis,
                    deletedAtMillis = it.deletedAtMillis
                )
            }

        val pushNotes = local.notes + tombstones
            .filter { it.entityType == SyncEntityType.NOTE }
            .map {
                SyncNoteDto(
                    consultationId = it.entityId,
                    updatedAtMillis = it.deletedAtMillis,
                    deletedAtMillis = it.deletedAtMillis
                )
            }

        val pushFollowUps = local.followUps + tombstones
            .filter { it.entityType == SyncEntityType.FOLLOW_UP }
            .map {
                SyncFollowUpDto(
                    id = it.entityId,
                    consultationId = "",
                    text = "",
                    isCompleted = false,
                    createdAtMillis = it.deletedAtMillis,
                    updatedAtMillis = it.deletedAtMillis,
                    deletedAtMillis = it.deletedAtMillis
                )
            }

        val pushAttachments = local.attachments + tombstones
            .filter { it.entityType == SyncEntityType.ATTACHMENT }
            .map {
                SyncAttachmentMetaDto(
                    id = it.entityId,
                    consultationId = "",
                    target = "ADDITIONAL_NOTE",
                    kind = "PHOTO",
                    fileName = "deleted",
                    mimeType = "application/octet-stream",
                    sizeBytes = 0L,
                    createdAtMillis = it.deletedAtMillis,
                    updatedAtMillis = it.deletedAtMillis,
                    deletedAtMillis = it.deletedAtMillis
                )
            }

        val pushRes = apiClient.pushSync(
            accessToken = session.accessToken,
            request = SyncPushRequest(
                consultations = pushConsultations,
                questions = pushQuestions,
                notes = pushNotes,
                followUps = pushFollowUps,
                attachments = pushAttachments,
                reminderSettings = settings.toSyncDto(System.currentTimeMillis())
            ),
            baseUrl = session.serverBaseUrl
        )

        if (tombstones.isNotEmpty()) {
            preferencesRepository.clearSyncTombstones(tombstones.map { it.entityId }.toSet())
        }

        // Upload local binary attachment files that exist on device
        var uploadedFiles = 0
        local.attachments.forEach { att ->
            val bytes = storage.readAttachmentBytes(att.consultationId, att.fileName, null)
            if (bytes != null && bytes.isNotEmpty()) {
                apiClient.uploadAttachmentFile(session.accessToken, att.id, bytes, session.serverBaseUrl)
                uploadedFiles++
            }
        }

        // Pull updates since lastSync (or 0 for full restore)
        val pulled = apiClient.pullSync(
            accessToken = session.accessToken,
            sinceMillis = lastSync,
            baseUrl = session.serverBaseUrl
        )

        val pulledApplied = storage.applyPulledChanges(pulled)

        // Sync reminder settings if present on server
        pulled.reminderSettings?.let { remoteSettings ->
            preferencesRepository.updateReminderSettings { current ->
                current.copy(
                    visitReminderEnabled = remoteSettings.visitReminderEnabled,
                    visitReminderHour = remoteSettings.visitReminderHour,
                    visitThirtyMinBeforeEnabled = remoteSettings.visitThirtyMinBeforeEnabled,
                    visitAtTimeEnabled = remoteSettings.visitAtTimeEnabled,
                    customLeadMinutes = remoteSettings.customLeadMinutes,
                    alarmSoundModeEnabled = remoteSettings.alarmSoundModeEnabled,
                    prepareReminderEnabled = remoteSettings.prepareReminderEnabled,
                    followUpReminderEnabled = remoteSettings.followUpReminderEnabled,
                    followUpReminderHour = remoteSettings.followUpReminderHour,
                    backupReminderEnabled = remoteSettings.backupReminderEnabled,
                    showTitleOnLockScreen = remoteSettings.showTitleOnLockScreen
                )
            }
        }

        // Download any active remote attachments whose binary file is missing locally
        var downloadedFiles = 0
        pulled.attachments
            .filter { it.deletedAtMillis == null }
            .forEach { remoteAtt ->
                val localBytes = storage.readAttachmentBytes(remoteAtt.consultationId, remoteAtt.fileName, null)
                if (localBytes == null) {
                    val bytes = apiClient.downloadAttachmentFile(
                        session.accessToken,
                        remoteAtt.id,
                        session.serverBaseUrl
                    )
                    if (bytes != null) {
                        storage.writeDownloadedAttachment(remoteAtt, bytes)
                        downloadedFiles++
                    }
                }
            }

        val syncTs = maxOf(pushRes.serverTimestampMillis, pulled.serverTimestampMillis)
        preferencesRepository.updateLastSyncAt(syncTs)

        return AppResult.Success(
            SyncSummaryResult(
                pushedRecords = pushRes.acceptedCount,
                pulledRecords = pulledApplied,
                uploadedFiles = uploadedFiles,
                downloadedFiles = downloadedFiles,
                syncedAtMillis = syncTs
            )
        )
    }

    private fun AuthResponse.toAccountSession(serverBaseUrl: String): AccountSession = AccountSession(
        userId = user.userId,
        email = user.email,
        displayName = user.displayName,
        accessToken = accessToken,
        refreshToken = refreshToken,
        lastSyncAtMillis = null,
        serverBaseUrl = serverBaseUrl
    )

    private fun ReminderSettings.toSyncDto(nowMs: Long): SyncReminderSettingsDto = SyncReminderSettingsDto(
        visitReminderEnabled = visitReminderEnabled,
        visitReminderHour = visitReminderHour,
        visitThirtyMinBeforeEnabled = visitThirtyMinBeforeEnabled,
        visitAtTimeEnabled = visitAtTimeEnabled,
        customLeadMinutes = customLeadMinutes,
        alarmSoundModeEnabled = alarmSoundModeEnabled,
        prepareReminderEnabled = prepareReminderEnabled,
        followUpReminderEnabled = followUpReminderEnabled,
        followUpReminderHour = followUpReminderHour,
        backupReminderEnabled = backupReminderEnabled,
        showTitleOnLockScreen = showTitleOnLockScreen,
        updatedAtMillis = nowMs
    )
}
