package id.temankontrol.composeapp.platform

import id.temankontrol.shared.dto.SyncConsultationDto
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSMutableData
import platform.Foundation.NSString
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.drawAtPoint
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
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
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear

actual fun platformName(): String = "iPhone (iOS)"

actual fun createHttpClientEngine(): HttpClientEngine = Darwin.create()

actual fun createPlatformAlarmScheduler(): PlatformAlarmScheduler = IosAlarmScheduler()

actual fun createPlatformMediaAdapter(): PlatformMediaAdapter = object : PlatformMediaAdapter {
    override fun compressPhotoHint(): String = "PHPicker + UIImage JPEG q80 <= 1600px"
    override fun supportedVoiceExtension(): String = ".m4a (AVAudioRecorder AAC 16kHz Mono)"
}

actual fun createPlatformBiometricAuth(): PlatformBiometricAuth = IosBiometricAuth()

actual fun createPlatformPdfExporter(): PlatformPdfExporter = IosPdfExporter()

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
            var y = 72.0
            questionLines.forEach { line ->
                (line as NSString).drawAtPoint(
                    CGPointMake(36.0, y),
                    withAttributes = mapOf<Any?, Any?>(
                        platform.UIKit.NSFontAttributeName to UIFont.systemFontOfSize(12.0)
                    )
                )
                y += 20.0
            }
            if (!noteSummary.isNullOrBlank()) {
                (noteSummary as NSString).drawAtPoint(
                    CGPointMake(36.0, y + 12.0),
                    withAttributes = mapOf<Any?, Any?>(
                        platform.UIKit.NSFontAttributeName to UIFont.systemFontOfSize(12.0)
                    )
                )
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
