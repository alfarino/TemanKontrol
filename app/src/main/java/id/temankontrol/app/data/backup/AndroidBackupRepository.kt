package id.temankontrol.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import id.temankontrol.app.BuildConfig
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppLogger
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.data.local.AppDatabase
import id.temankontrol.app.data.local.entity.*
import id.temankontrol.app.domain.repository.BackupExportResult
import id.temankontrol.app.domain.repository.BackupInspectResult
import id.temankontrol.app.domain.repository.BackupRepository
import id.temankontrol.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val preferencesRepository: PreferencesRepository
) : BackupRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun attachmentsRootDir(): File = File(context.filesDir, "attachments")

    override suspend fun exportToUri(uriString: String, password: String?): AppResult<BackupExportResult> =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val consultations = database.consultationDao().getAll()
                val questions = database.questionDao().getAll()
                val notes = database.noteDao().getAll()
                val followUps = database.followUpDao().getAll()
                val attachments = database.attachmentDao().getAll()

                val payload = BackupPayload(
                    consultations = consultations.map {
                        BackupConsultation(
                            id = it.id,
                            title = it.title,
                            appointmentAt = it.appointmentAt,
                            reason = it.reason,
                            facilityName = it.facilityName,
                            providerName = it.providerName,
                            status = it.status,
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt
                        )
                    },
                    questions = questions.map {
                        BackupQuestion(
                            id = it.id,
                            consultationId = it.consultationId,
                            text = it.text,
                            textNormalized = it.textNormalized,
                            isPriority = it.isPriority,
                            isDiscussed = it.isDiscussed,
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt,
                            answer = it.answer,
                            answerUpdatedAt = it.answerUpdatedAt
                        )
                    },
                    notes = notes.map {
                        BackupNote(
                            consultationId = it.consultationId,
                            summary = it.summary,
                            providerInstruction = it.providerInstruction,
                            additionalNotes = it.additionalNotes,
                            updatedAt = it.updatedAt
                        )
                    },
                    followUps = followUps.map {
                        BackupFollowUp(
                            id = it.id,
                            consultationId = it.consultationId,
                            text = it.text,
                            dueDate = it.dueDate,
                            isCompleted = it.isCompleted,
                            completedAt = it.completedAt,
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt
                        )
                    },
                    attachments = attachments.map {
                        BackupAttachment(
                            id = it.id,
                            consultationId = it.consultationId,
                            questionId = it.questionId,
                            target = it.target,
                            kind = it.kind,
                            fileName = it.fileName,
                            mimeType = it.mimeType,
                            sizeBytes = it.sizeBytes,
                            durationMs = it.durationMs,
                            caption = it.caption,
                            uriString = it.uriString,
                            createdAt = it.createdAt
                        )
                    }
                )

                val dataBytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
                val now = Instant.now()
                val manifest = BackupManifest(
                    formatVersion = 1,
                    appVersion = BuildConfig.VERSION_NAME,
                    dbSchemaVersion = 3,
                    createdAt = now.toEpochMilli(),
                    counts = BackupCounts(
                        consultations = consultations.size,
                        questions = questions.size,
                        notes = notes.size,
                        followUps = followUps.size,
                        attachments = attachments.size
                    ),
                    sha256Data = BackupCrypto.sha256Hex(dataBytes)
                )
                val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)

                val zipOut = ByteArrayOutputStream()
                ZipOutputStream(zipOut).use { zos ->
                    zos.putNextEntry(ZipEntry("manifest.json"))
                    zos.write(manifestBytes)
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("data.json"))
                    zos.write(dataBytes)
                    zos.closeEntry()

                    val root = attachmentsRootDir()
                    attachments.forEach { att ->
                        val localFile = File(root, "${att.consultationId}/${att.fileName}")
                        if (localFile.exists() && localFile.isFile) {
                            zos.putNextEntry(ZipEntry("attachments/${att.consultationId}/${att.fileName}"))
                            localFile.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }

                val containerBytes = BackupCrypto.packContainer(zipOut.toByteArray(), password)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(containerBytes)
                    out.flush()
                } ?: return@withContext AppResult.Failure(AppError.Storage)

                preferencesRepository.recordBackupCreated(now)
                val isLocal = uri.authority?.contains("externalstorage", ignoreCase = true) == true
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "TemanKontrol.tkbackup"
                AppLogger.d("Backup", "Exported backup bytes=${containerBytes.size}")
                AppResult.Success(BackupExportResult(fileName, containerBytes.size.toLong(), isLocal))
            } catch (e: Exception) {
                AppLogger.e("Backup", "Export failed", e)
                AppResult.Failure(AppError.Storage)
            }
        }

    override suspend fun inspectFromUri(uriString: String, password: String?): AppResult<BackupInspectResult> =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val containerBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext AppResult.Failure(AppError.BackupError("Berkas cadangan tidak dapat dibuka."))

                val isEncrypted = BackupCrypto.isContainerEncrypted(containerBytes)
                if (isEncrypted && password.isNullOrEmpty()) {
                    return@withContext AppResult.Success(
                        BackupInspectResult(
                            isEncrypted = true,
                            formatVersion = 1,
                            appVersion = "-",
                            createdAtMillis = 0L,
                            consultationCount = 0,
                            questionCount = 0,
                            attachmentCount = 0,
                            requiresPasswordPrompt = true
                        )
                    )
                }

                val zipBytes = BackupCrypto.unpackContainer(containerBytes, password)
                val (manifest, _) = readAndValidateZip(zipBytes)
                AppResult.Success(
                    BackupInspectResult(
                        isEncrypted = isEncrypted,
                        formatVersion = manifest.formatVersion,
                        appVersion = manifest.appVersion,
                        createdAtMillis = manifest.createdAt,
                        consultationCount = manifest.counts.consultations,
                        questionCount = manifest.counts.questions,
                        attachmentCount = manifest.counts.attachments,
                        requiresPasswordPrompt = false
                    )
                )
            } catch (e: UnsupportedOperationException) {
                AppResult.Failure(AppError.BackupError("Cadangan ini dibuat oleh versi Teman Kontrol yang lebih baru. Perbarui aplikasi lalu coba lagi."))
            } catch (e: SecurityException) {
                AppResult.Failure(AppError.BackupError("Cadangan tidak bisa dibuka. Periksa kata sandi atau pastikan berkas tidak rusak."))
            } catch (e: Exception) {
                AppLogger.e("Backup", "Inspect failed", e)
                AppResult.Failure(AppError.BackupError("Cadangan tidak bisa dibuka. Pastikan berkas .tkbackup tidak rusak."))
            }
        }

    override suspend fun restoreFromUri(uriString: String, password: String?): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            val stagingDir = File(context.cacheDir, "restore_staging_${System.currentTimeMillis()}")
            try {
                val uri = Uri.parse(uriString)
                val containerBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext AppResult.Failure(AppError.BackupError("Berkas cadangan tidak dapat dibaca."))

                val zipBytes = BackupCrypto.unpackContainer(containerBytes, password)
                val (manifest, payloadBytes) = readAndValidateZip(zipBytes)
                if (manifest.formatVersion > 1) {
                    return@withContext AppResult.Failure(
                        AppError.BackupError("Cadangan ini dibuat oleh versi Teman Kontrol yang lebih baru. Perbarui aplikasi lalu coba lagi.")
                    )
                }

                val payload = json.decodeFromString<BackupPayload>(payloadBytes.toString(Charsets.UTF_8))

                // Stage attachment binary files before touching the DB
                stagingDir.mkdirs()
                extractAttachmentsToStaging(zipBytes, stagingDir)

                // Atomic Room replacement
                database.withTransaction {
                    database.clearAllTables()
                    val cDao = database.consultationDao()
                    val qDao = database.questionDao()
                    val nDao = database.noteDao()
                    val fDao = database.followUpDao()
                    val aDao = database.attachmentDao()

                    payload.consultations.forEach {
                        cDao.insert(
                            ConsultationEntity(
                                id = it.id,
                                title = it.title,
                                appointmentAt = it.appointmentAt,
                                reason = it.reason,
                                facilityName = it.facilityName,
                                providerName = it.providerName,
                                status = it.status,
                                createdAt = it.createdAt,
                                updatedAt = it.updatedAt
                            )
                        )
                    }
                    payload.questions.forEach {
                        qDao.insert(
                            QuestionEntity(
                                id = it.id,
                                consultationId = it.consultationId,
                                text = it.text,
                                textNormalized = it.textNormalized,
                                isPriority = it.isPriority,
                                isDiscussed = it.isDiscussed,
                                createdAt = it.createdAt,
                                updatedAt = it.updatedAt,
                                answer = it.answer,
                                answerUpdatedAt = it.answerUpdatedAt
                            )
                        )
                    }
                    payload.notes.forEach {
                        nDao.upsert(
                            ConsultationNoteEntity(
                                consultationId = it.consultationId,
                                summary = it.summary,
                                providerInstruction = it.providerInstruction,
                                additionalNotes = it.additionalNotes,
                                updatedAt = it.updatedAt
                            )
                        )
                    }
                    payload.followUps.forEach {
                        fDao.insert(
                            FollowUpActionEntity(
                                id = it.id,
                                consultationId = it.consultationId,
                                text = it.text,
                                dueDate = it.dueDate,
                                isCompleted = it.isCompleted,
                                completedAt = it.completedAt,
                                createdAt = it.createdAt,
                                updatedAt = it.updatedAt
                            )
                        )
                    }
                    payload.attachments.forEach {
                        val restoredFile = File(attachmentsRootDir(), "${it.consultationId}/${it.fileName}")
                        aDao.insert(
                            AttachmentEntity(
                                id = it.id,
                                consultationId = it.consultationId,
                                questionId = it.questionId,
                                target = it.target,
                                kind = it.kind,
                                fileName = it.fileName,
                                mimeType = it.mimeType,
                                sizeBytes = it.sizeBytes,
                                durationMs = it.durationMs,
                                caption = it.caption,
                                uriString = if (File(stagingDir, "${it.consultationId}/${it.fileName}").exists()) {
                                    Uri.fromFile(restoredFile).toString()
                                } else {
                                    it.uriString
                                },
                                createdAt = it.createdAt
                            )
                        )
                    }
                }

                // Swap attachment directory after DB transaction succeeds
                val destRoot = attachmentsRootDir()
                destRoot.deleteRecursively()
                if (stagingDir.exists()) {
                    stagingDir.copyRecursively(destRoot, overwrite = true)
                }

                AppLogger.d("Backup", "Restore succeeded consultations=${payload.consultations.size}")
                AppResult.Success(Unit)
            } catch (e: SecurityException) {
                AppResult.Failure(AppError.BackupError("Cadangan tidak bisa dibuka. Periksa kata sandi Anda."))
            } catch (e: Exception) {
                AppLogger.e("Backup", "Restore failed", e)
                AppResult.Failure(AppError.BackupError("Pemulihan gagal. Data Anda saat ini tidak berubah."))
            } finally {
                stagingDir.deleteRecursively()
            }
        }

    private fun readAndValidateZip(zipBytes: ByteArray): Pair<BackupManifest, ByteArray> {
        var manifestBytes: ByteArray? = null
        var dataBytes: ByteArray? = null
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "manifest.json" -> manifestBytes = zis.readBytes()
                    "data.json" -> dataBytes = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        val mBytes = manifestBytes ?: throw IllegalArgumentException("manifest.json tidak ditemukan.")
        val dBytes = dataBytes ?: throw IllegalArgumentException("data.json tidak ditemukan.")
        val manifest = json.decodeFromString<BackupManifest>(mBytes.toString(Charsets.UTF_8))
        val actualHash = BackupCrypto.sha256Hex(dBytes)
        if (!actualHash.equals(manifest.sha256Data, ignoreCase = true)) {
            throw SecurityException("Checksum data cadangan tidak cocok.")
        }
        return manifest to dBytes
    }

    private fun extractAttachmentsToStaging(zipBytes: ByteArray, stagingDir: File) {
        val canonicalRoot = stagingDir.canonicalPath
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.startsWith("attachments/")) {
                    val relPath = entry.name.removePrefix("attachments/")
                    val targetFile = File(stagingDir, relPath)
                    if (targetFile.canonicalPath.startsWith(canonicalRoot)) {
                        targetFile.parentFile?.mkdirs()
                        targetFile.outputStream().use { out -> zis.copyTo(out) }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
