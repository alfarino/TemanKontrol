package id.temankontrol.composeapp.platform

import id.temankontrol.shared.dto.SyncConsultationDto
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSMutableData
import platform.Foundation.NSString
import platform.Foundation.NSTimeZone
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.appendBytes
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.drawAtPoint
import platform.Foundation.localTimeZone
import platform.Foundation.secondsFromGMTForDate
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToURL
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionForeground
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationInterruptionLevel
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * Bridge yang diisi oleh kode Swift (NativeBridge.swift) saat aplikasi iOS berjalan,
 * menggunakan Base64 String agar interop Swift <-> Kotlin/Native aman tanpa konversi KotlinByteArray.
 */
interface IosNativeMediaAndShareBridge {
    fun pickPhoto(callback: (String?, String?, String?) -> Unit)
    fun capturePhoto(callback: (String?, String?, String?) -> Unit)
    fun startVoiceRecording(onError: (String) -> Unit): Boolean
    fun stopVoiceRecording(callback: (String?, Long, String?, String?) -> Unit)
    fun playVoice(base64Audio: String, fileName: String, onCompleted: () -> Unit)
    fun stopVoice()
    fun shareText(title: String, text: String)
    fun sharePdf(fileName: String, base64Pdf: String)
    fun exportBackupJson(fileName: String, jsonText: String, onDone: (Boolean, String) -> Unit)
    fun importBackupJson(onLoaded: (String?) -> Unit)
}

object IosNativeBridgeHolder {
    var bridge: IosNativeMediaAndShareBridge? = null
}

actual fun platformName(): String = "iPhone (iOS)"

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun localOffsetSeconds(epochMillis: Long): Int {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    return NSTimeZone.localTimeZone.secondsFromGMTForDate(date).toInt()
}

actual fun generateUuid(): String = NSUUID().UUIDString.lowercase()

actual fun createHttpClientEngine(): HttpClientEngine = Darwin.create()

actual fun createPlatformAlarmScheduler(): PlatformAlarmScheduler = IosAlarmScheduler()

actual fun createPlatformMediaAdapter(): PlatformMediaAdapter = object : PlatformMediaAdapter {
    override fun compressPhotoHint(): String = "PHPicker + UIImage JPEG q80 <= 1600px"
    override fun supportedVoiceExtension(): String = ".m4a (AVAudioRecorder AAC 16kHz Mono)"
}

actual fun createPlatformBiometricAuth(): PlatformBiometricAuth = IosBiometricAuth()

actual fun createPlatformPdfExporter(): PlatformPdfExporter = IosPdfExporter()

actual fun createPlatformLocalStore(): PlatformLocalStore = IosFileLocalStore()

@OptIn(ExperimentalEncodingApi::class)
actual fun createPlatformMediaActions(): PlatformMediaActions = object : PlatformMediaActions {
    override fun pickPhoto(onResult: (PickedMediaFile?) -> Unit) {
        val bridge = IosNativeBridgeHolder.bridge ?: return onResult(null)
        bridge.pickPhoto { b64, fileName, mimeType ->
            dispatch_async(dispatch_get_main_queue()) {
                val bytes = b64?.let { runCatching { Base64.decode(it) }.getOrNull() }
                if (bytes != null && fileName != null && mimeType != null) {
                    onResult(PickedMediaFile(bytes, fileName, mimeType))
                } else {
                    onResult(null)
                }
            }
        }
    }

    override fun capturePhoto(onResult: (PickedMediaFile?) -> Unit) {
        val bridge = IosNativeBridgeHolder.bridge ?: return onResult(null)
        bridge.capturePhoto { b64, fileName, mimeType ->
            dispatch_async(dispatch_get_main_queue()) {
                val bytes = b64?.let { runCatching { Base64.decode(it) }.getOrNull() }
                if (bytes != null && fileName != null && mimeType != null) {
                    onResult(PickedMediaFile(bytes, fileName, mimeType))
                } else {
                    onResult(null)
                }
            }
        }
    }

    override fun startVoiceRecording(onError: (String) -> Unit): Boolean {
        val bridge = IosNativeBridgeHolder.bridge ?: run {
            onError("Perekam suara belum terhubung di perangkat iOS.")
            return false
        }
        return bridge.startVoiceRecording(onError)
    }

    override fun stopVoiceRecording(onResult: (PickedMediaFile?) -> Unit) {
        val bridge = IosNativeBridgeHolder.bridge ?: return onResult(null)
        bridge.stopVoiceRecording { b64, durationMs, fileName, mimeType ->
            dispatch_async(dispatch_get_main_queue()) {
                val bytes = b64?.let { runCatching { Base64.decode(it) }.getOrNull() }
                if (bytes != null && fileName != null && mimeType != null) {
                    onResult(PickedMediaFile(bytes, fileName, mimeType, durationMs))
                } else {
                    onResult(null)
                }
            }
        }
    }

    override fun playVoice(bytes: ByteArray, fileName: String, onCompleted: () -> Unit) {
        val bridge = IosNativeBridgeHolder.bridge ?: return onCompleted()
        bridge.playVoice(Base64.encode(bytes), fileName) {
            dispatch_async(dispatch_get_main_queue()) {
                onCompleted()
            }
        }
    }

    override fun stopVoice() {
        IosNativeBridgeHolder.bridge?.stopVoice()
    }
}

