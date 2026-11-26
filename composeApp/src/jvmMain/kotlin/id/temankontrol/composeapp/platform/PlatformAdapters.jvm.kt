package id.temankontrol.composeapp.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import id.temankontrol.shared.dto.SyncConsultationDto

actual fun platformName(): String = "JVM Desktop/Test"

actual fun createHttpClientEngine(): HttpClientEngine = OkHttp.create()

actual fun createPlatformAlarmScheduler(): PlatformAlarmScheduler = object : PlatformAlarmScheduler {
    override fun scheduleAlarm(request: PlatformAlarmRequest) = Unit
    override fun cancelAlarm(key: String) = Unit
    override fun triggerTestAlarm(title: String, message: String) = Unit
}

actual fun createPlatformMediaAdapter(): PlatformMediaAdapter = object : PlatformMediaAdapter {
    override fun compressPhotoHint(): String = "JVM Test Image Adapter"
    override fun supportedVoiceExtension(): String = ".m4a"
}

actual fun createPlatformBiometricAuth(): PlatformBiometricAuth = object : PlatformBiometricAuth {
    override fun canAuthenticate(): Boolean = true
    override fun authenticate(promptTitle: String, onResult: (BiometricOutcome) -> Unit) {
        onResult(BiometricOutcome.AUTHENTICATED)
    }
}

actual fun createPlatformPdfExporter(): PlatformPdfExporter = object : PlatformPdfExporter {
    override fun exportConsultationSummary(
        consultation: SyncConsultationDto,
        questionLines: List<String>,
        noteSummary: String?
    ): ByteArray = buildString {
        appendLine("Teman Kontrol Summary: ${consultation.title}")
        questionLines.forEach { appendLine("- $it") }
        if (!noteSummary.isNullOrBlank()) appendLine("Notes: $noteSummary")
    }.encodeToByteArray()
}
