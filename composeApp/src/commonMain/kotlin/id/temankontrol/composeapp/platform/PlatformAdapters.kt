package id.temankontrol.composeapp.platform

import id.temankontrol.shared.dto.SyncConsultationDto
import io.ktor.client.engine.HttpClientEngine

enum class BiometricOutcome {
    AUTHENTICATED,
    FAILED,
    UNAVAILABLE
}

data class PlatformAlarmRequest(
    val key: String,
    val title: String,
    val message: String,
    val triggerAtEpochMillis: Long,
    val useAlarmSound: Boolean
)

interface PlatformAlarmScheduler {
    fun scheduleAlarm(request: PlatformAlarmRequest)
    fun cancelAlarm(key: String)
    fun triggerTestAlarm(title: String, message: String)
}

interface PlatformMediaAdapter {
    fun compressPhotoHint(): String
    fun supportedVoiceExtension(): String
}

interface PlatformBiometricAuth {
    fun canAuthenticate(): Boolean
    fun authenticate(promptTitle: String, onResult: (BiometricOutcome) -> Unit)
}

interface PlatformPdfExporter {
    fun exportConsultationSummary(
        consultation: SyncConsultationDto,
        questionLines: List<String>,
        noteSummary: String?
    ): ByteArray
}

expect fun platformName(): String
expect fun createHttpClientEngine(): HttpClientEngine
expect fun createPlatformAlarmScheduler(): PlatformAlarmScheduler
expect fun createPlatformMediaAdapter(): PlatformMediaAdapter
expect fun createPlatformBiometricAuth(): PlatformBiometricAuth
expect fun createPlatformPdfExporter(): PlatformPdfExporter
