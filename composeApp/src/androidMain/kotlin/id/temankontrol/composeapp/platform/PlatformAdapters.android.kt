package id.temankontrol.composeapp.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import id.temankontrol.shared.dto.SyncConsultationDto

actual fun platformName(): String = "Android"

actual fun createHttpClientEngine(): HttpClientEngine = OkHttp.create()

actual fun createPlatformAlarmScheduler(): PlatformAlarmScheduler = object : PlatformAlarmScheduler {
    override fun scheduleAlarm(request: PlatformAlarmRequest) {
        // Delegated to :app Android AlarmManager.setAlarmClock implementation
    }

    override fun cancelAlarm(key: String) = Unit

    override fun triggerTestAlarm(title: String, message: String) = Unit
}

actual fun createPlatformMediaAdapter(): PlatformMediaAdapter = object : PlatformMediaAdapter {
    override fun compressPhotoHint(): String = "ImageDecoder <= 1600px JPEG q80 (no EXIF)"
    override fun supportedVoiceExtension(): String = ".m4a (AAC Mono)"
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
    ): ByteArray = "${consultation.title}\n${noteSummary.orEmpty()}".encodeToByteArray()
}