@OptIn(ExperimentalEncodingApi::class)
actual fun createPlatformShareBridge(): PlatformShareBridge = object : PlatformShareBridge {
    override fun shareText(title: String, text: String) {
        IosNativeBridgeHolder.bridge?.shareText(title, text)
    }

    override fun sharePdfBytes(fileName: String, pdfBytes: ByteArray) {
        IosNativeBridgeHolder.bridge?.sharePdf(fileName, Base64.encode(pdfBytes))
    }

    override fun exportBackupJson(fileName: String, jsonText: String, onDone: (Boolean, String) -> Unit) {
        val bridge = IosNativeBridgeHolder.bridge ?: return onDone(false, "Bridge tidak tersedia")
        bridge.exportBackupJson(fileName, jsonText) { ok, msg ->
            dispatch_async(dispatch_get_main_queue()) {
                onDone(ok, msg)
            }
        }
    }

    override fun importBackupJson(onLoaded: (String?) -> Unit) {
        val bridge = IosNativeBridgeHolder.bridge ?: return onLoaded(null)
        bridge.importBackupJson { content ->
            dispatch_async(dispatch_get_main_queue()) {
                onLoaded(content)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
class IosFileLocalStore : PlatformLocalStore {
    private fun documentsUrl(): NSURL? =
        NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).firstOrNull() as? NSURL

    private fun storeFileUrl(): NSURL? = documentsUrl()?.URLByAppendingPathComponent("temankontrol_store.json")

    private fun attachmentsRootUrl(): NSURL? = documentsUrl()?.URLByAppendingPathComponent("attachments")

    private fun consultationDirUrl(consultationId: String): NSURL? =
        attachmentsRootUrl()?.URLByAppendingPathComponent(consultationId)

    override fun loadStateJson(): String? {
        val url = storeFileUrl() ?: return null
        return NSString.stringWithContentsOfURL(url, encoding = NSUTF8StringEncoding, error = null)
    }

    override fun saveStateJson(json: String) {
        val url = storeFileUrl() ?: return
        (json as NSString).writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    override fun writeAttachmentBytes(consultationId: String, fileName: String, bytes: ByteArray): String {
        val dirUrl = consultationDirUrl(consultationId) ?: return ""
        NSFileManager.defaultManager.createDirectoryAtURL(
            url = dirUrl,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        val fileUrl = dirUrl.URLByAppendingPathComponent(fileName) ?: return ""
        val data = NSMutableData()
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                data.appendBytes(pinned.addressOf(0), length = bytes.size.toULong())
            }
        }
        data.writeToURL(fileUrl, atomically = true)
        return fileUrl.absoluteString ?: ""
    }

    override fun readAttachmentBytes(consultationId: String, fileName: String): ByteArray? {
        val fileUrl = consultationDirUrl(consultationId)?.URLByAppendingPathComponent(fileName) ?: return null
        val nsData = NSData.dataWithContentsOfURL(fileUrl) ?: return null
        val size = nsData.length.toInt()
        if (size <= 0) return ByteArray(0)
        val out = ByteArray(size)
        out.usePinned { pinned ->
            memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
        }
        return out
    }

    override fun deleteAttachmentFile(consultationId: String, fileName: String) {
        val fileUrl = consultationDirUrl(consultationId)?.URLByAppendingPathComponent(fileName) ?: return
        NSFileManager.defaultManager.removeItemAtURL(fileUrl, error = null)
    }

    override fun deleteConsultationFiles(consultationId: String) {
        val dirUrl = consultationDirUrl(consultationId) ?: return
        NSFileManager.defaultManager.removeItemAtURL(dirUrl, error = null)
    }

    override fun wipeAllAttachments() {
        val root = attachmentsRootUrl() ?: return
        NSFileManager.defaultManager.removeItemAtURL(root, error = null)
    }
}

class IosAlarmScheduler : PlatformAlarmScheduler {

    companion object {
        const val CATEGORY_ALARM = "TEMANKONTROL_ALARM"
        const val ACTION_SNOOZE_10M = "SNOOZE_10_MIN"
        const val ACTION_MARK_READY = "MARK_READY"
        const val ALARM_SOUND_CAF = "alarm_teman_kontrol.caf"
    }

    init {
        registerNotificationCategories()
    }

    private fun registerNotificationCategories() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val snooze = UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_SNOOZE_10M,
            title = "Tunda 10 Menit",
            options = 0uL
        )
        val ready = UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_MARK_READY,
            title = "Matikan / Saya Siap",
            options = UNNotificationActionOptionForeground
        )
        val category = UNNotificationCategory.categoryWithIdentifier(
            identifier = CATEGORY_ALARM,
            actions = listOf(ready, snooze),
            intentIdentifiers = emptyList<Any>(),
            options = 0uL
        )
        center.setNotificationCategories(setOf(category))
    }

    override fun scheduleAlarm(request: PlatformAlarmRequest) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        ) { granted, _ ->
            if (!granted) return@requestAuthorizationWithOptions

            val content = UNMutableNotificationContent().apply {
                setTitle(request.title)
                setBody(request.message)
                setCategoryIdentifier(CATEGORY_ALARM)
                setInterruptionLevel(UNNotificationInterruptionLevel.UNNotificationInterruptionLevelTimeSensitive)
                setSound(
                    if (request.useAlarmSound) {
                        UNNotificationSound.soundNamed(ALARM_SOUND_CAF)
                    } else {
                        UNNotificationSound.defaultSound()
                    }
                )
            }

            val date = NSDate.dateWithTimeIntervalSince1970(request.triggerAtEpochMillis / 1000.0)
            val units = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond
            val components = NSCalendar.currentCalendar.components(units, fromDate = date)
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents = components,
                repeats = false
            )
            val req = UNNotificationRequest.requestWithIdentifier(
                identifier = request.key,
                content = content,
                trigger = trigger
            )
            center.addNotificationRequest(req, withCompletionHandler = null)
        }
    }

    override fun cancelAlarm(key: String) {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(key))
    }

    override fun triggerTestAlarm(title: String, message: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        ) { granted, _ ->
            if (!granted) return@requestAuthorizationWithOptions
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(message)
                setCategoryIdentifier(CATEGORY_ALARM)
                setInterruptionLevel(UNNotificationInterruptionLevel.UNNotificationInterruptionLevelTimeSensitive)
                setSound(UNNotificationSound.soundNamed(ALARM_SOUND_CAF))
            }
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, repeats = false)
            val req = UNNotificationRequest.requestWithIdentifier("test-alarm", content, trigger)
            center.addNotificationRequest(req, withCompletionHandler = null)
        }
    }
}

