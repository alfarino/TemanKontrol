package id.temankontrol.composeapp.platform

import id.temankontrol.shared.dto.SyncConsultationDto
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

actual fun platformName(): String = "JVM Desktop/Test"

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun localOffsetSeconds(epochMillis: Long): Int =
    ZoneId.systemDefault().rules.getOffset(Instant.ofEpochMilli(epochMillis)).totalSeconds

actual fun generateUuid(): String = UUID.randomUUID().toString()

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

actual fun createPlatformLocalStore(): PlatformLocalStore = object : PlatformLocalStore {
    private var stateJson: String? = null
    private val files = ConcurrentHashMap<String, ByteArray>()

    override fun loadStateJson(): String? = stateJson
    override fun saveStateJson(json: String) {
        stateJson = json
    }

    override fun writeAttachmentBytes(consultationId: String, fileName: String, bytes: ByteArray): String {
        files["$consultationId/$fileName"] = bytes
        return "jvm://$consultationId/$fileName"
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

actual fun createPlatformMediaActions(): PlatformMediaActions = object : PlatformMediaActions {
    override fun pickPhoto(onResult: (PickedMediaFile?) -> Unit) = onResult(null)
    override fun capturePhoto(onResult: (PickedMediaFile?) -> Unit) = onResult(null)
    override fun startVoiceRecording(onError: (String) -> Unit): Boolean = false
    override fun stopVoiceRecording(onResult: (PickedMediaFile?) -> Unit) = onResult(null)
    override fun playVoice(bytes: ByteArray, fileName: String, onCompleted: () -> Unit) = onCompleted()
    override fun stopVoice() = Unit
}

actual fun createPlatformShareBridge(): PlatformShareBridge = object : PlatformShareBridge {
    override fun shareText(title: String, text: String) = Unit
    override fun sharePdfBytes(fileName: String, pdfBytes: ByteArray) = Unit
    override fun exportBackupJson(fileName: String, jsonText: String, onDone: (Boolean, String) -> Unit) =
        onDone(true, fileName)

    override fun importBackupJson(onLoaded: (String?) -> Unit) = onLoaded(null)
}
