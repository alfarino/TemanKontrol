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

interface PlatformLocalStore {
    fun loadStateJson(): String?
    fun saveStateJson(json: String)
    fun writeAttachmentBytes(consultationId: String, fileName: String, bytes: ByteArray): String
    fun readAttachmentBytes(consultationId: String, fileName: String): ByteArray?
    fun deleteAttachmentFile(consultationId: String, fileName: String)
    fun deleteConsultationFiles(consultationId: String)
    fun wipeAllAttachments()
}

data class PickedMediaFile(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    val durationMs: Long? = null
)

interface PlatformMediaActions {
    fun pickPhoto(onResult: (PickedMediaFile?) -> Unit)
    fun capturePhoto(onResult: (PickedMediaFile?) -> Unit)
    fun startVoiceRecording(onError: (String) -> Unit): Boolean
    fun stopVoiceRecording(onResult: (PickedMediaFile?) -> Unit)
    fun playVoice(bytes: ByteArray, fileName: String, onCompleted: () -> Unit)
    fun stopVoice()
}

interface PlatformShareBridge {
    fun shareText(title: String, text: String)
    fun sharePdfBytes(fileName: String, pdfBytes: ByteArray)
    fun exportBackupJson(fileName: String, jsonText: String, onDone: (Boolean, String) -> Unit)
    fun importBackupJson(onLoaded: (String?) -> Unit)
}

expect fun platformName(): String
expect fun currentTimeMillis(): Long
expect fun localOffsetSeconds(epochMillis: Long): Int
expect fun generateUuid(): String
expect fun createHttpClientEngine(): HttpClientEngine
expect fun createPlatformAlarmScheduler(): PlatformAlarmScheduler
expect fun createPlatformMediaAdapter(): PlatformMediaAdapter
expect fun createPlatformBiometricAuth(): PlatformBiometricAuth
expect fun createPlatformPdfExporter(): PlatformPdfExporter
expect fun createPlatformLocalStore(): PlatformLocalStore
expect fun createPlatformMediaActions(): PlatformMediaActions
expect fun createPlatformShareBridge(): PlatformShareBridge
