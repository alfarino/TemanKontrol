package id.temankontrol.composeapp

import id.temankontrol.composeapp.di.MultiplatformApiGateway
import id.temankontrol.composeapp.platform.createPlatformBiometricAuth
import id.temankontrol.composeapp.platform.createPlatformMediaAdapter
import id.temankontrol.composeapp.platform.createPlatformPdfExporter
import id.temankontrol.shared.dto.HealthResponse
import id.temankontrol.shared.dto.SyncConsultationDto
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeAppMultiplatformTest {

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
}
