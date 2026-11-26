package id.temankontrol.app.data.sync

import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.data.mock.InMemoryDataStore
import id.temankontrol.app.data.mock.InMemoryPreferencesRepository
import id.temankontrol.app.data.mock.MockScenarioManager
import id.temankontrol.app.data.remote.TemanKontrolApiClient
import id.temankontrol.app.domain.model.SyncEntityType
import id.temankontrol.shared.dto.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

private class FakeLocalSyncStorage : LocalSyncStorage {
    val consultations = mutableMapOf<String, SyncConsultationDto>()
    val questions = mutableMapOf<String, SyncQuestionDto>()
    val notes = mutableMapOf<String, SyncNoteDto>()
    val followUps = mutableMapOf<String, SyncFollowUpDto>()
    val attachments = mutableMapOf<String, SyncAttachmentMetaDto>()
    val files = mutableMapOf<String, ByteArray>()

    override suspend fun readSnapshot(): LocalSyncSnapshot = LocalSyncSnapshot(
        consultations = consultations.values.toList(),
        questions = questions.values.toList(),
        notes = notes.values.toList(),
        followUps = followUps.values.toList(),
        attachments = attachments.values.toList()
    )

    override suspend fun applyPulledChanges(pulled: SyncPullResponse): Int {
        var count = 0
        pulled.consultations.forEach { dto ->
            val cur = consultations[dto.id]
            if (dto.deletedAtMillis != null) {
                if (consultations.remove(dto.id) != null) count++
            } else if (cur == null || dto.updatedAtMillis > cur.updatedAtMillis) {
                consultations[dto.id] = dto
                count++
            }
        }
        pulled.questions.forEach { dto ->
            val cur = questions[dto.id]
            if (dto.deletedAtMillis != null) {
                if (questions.remove(dto.id) != null) count++
            } else if (cur == null || dto.updatedAtMillis > cur.updatedAtMillis) {
                questions[dto.id] = dto
                count++
            }
        }
        pulled.attachments.forEach { dto ->
            val cur = attachments[dto.id]
            if (dto.deletedAtMillis != null) {
                if (attachments.remove(dto.id) != null) count++
            } else if (cur == null || dto.updatedAtMillis > cur.updatedAtMillis) {
                attachments[dto.id] = dto
                count++
            }
        }
        return count
    }

    override suspend fun readAttachmentBytes(consultationId: String, fileName: String, uriString: String?): ByteArray? =
        files["$consultationId/$fileName"]

    override suspend fun writeDownloadedAttachment(meta: SyncAttachmentMetaDto, bytes: ByteArray): String {
        attachments[meta.id] = meta
        files["${meta.consultationId}/${meta.fileName}"] = bytes
        return "file:///mock/${meta.consultationId}/${meta.fileName}"
    }

    override suspend fun wipeAllLocalUserData() {
        consultations.clear()
        questions.clear()
        notes.clear()
        followUps.clear()
        attachments.clear()
        files.clear()
    }
}

class OfflineFirstSyncEngineTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun login_mergesGuestDataAndRestoresCloudRecordsAndAttachments() = runTest {
        val serverConsultations = mutableMapOf(
            "c-cloud" to SyncConsultationDto(
                id = "c-cloud",
                title = "Kontrol Mata di Cloud",
                appointmentAtMillis = 1796500000000L,
                reason = "Pemeriksaan retina",
                status = "SCHEDULED",
                createdAtMillis = 1795000000000L,
                updatedAtMillis = 1795000000000L
            )
        )
        val serverAttachments = mutableMapOf(
            "att-cloud" to SyncAttachmentMetaDto(
                id = "att-cloud",
                consultationId = "c-cloud",
                target = "ADDITIONAL_NOTE",
                kind = "PHOTO",
                fileName = "cloud_photo.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 11L,
                createdAtMillis = 1795000000000L,
                updatedAtMillis = 1795000000000L
            )
        )
        val serverFiles = mutableMapOf("att-cloud" to "cloud-photo".encodeToByteArray())
        val deletedTombstonesReceived = mutableListOf<String>()

