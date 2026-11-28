package id.temankontrol.composeapp

import id.temankontrol.composeapp.data.KmpActionOutcome
import id.temankontrol.composeapp.data.KmpAppStore
import id.temankontrol.composeapp.data.SupabaseKmpApiClient
import id.temankontrol.composeapp.di.MultiplatformApiGateway
import id.temankontrol.composeapp.domain.KmpDateUtils
import id.temankontrol.composeapp.domain.KmpValidation
import id.temankontrol.composeapp.platform.PickedMediaFile
import id.temankontrol.composeapp.platform.PlatformLocalStore
import id.temankontrol.composeapp.platform.createPlatformBiometricAuth
import id.temankontrol.composeapp.platform.createPlatformMediaAdapter
import id.temankontrol.composeapp.platform.createPlatformPdfExporter
import id.temankontrol.shared.dto.HealthResponse
import id.temankontrol.shared.dto.SyncConsultationDto
import id.temankontrol.shared.dto.SyncPullResponse
import id.temankontrol.shared.dto.SyncPushRequest
import id.temankontrol.shared.dto.SyncPushResponse
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.test.*

private class InMemoryKmpStore : PlatformLocalStore {
    var jsonState: String? = null
    val files = mutableMapOf<String, ByteArray>()

    override fun loadStateJson(): String? = jsonState
    override fun saveStateJson(json: String) {
        jsonState = json
    }

    override fun writeAttachmentBytes(consultationId: String, fileName: String, bytes: ByteArray): String {
        files["$consultationId/$fileName"] = bytes
        return "test://$consultationId/$fileName"
    }

    override fun readAttachmentBytes(consultationId: String, fileName: String): ByteArray? =
        files["$consultationId/$fileName"]

    override fun deleteAttachmentFile(consultationId: String, fileName: String) {
        files.remove("$consultationId/$fileName")
    }

    override fun deleteConsultationFiles(consultationId: String) {
        files.keys.filter { it.startsWith("$consultationId/") }.forEach { files.remove(it) }
    }

    override fun wipeAllAttachments() {
        files.clear()
    }
}

class ComposeAppMultiplatformTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun kmpAdaptersAndGateway_workInCommonTest() = runBlocking {
        val media = createPlatformMediaAdapter()
        assertEquals(".m4a", media.supportedVoiceExtension().take(4))

        val bio = createPlatformBiometricAuth()
        assertTrue(bio.canAuthenticate())

        val pdfBytes = createPlatformPdfExporter().exportConsultationSummary(
            consultation = SyncConsultationDto(
                id = "c-kmp",
                title = "Kontrol Lintas Platform",
                appointmentAtMillis = 1796000000000L,
                reason = "Evaluasi",
                status = "SCHEDULED",
                createdAtMillis = 1795000000000L,
                updatedAtMillis = 1795000000000L
            ),
            questionLines = listOf("Apa langkah selanjutnya?"),
            noteSummary = "Kondisi membaik"
        )
        assertTrue(pdfBytes.isNotEmpty())