class IosBiometricAuth : PlatformBiometricAuth {
    @OptIn(ExperimentalForeignApi::class)
    override fun canAuthenticate(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun authenticate(promptTitle: String, onResult: (BiometricOutcome) -> Unit) {
        val context = LAContext()
        if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null)) {
            onResult(BiometricOutcome.UNAVAILABLE)
            return
        }
        context.evaluatePolicy(LAPolicyDeviceOwnerAuthentication, localizedReason = promptTitle) { success, _ ->
            dispatch_async(dispatch_get_main_queue()) {
                onResult(if (success) BiometricOutcome.AUTHENTICATED else BiometricOutcome.FAILED)
            }
        }
    }
}

class IosPdfExporter : PlatformPdfExporter {
    @OptIn(ExperimentalForeignApi::class)
    override fun exportConsultationSummary(
        consultation: SyncConsultationDto,
        questionLines: List<String>,
        noteSummary: String?
    ): ByteArray {
        val bounds = CGRectMake(0.0, 0.0, 595.2, 841.8) // A4
        val renderer = UIGraphicsPDFRenderer(bounds = bounds, format = UIGraphicsPDFRendererFormat())
        val pdfData: NSData = renderer.PDFDataWithActions { context ->
            context?.beginPage()
            val title = consultation.title as NSString
            title.drawAtPoint(
                CGPointMake(36.0, 36.0),
                withAttributes = mapOf<Any?, Any?>(
                    platform.UIKit.NSFontAttributeName to UIFont.boldSystemFontOfSize(18.0)
                )
            )
            var y = 68.0
            val subtitle = "Keluhan: ${consultation.reason}"
            (subtitle as NSString).drawAtPoint(
                CGPointMake(36.0, y),
                withAttributes = mapOf<Any?, Any?>(
                    platform.UIKit.NSFontAttributeName to UIFont.systemFontOfSize(12.0)
                )
            )
            y += 24.0
            questionLines.forEach { line ->
                line.chunked(78).forEach { part ->
                    if (y > 790.0) {
                        context?.beginPage()
                        y = 36.0
                    }
                    (part as NSString).drawAtPoint(
                        CGPointMake(36.0, y),
                        withAttributes = mapOf<Any?, Any?>(
                            platform.UIKit.NSFontAttributeName to UIFont.systemFontOfSize(11.5)
                        )
                    )
                    y += 17.0
                }
                y += 4.0
            }
            if (!noteSummary.isNullOrBlank()) {
                y += 8.0
                noteSummary.lines().forEach { line ->
                    line.chunked(78).forEach { part ->
                        if (y > 790.0) {
                            context?.beginPage()
                            y = 36.0
                        }
                        (part as NSString).drawAtPoint(
                            CGPointMake(36.0, y),
                            withAttributes = mapOf<Any?, Any?>(
                                platform.UIKit.NSFontAttributeName to UIFont.systemFontOfSize(11.5)
                            )
                        )
                        y += 17.0
                    }
                }
            }
        }
        val bytes = ByteArray(pdfData.length.toInt())
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), pdfData.bytes, pdfData.length)
            }
        }
        return bytes
    }
}
