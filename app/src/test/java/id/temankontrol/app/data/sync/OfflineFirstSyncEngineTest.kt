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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
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
    fun login_mergesGuestDataAndRestoresCloudRecordsAndAttachmentsFromSupabase() = runTest {
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
        val serverFiles = mutableMapOf("u-1/att-cloud" to "cloud-photo".encodeToByteArray())
        val deletedTombstonesReceived = mutableListOf<String>()

        val mockEngine = MockEngine { req ->
            val path = req.url.encodedPath
            val grantType = req.url.parameters["grant_type"]
            val apiKey = req.headers["apikey"]
            assertEquals("test-publishable-key", apiKey)

            val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            when {
                path == "/auth/v1/token" && grantType == "password" -> {
                    respond(
                        content = """
                            {
                              "access_token": "jwt-access-1",
                              "refresh_token": "refresh-1",
                              "expires_in": 3600,
                              "user": {
                                "id": "u-1",
                                "email": "pasien@example.com",
                                "created_at": "2026-11-28T06:00:00Z",
                                "user_metadata": { "display_name": "Pasien" }
                              }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                path == "/rest/v1/rpc/sync_push" -> {
                    val rpcObj = json.decodeFromString<JsonObject>((req.body as io.ktor.http.content.TextContent).text)
                    val pushReq = json.decodeFromJsonElement<SyncPushRequest>(rpcObj["payload"]!!)
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
                path == "/rest/v1/rpc/sync_pull" -> {
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
                path.startsWith("/storage/v1/object/attachments/") && req.method == HttpMethod.Get -> {
                    val key = path.removePrefix("/storage/v1/object/attachments/")
                    val bytes = serverFiles[key]
                    if (bytes != null) {
                        respond(content = bytes, status = HttpStatusCode.OK)
                    } else {
                        respond(content = """{"error":"not_found"}""", status = HttpStatusCode.NotFound, headers = jsonHeaders)
                    }
                }
                path.startsWith("/storage/v1/object/attachments/") && req.method == HttpMethod.Post -> {
                    respond(content = """{"Key":"attachments/ok"}""", status = HttpStatusCode.OK, headers = jsonHeaders)
                }
                path == "/functions/v1/delete-account" && req.method == HttpMethod.Post -> {
                    serverConsultations.clear()
                    respond(content = """{"deleted":true}""", status = HttpStatusCode.OK, headers = jsonHeaders)
                }
                else -> respond(content = "{}", status = HttpStatusCode.NotFound, headers = jsonHeaders)
            }
        }

        val apiClient = TemanKontrolApiClient(
            engine = mockEngine,
            defaultBaseUrl = "https://hqtrpbagnosmrznjiiph.supabase.co",
            defaultApiKey = "test-publishable-key"
        )
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
        val result = engine.login("pasien@example.com", "Rahasia123!")
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

    @Test
    fun resetPasswordAndLegacyUrlFallbackWorkWithSupabaseApiClient() = runTest {
        var verifiedOtp = false
        var updatedPassword = false
        var loggedOutRecovery = false
        var requestedRecoverHost = ""

        val mockEngine = MockEngine { req ->
            requestedRecoverHost = req.url.host
            val path = req.url.encodedPath
            val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            when {
                path == "/auth/v1/recover" -> respond("""{}""", HttpStatusCode.OK, jsonHeaders)
                path == "/auth/v1/verify" -> {
                    verifiedOtp = true
                    respond(
                        content = """{"access_token":"recovery-jwt","refresh_token":"r2","expires_in":3600}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                path == "/auth/v1/user" && req.method == HttpMethod.Put -> {
                    updatedPassword = req.headers[HttpHeaders.Authorization] == "Bearer recovery-jwt"
                    respond("""{}""", HttpStatusCode.OK, jsonHeaders)
                }
                path == "/auth/v1/logout" -> {
                    loggedOutRecovery = true
                    respond("", HttpStatusCode.NoContent)
                }
                else -> respond("{}", HttpStatusCode.NotFound, jsonHeaders)
            }
        }

        val apiClient = TemanKontrolApiClient(
            engine = mockEngine,
            defaultBaseUrl = "https://hqtrpbagnosmrznjiiph.supabase.co",
            defaultApiKey = "test-key"
        )

        val otpRes = apiClient.forgotPassword("user@example.com", baseUrl = "https://temankontrol.unand.tech")
        assertTrue(otpRes.sent)
        assertEquals("hqtrpbagnosmrznjiiph.supabase.co", requestedRecoverHost)

        apiClient.resetPassword(
            ResetPasswordRequest("user@example.com", "123456", "BaruRahasia123"),
            baseUrl = "https://temankontrol.unand.tech"
        )
        assertTrue(verifiedOtp)
        assertTrue(updatedPassword)
        assertTrue(loggedOutRecovery)
    }

    @Test
    fun switchingAccounts_backsUpBeforeLogoutAndIsolatesLocalDataBetweenUsers() = runTest {
        val cloudUserA = mutableMapOf<String, SyncConsultationDto>()
        val cloudUserB = mutableMapOf(
            "c-user-b" to SyncConsultationDto(
                id = "c-user-b",
                title = "Kontrol Akun B",
                appointmentAtMillis = 1796800000000L,
                reason = "Kontrol khusus Akun B",
                status = "SCHEDULED",
                createdAtMillis = 1795000000000L,
                updatedAtMillis = 1795000000000L
            )
        )
        var failSyncTemporarily = false

        val mockEngine = MockEngine { req ->
            val path = req.url.encodedPath
            val grantType = req.url.parameters["grant_type"]
            val auth = req.headers[HttpHeaders.Authorization]
            val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

            when {
                path == "/auth/v1/token" && grantType == "password" -> {
                    val bodyText = (req.body as io.ktor.http.content.TextContent).text
                    val isUserA = bodyText.contains("a@example.com")
                    val uid = if (isUserA) "u-a" else "u-b"
                    val email = if (isUserA) "a@example.com" else "b@example.com"
                    respond(
                        content = """
                            {
                              "access_token": "jwt-$uid",
                              "refresh_token": "ref-$uid",
                              "expires_in": 3600,
                              "user": { "id": "$uid", "email": "$email", "user_metadata": { "display_name": "$uid" } }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                path == "/rest/v1/rpc/sync_push" -> {
                    if (failSyncTemporarily) {
                        return@MockEngine respond("""{"message":"offline"}""", HttpStatusCode.ServiceUnavailable, jsonHeaders)
                    }
                    val rpcObj = json.decodeFromString<JsonObject>((req.body as io.ktor.http.content.TextContent).text)
                    val pushReq = json.decodeFromJsonElement<SyncPushRequest>(rpcObj["payload"]!!)
                    val targetMap = if (auth == "Bearer jwt-u-a") cloudUserA else cloudUserB
                    pushReq.consultations.forEach { targetMap[it.id] = it }
                    respond(
                        content = json.encodeToString(SyncPushResponse(pushReq.consultations.size, 0, 1796000000000L)),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                path == "/rest/v1/rpc/sync_pull" -> {
                    val sourceMap = if (auth == "Bearer jwt-u-a") cloudUserA else cloudUserB
                    respond(
                        content = json.encodeToString(
                            SyncPullResponse(
                                serverTimestampMillis = 1796000000000L,
                                consultations = sourceMap.values.toList()
                            )
                        ),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                path == "/auth/v1/logout" -> respond("", HttpStatusCode.NoContent)
                else -> respond("{}", HttpStatusCode.NotFound, jsonHeaders)
            }
        }

        val apiClient = TemanKontrolApiClient(mockEngine, "https://hqtrpbagnosmrznjiiph.supabase.co", "test-key")
        val storage = FakeLocalSyncStorage()
        val prefs = InMemoryPreferencesRepository(InMemoryDataStore(MockScenarioManager()))
        val engine = OfflineFirstSyncEngine(apiClient, storage, prefs)

        // 1. Akun A login dan membuat data lokal baru
        assertTrue(engine.login("a@example.com", "Rahasia123!") is AppResult.Success)
        storage.consultations["c-user-a-local"] = SyncConsultationDto(
            id = "c-user-a-local",
            title = "Kontrol Rahasia Akun A",
            appointmentAtMillis = 1796700000000L,
            reason = "Keluhan A",
            status = "SCHEDULED",
            createdAtMillis = 1795600000000L,
            updatedAtMillis = 1795600000000L
        )

        // 2. Jika offline saat mau logout/ganti akun, logout ditolak agar data lokal Akun A tidak hilang
        failSyncTemporarily = true
        val offlineLogoutRes = engine.logout()
        assertTrue(offlineLogoutRes is AppResult.Failure)
        assertNotNull(prefs.accountSession.first())
        assertTrue(storage.consultations.containsKey("c-user-a-local"))

        // 3. Saat online, logout mencadangkan data lokal ke cloud Akun A lalu membersihkan data lokal perangkat
        failSyncTemporarily = false
        val cleanLogoutRes = engine.logout()
        assertTrue(cleanLogoutRes is AppResult.Success)
        assertNull(prefs.accountSession.first())
        assertNull(prefs.localDataOwnerUserId.first())
        assertTrue(cloudUserA.containsKey("c-user-a-local"))
        assertTrue(storage.consultations.isEmpty())

        // 4. Login ke Akun B hanya memuat data Akun B dan tidak membawa data Akun A
        assertTrue(engine.login("b@example.com", "Rahasia123!") is AppResult.Success)
        assertEquals(setOf("c-user-b"), storage.consultations.keys)
        assertFalse(cloudUserB.containsKey("c-user-a-local"))
        assertEquals("u-b", prefs.localDataOwnerUserId.first())
    }

    @Test
    fun syncIfPendingOrStale_syncsDirtyChangesAndPreservesNewerInFlightEdits() = runTest {
        var syncPushCalls = 0
        var prefsRef: InMemoryPreferencesRepository? = null
        var injectedInFlightEdit = false

        val mockEngine = MockEngine { req ->
            val path = req.url.encodedPath
            val grantType = req.url.parameters["grant_type"]
            val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            when {
                path == "/auth/v1/token" && grantType == "password" -> respond(
                    content = """{"access_token":"jwt-auto","refresh_token":"ref-auto","expires_in":3600,"user":{"id":"u-auto","email":"auto@example.com"}}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
                path == "/rest/v1/rpc/sync_push" -> {
                    syncPushCalls++
                    if (syncPushCalls == 2 && !injectedInFlightEdit) {
                        injectedInFlightEdit = true
                        // Simulasikan user mengedit lagi tepat saat HTTP request sync sedang berjalan
                        Thread.sleep(25)
                        prefsRef?.markDataChanged()
                    }
                    respond(
                        content = json.encodeToString(SyncPushResponse(1, 0, System.currentTimeMillis())),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                path == "/rest/v1/rpc/sync_pull" -> respond(
                    content = json.encodeToString(SyncPullResponse(serverTimestampMillis = System.currentTimeMillis())),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
                else -> respond("{}", HttpStatusCode.NotFound, jsonHeaders)
            }
        }

        val apiClient = TemanKontrolApiClient(mockEngine, "https://hqtrpbagnosmrznjiiph.supabase.co", "test-key")
        val storage = FakeLocalSyncStorage()
        val prefs = InMemoryPreferencesRepository(InMemoryDataStore(MockScenarioManager()))
        prefsRef = prefs
        val engine = OfflineFirstSyncEngine(apiClient, storage, prefs)

        // Login awal (syncPushCalls = 1)
        assertTrue(engine.login("auto@example.com", "Rahasia123!") is AppResult.Success)
        assertFalse(prefs.hasUnsyncedCloudChanges.first())

        // Jika tidak ada perubahan & belum stale, syncIfPendingOrStale dilewati
        assertNull(engine.syncIfPendingOrStale(maxStaleMillis = 5 * 60_000L))
        assertEquals(1, syncPushCalls)

        // Setelah markDataChanged(), syncIfPendingOrStale langsung mengirim ke Supabase (syncPushCalls = 2)
        // Karena terjadi edit baru saat syncPushCalls == 2 in-flight, hasUnsyncedCloudChanges tetap true!
        prefs.markDataChanged()
        assertTrue(prefs.hasUnsyncedCloudChanges.first())
        val autoRes = engine.syncIfPendingOrStale(maxStaleMillis = 5 * 60_000L)
        assertTrue(autoRes is AppResult.Success)
        assertEquals(2, syncPushCalls)
        assertTrue(prefs.hasUnsyncedCloudChanges.first())

        // Sync berikutnya (tanpa edit baru di tengah jalan) membersihkan flag tertunda
        assertTrue(engine.syncIfPendingOrStale(maxStaleMillis = 5 * 60_000L) is AppResult.Success)
        assertEquals(3, syncPushCalls)
        assertFalse(prefs.hasUnsyncedCloudChanges.first())
    }
}