        val mockEngine = MockEngine {
            respond(
                content = Json.encodeToString(
                    HealthResponse("UP", "temankontrol-api", "sqlite", "wal", 1796000000000L)
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val gateway = MultiplatformApiGateway(mockEngine)
        val health = gateway.health()
        assertEquals("sqlite", health.database)
        assertEquals("wal", health.journalMode)
    }

    @Test
    fun kmpStoreCrudValidationDateAndPortableBackup_workEndToEnd() {
        val localStore = InMemoryKmpStore()
        val store = KmpAppStore(localStore = localStore, autoStartSyncLoop = false)

        // Validation & closed question tip
        assertTrue(KmpValidation.isClosedQuestion("Apakah obat ini aman?"))
        assertFalse(KmpValidation.isClosedQuestion("Bagaimana cara minum obat ini?"))
        assertTrue(store.saveConsultation(null, "", 1796500000000L, "Keluhan", null, null) is KmpActionOutcome.Err)

        // Date roundtrip
        val ts = KmpDateUtils.fromLocalParts(2026, 12, 10, 9, 30)
        val parts = KmpDateUtils.toLocalParts(ts)
        assertEquals(2026, parts.year)
        assertEquals(12, parts.month)
        assertEquals(10, parts.day)
        assertEquals(9, parts.hour)
        assertEquals(30, parts.minute)

        // Consultation + Question + Note + FollowUp + Attachment + CarryOver
        val c1 = (store.saveConsultation(null, "Kontrol Jantung", ts, "Evaluasi tekanan darah", "RS Unand", "dr. Budi") as KmpActionOutcome.Ok).data
        val c2 = (store.saveConsultation(null, "Kontrol Lanjutan", ts + 86_400_000L, "Cek ulang", null, null) as KmpActionOutcome.Ok).data
        val q1 = (store.saveQuestion(c1, null, "Apa target tekanan darah saya?", true, null) as KmpActionOutcome.Ok).data
        assertTrue(store.saveQuestion(c1, null, "Apa target tekanan darah saya?", false, null) is KmpActionOutcome.Err) // duplicate
        assertTrue(store.saveNote(c1, "Stabil", "Minum rutin", "Bawa hasil tensi") is KmpActionOutcome.Ok)
        assertTrue(store.saveFollowUp(c1, null, "Cek lab bulan depan", null) is KmpActionOutcome.Ok)
        val attId = store.addAttachment(c1, q1, "QUESTION", "VOICE", PickedMediaFile("audio".encodeToByteArray(), "vn.m4a", "audio/mp4", 4200L))
        assertEquals(1, store.state.value.attachments.count { it.id == attId })

        val moved = store.carryOverUnfinishedQuestions(c1, c2)
        assertEquals(1, moved)

        val summary = store.buildSummaryText(c1, true, true, true, true)
        assertTrue(summary.contains("Kontrol Jantung"))
        assertTrue(summary.contains("Cek lab bulan depan"))

        // Portable backup export -> delete -> restore
        val backupJson = store.exportPortableBackupJson()
        store.deleteConsultation(c1)
        assertNull(store.state.value.consultations.find { it.id == c1 })

        val restored = store.restorePortableBackupJson(backupJson)
        assertTrue(restored is KmpActionOutcome.Ok)
        assertNotNull(store.state.value.consultations.find { it.id == c1 })
    }

    @Test
    fun kmpSupabaseSync_autoSyncAndAccountSwitchIsolation_workReliably() = runBlocking {
        val cloudA = mutableMapOf<String, SyncConsultationDto>()
        val cloudB = mutableMapOf(
            "c-b-cloud" to SyncConsultationDto(
                id = "c-b-cloud",
                title = "Data Khusus Akun B",
                appointmentAtMillis = 1797000000000L,
                reason = "Kontrol B",
                status = "SCHEDULED",
                createdAtMillis = 1795000000000L,
                updatedAtMillis = 1795000000000L
            )
        )
        val cloudFiles = mutableMapOf<String, ByteArray>()
        var failSync = false

        val mockEngine = MockEngine { req ->
            val path = req.url.encodedPath
            val grantType = req.url.parameters["grant_type"]
            val auth = req.headers[HttpHeaders.Authorization]
            assertEquals("kmp-test-key", req.headers["apikey"])
            val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

            when {
                path == "/auth/v1/token" && grantType == "password" -> {
                    val bodyText = (req.body as io.ktor.http.content.TextContent).text
                    val isA = bodyText.contains("a@test.id")
                    val uid = if (isA) "uid-a" else "uid-b"
                    val email = if (isA) "a@test.id" else "b@test.id"
                    respond(
                        """{"access_token":"tok-$uid","refresh_token":"ref-$uid","expires_in":3600,"user":{"id":"$uid","email":"$email","user_metadata":{"display_name":"$uid"}}}""",
                        HttpStatusCode.OK,
                        jsonHeaders
                    )
                }
                path == "/rest/v1/rpc/sync_push" -> {
                    if (failSync) return@MockEngine respond("""{"message":"offline"}""", HttpStatusCode.ServiceUnavailable, jsonHeaders)
                    val obj = json.decodeFromString<JsonObject>((req.body as io.ktor.http.content.TextContent).text)
                    val pushReq = json.decodeFromJsonElement<SyncPushRequest>(obj["payload"]!!)
                    val target = if (auth == "Bearer tok-uid-a") cloudA else cloudB
                    pushReq.consultations.forEach { dto ->
                        if (dto.deletedAtMillis != null) target.remove(dto.id) else target[dto.id] = dto
                    }
                    respond(json.encodeToString(SyncPushResponse(pushReq.consultations.size, 0, 1796000000000L)), HttpStatusCode.OK, jsonHeaders)
                }
                path == "/rest/v1/rpc/sync_pull" -> {
                    val src = if (auth == "Bearer tok-uid-a") cloudA else cloudB
                    respond(json.encodeToString(SyncPullResponse(1796000000000L, consultations = src.values.toList())), HttpStatusCode.OK, jsonHeaders)
                }
                path.startsWith("/storage/v1/object/attachments/") && req.method == HttpMethod.Post -> {
                    val key = path.removePrefix("/storage/v1/object/attachments/")
                    cloudFiles[key] = req.body.toByteArray()
                    respond("""{"Key":"$key"}""", HttpStatusCode.OK, jsonHeaders)
                }
                path == "/auth/v1/logout" -> respond("", HttpStatusCode.NoContent)
                else -> respond("{}", HttpStatusCode.NotFound, jsonHeaders)
            }
        }

        val apiClient = SupabaseKmpApiClient(mockEngine, "https://hqtrpbagnosmrznjiiph.supabase.co", "kmp-test-key")
        val localStore = InMemoryKmpStore()
        val store = KmpAppStore(localStore = localStore, apiClient = apiClient, autoStartSyncLoop = false)

        // 1. Login Account A -> add consultation & attachment -> dirty flag becomes true
        assertTrue(store.login("a@test.id", "Rahasia123!") is KmpActionOutcome.Ok)
        val cId = (store.saveConsultation(null, "Kontrol Akun A", 1796500000000L, "Keluhan A", null, null) as KmpActionOutcome.Ok).data
        store.addAttachment(cId, null, "ADDITIONAL_NOTE", "PHOTO", PickedMediaFile("jpg".encodeToByteArray(), "foto.jpg", "image/jpeg"))
        assertTrue(store.state.value.hasUnsyncedCloudChanges)

        // 2. syncIfPendingOrStale uploads data & clears dirty flag
        assertTrue(store.syncIfPendingOrStale() is KmpActionOutcome.Ok)
        assertFalse(store.state.value.hasUnsyncedCloudChanges)
        assertTrue(cloudA.containsKey(cId))
        assertTrue(cloudFiles.keys.any { it.startsWith("uid-a/") })

        // 3. Offline logout aborts and preserves local data
        store.saveConsultation(null, "Belum Tersinkron", 1796600000000L, "Catatan offline", null, null)
        failSync = true
        assertTrue(store.logout(forceWithoutBackup = false) is KmpActionOutcome.Err)
        assertNotNull(store.state.value.accountSession)

        // 4. Online logout backs up first, then wipes local data & files cleanly
        failSync = false
        assertTrue(store.logout(forceWithoutBackup = false) is KmpActionOutcome.Ok)
        assertNull(store.state.value.accountSession)
        assertNull(store.state.value.localDataOwnerUserId)
        assertTrue(store.state.value.consultations.isEmpty())
        assertTrue(localStore.files.isEmpty())
        assertTrue(cloudA.values.any { it.title == "Belum Tersinkron" })

        // 5. Login Account B loads only Account B data without Account A leakage
        assertTrue(store.login("b@test.id", "Rahasia123!") is KmpActionOutcome.Ok)
        assertEquals(setOf("c-b-cloud"), store.state.value.consultations.map { it.id }.toSet())
        assertFalse(cloudB.containsKey(cId))
    }
}