        val mockEngine = MockEngine { req ->
            val path = req.url.encodedPath
            val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            when {
                path == "/v1/auth/login" -> {
                    respond(
                        content = json.encodeToString(
                            AuthResponse(
                                accessToken = "jwt-access-1",
                                refreshToken = "refresh-1",
                                expiresInSeconds = 900L,
                                user = UserProfileDto("u-1", "pasien@unand.tech", "Pasien", 1795000000000L)
                            )
                        ),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                path == "/v1/sync/push" -> {
                    val pushReq = json.decodeFromString<SyncPushRequest>((req.body as io.ktor.http.content.TextContent).text)
                    pushReq.consultations.forEach { dto ->
                        if (dto.deletedAtMillis != null) {
                            deletedTombstonesReceived.add(dto.id)
                            serverConsultations.remove(dto.id)
                        } else {
                            serverConsultations[dto.id] = dto
                        }
                    }
                    respond(
                        content = json.encodeToString(
                            SyncPushResponse(
                                acceptedCount = pushReq.consultations.size,
                                skippedConflictsCount = 0,
                                serverTimestampMillis = 1796000000000L
                            )
                        ),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                path == "/v1/sync/pull" -> {
                    respond(
                        content = json.encodeToString(
                            SyncPullResponse(
                                serverTimestampMillis = 1796000000000L,
                                consultations = serverConsultations.values.toList(),
                                attachments = serverAttachments.values.toList()
                            )
                        ),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                path.startsWith("/v1/attachments/") && req.method == HttpMethod.Get -> {
                    val attId = path.removePrefix("/v1/attachments/").removeSuffix("/file")
                    val bytes = serverFiles[attId]
                    if (bytes != null) {
                        respond(content = bytes, status = HttpStatusCode.OK)
                    } else {
                        respond(content = "", status = HttpStatusCode.NotFound)
                    }
                }
                path.startsWith("/v1/attachments/") && req.method == HttpMethod.Put -> {
                    respond(content = """{"storedBytes":5}""", status = HttpStatusCode.OK, headers = jsonHeaders)
                }
                path == "/v1/account" && req.method == HttpMethod.Delete -> {
                    serverConsultations.clear()
                    respond(content = "", status = HttpStatusCode.NoContent)
                }
                else -> respond(content = "{}", status = HttpStatusCode.NotFound, headers = jsonHeaders)
            }
        }

        val apiClient = TemanKontrolApiClient(engine = mockEngine, defaultBaseUrl = "https://api.temankontrol.unand.tech")
        val storage = FakeLocalSyncStorage()
        // Pre-existing Guest consultation + tombstone created offline before login (AC-42)
        storage.consultations["c-guest"] = SyncConsultationDto(
            id = "c-guest",
            title = "Kunjungan Tamu Lokal",
            appointmentAtMillis = 1796200000000L,
            reason = "Kontrol lokal sebelum login",
            status = "SCHEDULED",
            createdAtMillis = 1795500000000L,
            updatedAtMillis = 1795500000000L
        )
        val prefs = InMemoryPreferencesRepository(InMemoryDataStore(MockScenarioManager()))
        prefs.recordSyncTombstone(SyncEntityType.CONSULTATION, "c-deleted-offline", 1795500000000L)

        val engine = OfflineFirstSyncEngine(apiClient, storage, prefs)

        // Login should merge c-guest + tombstone to server AND pull c-cloud + att-cloud bytes (AC-42 & AC-43)
        val result = engine.login("pasien@unand.tech", "Rahasia123!")
        assertTrue(result is AppResult.Success)
        val summary = (result as AppResult.Success).data
        assertEquals(2, summary.pushedRecords) // c-guest + c-deleted-offline tombstone
        assertTrue("c-deleted-offline" in deletedTombstonesReceived)
        assertTrue(prefs.pendingTombstones.first().isEmpty())
        assertTrue(storage.consultations.containsKey("c-guest"))
        assertTrue(storage.consultations.containsKey("c-cloud"))
        assertEquals(1, summary.downloadedFiles)
        assertTrue(storage.files.containsKey("c-cloud/cloud_photo.jpg"))

        // Permanent account deletion wipes local data & session (AC-45)
        val deleteRes = engine.deleteAccountPermanently("Rahasia123!")
        assertTrue(deleteRes is AppResult.Success)
        assertNull(prefs.accountSession.first())
        assertTrue(storage.consultations.isEmpty())
        assertTrue(storage.files.isEmpty())
    }
}
