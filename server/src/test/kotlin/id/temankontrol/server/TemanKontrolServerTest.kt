package id.temankontrol.server

import id.temankontrol.shared.dto.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TemanKontrolServerTest {

    private fun testConfig(tempDir: java.nio.file.Path): ServerConfig = ServerConfig(
        dbPath = tempDir.resolve("test.db").toString(),
        mediaDir = tempDir.resolve("media").toFile(),
        jwtSecret = "unit-test-secret-key",
        pbkdf2Iterations = 1_000
    )

    @Test
    fun healthAndPrivacyEndpoints_returnSqliteWalAndPolicyHtml() = testApplication {
        val tempDir = Files.createTempDirectory("tk-server-health")
        application { temanKontrolModule(testConfig(tempDir)) }

        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val healthRes = client.get("/health")
        assertEquals(HttpStatusCode.OK, healthRes.status)
        val health = healthRes.body<HealthResponse>()
        assertEquals("UP", health.status)
        assertEquals("sqlite", health.database)
        assertEquals("wal", health.journalMode.lowercase())

        val privacyRes = client.get("/privacy")
        assertEquals(HttpStatusCode.OK, privacyRes.status)
        assertTrue(privacyRes.bodyAsText().contains("Kebijakan Privasi Teman Kontrol"))
    }

    @Test
    fun authSyncAttachmentsAndAccountDeletion_endToEndFlow() = testApplication {
        val tempDir = Files.createTempDirectory("tk-server-e2e")
        var lastOtp: String? = null
        application {
            temanKontrolModule(testConfig(tempDir)) { _, otp ->
                lastOtp = otp
            }
        }

        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        // 1. Register
        val regRes = client.post("/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("budi@unand.tech", "Rahasia123!", "Budi Santoso", true))
        }
        assertEquals(HttpStatusCode.Created, regRes.status)
        val auth = regRes.body<AuthResponse>()
        assertEquals("budi@unand.tech", auth.user.email)

        // 2. Refresh token rotates token
        val refreshRes = client.post("/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(auth.refreshToken))
        }
        assertEquals(HttpStatusCode.OK, refreshRes.status)
        val refreshed = refreshRes.body<AuthResponse>()
        assertNotEquals(auth.refreshToken, refreshed.refreshToken)

        // 3. Push Sync + LWW Conflict Resolution
        val consultation = SyncConsultationDto(
            id = "c-100",
            title = "Kontrol Hipertensi",
            appointmentAtMillis = 1796000000000L,
            reason = "Evaluasi obat",
            facilityName = "RS Unand",
            providerName = "dr. Budi",
            status = "SCHEDULED",
            createdAtMillis = 1795900000000L,
            updatedAtMillis = 1795900002000L
        )
        val pushRes = client.post("/v1/sync/push") {
            header(HttpHeaders.Authorization, "Bearer ${refreshed.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(
                SyncPushRequest(
                    consultations = listOf(consultation),
                    questions = listOf(
                        SyncQuestionDto(
                            id = "q-100",
                            consultationId = "c-100",
                            text = "Bagaimana cara minum obat yang aman?",
                            isPriority = true,
                            isDiscussed = true,
                            createdAtMillis = 1795900000000L,
                            updatedAtMillis = 1795900002000L,
                            answer = "Minum sesudah sarapan",
                            answerUpdatedAtMillis = 1795900002000L
                        )
                    )
                )
            )
        }
        assertEquals(HttpStatusCode.OK, pushRes.status)
        val pushBody = pushRes.body<SyncPushResponse>()
        assertEquals(2, pushBody.acceptedCount)

        // Stale update should be skipped (LWW protection)
        val stalePush = client.post("/v1/sync/push") {
            header(HttpHeaders.Authorization, "Bearer ${refreshed.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(SyncPushRequest(consultations = listOf(consultation.copy(title = "Stale", updatedAtMillis = 1795900001000L))))
        }
        assertEquals(1, stalePush.body<SyncPushResponse>().skippedConflictsCount)

        // 4. Pull Sync (Full restore with since=0)
        val pullRes = client.get("/v1/sync/pull?since=0") {
            header(HttpHeaders.Authorization, "Bearer ${refreshed.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, pullRes.status)
        val pulled = pullRes.body<SyncPullResponse>()
        assertEquals(1, pulled.consultations.size)
        assertEquals("Kontrol Hipertensi", pulled.consultations.first().title)
        assertEquals(1, pulled.questions.size)

        // 5. Upload & Download Binary Attachment
        val sampleBytes = "fake-jpeg-binary-payload".encodeToByteArray()
        val putFile = client.put("/v1/attachments/att-1/file") {
            header(HttpHeaders.Authorization, "Bearer ${refreshed.accessToken}")
            setBody(sampleBytes)
        }
        assertEquals(HttpStatusCode.OK, putFile.status)

        val getFile = client.get("/v1/attachments/att-1/file") {
            header(HttpHeaders.Authorization, "Bearer ${refreshed.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, getFile.status)
        assertTrue(sampleBytes.contentEquals(getFile.readRawBytes()))

        // 6. Forgot & Reset Password via OTP
        val forgotRes = client.post("/v1/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest("budi@unand.tech"))
        }
        assertEquals(HttpStatusCode.OK, forgotRes.status)
        val resetRes = client.post("/v1/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest("budi@unand.tech", lastOtp!!, "BaruAman456!"))
        }
        assertEquals(HttpStatusCode.OK, resetRes.status)

        val loginNew = client.post("/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("budi@unand.tech", "BaruAman456!"))
        }
        assertEquals(HttpStatusCode.OK, loginNew.status)
        val newAuth = loginNew.body<AuthResponse>()

        // 7. Delete Account & All Data Permanently (AC-45)
        val deleteRes = client.delete("/v1/account") {
            header(HttpHeaders.Authorization, "Bearer ${newAuth.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(DeleteAccountRequest("BaruAman456!"))
        }
        assertEquals(HttpStatusCode.NoContent, deleteRes.status)

        val getFileAfterDelete = client.get("/v1/attachments/att-1/file") {
            header(HttpHeaders.Authorization, "Bearer ${newAuth.accessToken}")
        }
        assertEquals(HttpStatusCode.NotFound, getFileAfterDelete.status)
    }
}
