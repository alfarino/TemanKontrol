package id.temankontrol.composeapp.domain

import id.temankontrol.composeapp.platform.PlatformAlarmRequest
import id.temankontrol.composeapp.platform.PlatformAlarmScheduler
import id.temankontrol.composeapp.platform.currentTimeMillis
import id.temankontrol.composeapp.platform.localOffsetSeconds
import id.temankontrol.shared.dto.SyncAttachmentMetaDto
import id.temankontrol.shared.dto.SyncConsultationDto
import id.temankontrol.shared.dto.SyncFollowUpDto
import id.temankontrol.shared.dto.SyncNoteDto
import id.temankontrol.shared.dto.SyncQuestionDto
import id.temankontrol.shared.dto.SyncReminderSettingsDto
import kotlinx.serialization.Serializable

@Serializable
enum class KmpSyncEntityType {
    CONSULTATION,
    QUESTION,
    NOTE,
    FOLLOW_UP,
    ATTACHMENT
}

@Serializable
data class KmpSyncTombstone(
    val entityType: KmpSyncEntityType,
    val entityId: String,
    val deletedAtMillis: Long
)

@Serializable
data class KmpAccountSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String,
    val lastSyncAtMillis: Long? = null,
    val serverBaseUrl: String = "https://hqtrpbagnosmrznjiiph.supabase.co"
)

@Serializable
data class KmpLocalDatabaseState(
    val consultations: List<SyncConsultationDto> = emptyList(),
    val questions: List<SyncQuestionDto> = emptyList(),
    val notes: List<SyncNoteDto> = emptyList(),
    val followUps: List<SyncFollowUpDto> = emptyList(),
    val attachments: List<SyncAttachmentMetaDto> = emptyList(),
    val reminderSettings: SyncReminderSettingsDto = defaultReminderSettingsDto(),
    val tombstones: List<KmpSyncTombstone> = emptyList(),
    val noticeAccepted: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockTimeoutMinutes: Int = 1,
    val accountSession: KmpAccountSession? = null,
    val localDataOwnerUserId: String? = null,
    val hasUnsyncedCloudChanges: Boolean = false,
    val lastDataChangeMillis: Long = 0L,
    val lastBackupAtMillis: Long? = null
)

fun defaultReminderSettingsDto(updatedAtMillis: Long = 0L) = SyncReminderSettingsDto(
    visitReminderEnabled = true,
    visitReminderHour = 19,
    visitThirtyMinBeforeEnabled = true,
    visitAtTimeEnabled = true,
    customLeadMinutes = null,
    alarmSoundModeEnabled = true,
    prepareReminderEnabled = true,
    followUpReminderEnabled = true,
    followUpReminderHour = 8,
    backupReminderEnabled = true,
    showTitleOnLockScreen = false,
    updatedAtMillis = updatedAtMillis
)

enum class KmpSuggestionCategory(val label: String) {
    SYMPTOM("Gejala & Keluhan"),
    EXAM("Pemeriksaan & Hasil"),
    TREATMENT("Obat & Perawatan"),
    NEXT_STEP("Langkah Berikutnya"),
    UNDERSTANDING("Pemahaman (Ask Me 3)")
}

data class KmpQuestionSuggestion(
    val id: String,
    val category: KmpSuggestionCategory,
    val text: String
)

object KmpSuggestionCatalog {
    val all: List<KmpQuestionSuggestion> = listOf(
        KmpQuestionSuggestion("sug-g-1", KmpSuggestionCategory.SYMPTOM, "Apa kemungkinan penyebab keluhan yang saya alami? Tolong jelaskan."),
        KmpQuestionSuggestion("sug-g-2", KmpSuggestionCategory.SYMPTOM, "Keluhan seperti apa yang perlu saya perhatikan, dan apa yang harus saya lakukan jika itu terjadi?"),
        KmpQuestionSuggestion("sug-g-3", KmpSuggestionCategory.SYMPTOM, "Bagaimana perkembangan keluhan ini biasanya, dan berapa lama saya perlu menunggu perbaikan?"),
        KmpQuestionSuggestion("sug-g-4", KmpSuggestionCategory.SYMPTOM, "Aktivitas atau kebiasaan apa saja yang sebaiknya saya sesuaikan selama masa ini, dan mengapa?"),
        KmpQuestionSuggestion("sug-p-1", KmpSuggestionCategory.EXAM, "Pemeriksaan apa yang perlu saya jalani, dan apa tujuannya?"),
        KmpQuestionSuggestion("sug-p-2", KmpSuggestionCategory.EXAM, "Bagaimana saya harus mempersiapkan diri sebelum pemeriksaan?"),
        KmpQuestionSuggestion("sug-p-3", KmpSuggestionCategory.EXAM, "Bagaimana dan kapan saya akan mendapat hasilnya, dan siapa yang akan menjelaskannya?"),
        KmpQuestionSuggestion("sug-p-4", KmpSuggestionCategory.EXAM, "Tolong jelaskan arti hasil pemeriksaan saya dengan bahasa yang mudah saya pahami."),
        KmpQuestionSuggestion("sug-t-1", KmpSuggestionCategory.TREATMENT, "Apa saja pilihan perawatan yang tersedia, beserta manfaat dan risikonya masing-masing?"),
        KmpQuestionSuggestion("sug-t-2", KmpSuggestionCategory.TREATMENT, "Bagaimana cara yang benar menggunakan obat ini, dan apa yang terjadi jika saya salah menggunakannya?"),
        KmpQuestionSuggestion("sug-t-3", KmpSuggestionCategory.TREATMENT, "Efek samping apa yang mungkin muncul, dan bagaimana saya mengenalinya?"),
        KmpQuestionSuggestion("sug-t-4", KmpSuggestionCategory.TREATMENT, "Apa yang terjadi jika saya memilih untuk tidak menjalani perawatan ini?"),
        KmpQuestionSuggestion("sug-l-1", KmpSuggestionCategory.NEXT_STEP, "Bagaimana rencana kontrol saya berikutnya, dan apa yang perlu dipantau sampai saat itu?"),
        KmpQuestionSuggestion("sug-l-2", KmpSuggestionCategory.NEXT_STEP, "Siapa yang bisa saya hubungi, dan bagaimana caranya, bila saya punya pertanyaan di rumah?"),
        KmpQuestionSuggestion("sug-l-3", KmpSuggestionCategory.NEXT_STEP, "Tanda-tanda apa yang berarti saya perlu segera kembali, dan ke mana saya harus pergi?"),
        KmpQuestionSuggestion("sug-l-4", KmpSuggestionCategory.NEXT_STEP, "Apa yang perlu saya siapkan atau bawa pada kunjungan berikutnya?"),
        KmpQuestionSuggestion("sug-u-1", KmpSuggestionCategory.UNDERSTANDING, "Apa masalah utama saya saat ini, menurut tenaga kesehatan?"),
        KmpQuestionSuggestion("sug-u-2", KmpSuggestionCategory.UNDERSTANDING, "Apa yang perlu saya lakukan berikutnya, langkah demi langkah?"),
        KmpQuestionSuggestion("sug-u-3", KmpSuggestionCategory.UNDERSTANDING, "Mengapa langkah tersebut penting untuk saya?"),
        KmpQuestionSuggestion("sug-u-4", KmpSuggestionCategory.UNDERSTANDING, "Bagaimana penjelasan ini bisa saya sampaikan kepada keluarga di rumah?")
    )
}

object KmpValidation {
    const val MAX_CONSULTATION_TITLE = 80
    const val MAX_CONSULTATION_REASON = 500
    const val MAX_OPTIONAL = 100
    const val MAX_QUESTION = 250
    const val MAX_ANSWER = 1000
    const val MAX_NOTE = 2000
    const val MAX_FOLLOW_UP = 250

    private val CLOSED_STARTERS = setOf("apakah", "bolehkah", "bisakah", "haruskah", "perlukah", "benarkah", "sudahkah", "adakah")

    fun normalizeLine(text: String): String = text.trim().replace(Regex("\\s+"), " ")
    fun normalizeDuplicate(text: String): String = normalizeLine(text).lowercase()

    fun isClosedQuestion(text: String): Boolean {
        val firstWord = normalizeLine(text).lowercase().substringBefore(' ')
        return firstWord in CLOSED_STARTERS
    }

    fun validateConsultation(title: String, reason: String, facility: String?, provider: String?): String? {
        val t = normalizeLine(title)
        val r = reason.trim()
        if (t.isEmpty()) return "Judul kunjungan wajib diisi."
        if (t.length > MAX_CONSULTATION_TITLE) return "Judul kunjungan maksimal $MAX_CONSULTATION_TITLE karakter."
        if (r.isEmpty()) return "Alasan kunjungan wajib diisi."
        if (r.length > MAX_CONSULTATION_REASON) return "Alasan kunjungan maksimal $MAX_CONSULTATION_REASON karakter."
        if (!facility.isNullOrBlank() && normalizeLine(facility).length > MAX_OPTIONAL) {
            return "Nama fasilitas maksimal $MAX_OPTIONAL karakter."
        }
        if (!provider.isNullOrBlank() && normalizeLine(provider).length > MAX_OPTIONAL) {
            return "Nama tenaga kesehatan maksimal $MAX_OPTIONAL karakter."
        }
        return null
    }
}

/**
 * Pure Kotlin date/time formatter & converter (Howard Hinnant algorithm) — bebas java.time agar 100% kompatibel iOS/JVM/Android.
 */
object KmpDateUtils {
    private val MONTH_SHORT = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

    data class LocalParts(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int)

    fun toLocalParts(epochMillis: Long): LocalParts {
        val offsetSec = localOffsetSeconds(epochMillis)
        val localSec = epochMillis / 1000L + offsetSec
        val days = floorDiv(localSec, 86400L)
        val secOfDay = (localSec - days * 86400L).toInt()
        val (y, m, d) = civilFromDays(days)
        val hour = secOfDay / 3600
        val minute = (secOfDay % 3600) / 60
        return LocalParts(y, m, d, hour, minute)
    }

    fun fromLocalParts(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val days = daysFromCivil(year, month, day)
        val localSec = days * 86400L + hour * 3600L + minute * 60L
        val approxUtc = localSec * 1000L - localOffsetSeconds(localSec * 1000L) * 1000L
        return localSec * 1000L - localOffsetSeconds(approxUtc) * 1000L
    }

    fun epochDayFromLocalParts(year: Int, month: Int, day: Int): Long = daysFromCivil(year, month, day)

    fun formatDateTime(epochMillis: Long): String {
        val p = toLocalParts(epochMillis)
        val h = p.hour.toString().padStart(2, '0')
        val min = p.minute.toString().padStart(2, '0')
        return "${p.day} ${MONTH_SHORT[(p.month - 1).coerceIn(0, 11)]} ${p.year}, $h:$min"
    }

    fun formatEpochDay(epochDay: Long): String {
        val (y, m, d) = civilFromDays(epochDay)
        return "$d ${MONTH_SHORT[(m - 1).coerceIn(0, 11)]} $y"
    }

    fun formatRelative(epochMillis: Long, nowMillis: Long = currentTimeMillis()): String {
        val diffMin = (epochMillis - nowMillis) / 60_000L
        return when {
            diffMin in -59..0 -> "${-diffMin} menit lalu"
            diffMin in 1..59 -> "$diffMin menit lagi"
            diffMin in 60..1439 -> "${diffMin / 60} jam lagi"
            diffMin >= 1440 -> "${diffMin / 1440} hari lagi"
            else -> formatDateTime(epochMillis)
        }
    }

    fun formatDurationMs(durationMs: Long?): String {
        val totalSec = ((durationMs ?: 0L) / 1000L).coerceAtLeast(0L)
        val m = totalSec / 60
        val s = totalSec % 60
        return "${m}:${s.toString().padStart(2, '0')}"
    }

    fun formatFileSize(sizeBytes: Long): String = when {
        sizeBytes < 1024 -> "$sizeBytes B"
        sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
        else -> {
            val tenths = (sizeBytes * 10L) / (1024L * 1024L)
            "${tenths / 10}.${tenths % 10} MB"
        }
    }

    private fun floorDiv(a: Long, b: Long): Long {
        var q = a / b
        if ((a xor b) < 0 && q * b != a) q--
        return q
    }

    private fun civilFromDays(zIn: Long): Triple<Int, Int, Int> {
        val z = zIn + 719468L
        val era = (if (z >= 0) z else z - 146096L) / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
        val y = yoe + era * 400L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val d = (doy - (153L * mp + 2L) / 5L + 1L).toInt()
        val m = (mp + (if (mp < 10L) 3L else -9L)).toInt()
        val year = (y + if (m <= 2) 1L else 0L).toInt()
        return Triple(year, m, d)
    }

    private fun daysFromCivil(yIn: Int, m: Int, d: Int): Long {
        val y = yIn.toLong() - (if (m <= 2) 1L else 0L)
        val era = (if (y >= 0L) y else y - 399L) / 400L
        val yoe = y - era * 400L
        val doy = (153L * (m + if (m > 2) -3L else 9L) + 2L) / 5L + d - 1L
        val doe = yoe * 365L + yoe / 4L - yoe / 100L + doy
        return era * 146097L + doe - 719468L
    }
}

object KmpReminderScheduler {
    fun rescheduleAll(
        scheduler: PlatformAlarmScheduler,
        state: KmpLocalDatabaseState,
        nowMillis: Long = currentTimeMillis()
    ) {
        val s = state.reminderSettings
        state.consultations.forEach { c ->
            listOf("h1", "30m", "at", "custom", "prep").forEach { suffix ->
                scheduler.cancelAlarm("visit_${c.id}_$suffix")
            }
            if (c.status == "SCHEDULED" && c.appointmentAtMillis > nowMillis) {
                val label = if (s.showTitleOnLockScreen) c.title else "Jadwal Kontrol Kesehatan"
                if (s.visitThirtyMinBeforeEnabled) {
                    val triggerAt = c.appointmentAtMillis - 30 * 60_000L
                    if (triggerAt > nowMillis) {
                        scheduler.scheduleAlarm(
                            PlatformAlarmRequest(
                                key = "visit_${c.id}_30m",
                                title = "30 Menit Lagi: $label",
                                message = "Siapkan daftar pertanyaan dan catatan Anda.",
                                triggerAtEpochMillis = triggerAt,
                                useAlarmSound = s.alarmSoundModeEnabled
                            )
                        )
                    }
                }
                if (s.visitAtTimeEnabled) {
                    scheduler.scheduleAlarm(
                        PlatformAlarmRequest(
                            key = "visit_${c.id}_at",
                            title = "Saatnya Kontrol: $label",
                            message = "Buka Mode Konsultasi untuk mencatat jawaban dokter.",
                            triggerAtEpochMillis = c.appointmentAtMillis,
                            useAlarmSound = s.alarmSoundModeEnabled
                        )
                    )
                }
                val customMin = s.customLeadMinutes
                if (customMin != null && customMin > 0) {
                    val triggerAt = c.appointmentAtMillis - customMin * 60_000L
                    if (triggerAt > nowMillis) {
                        scheduler.scheduleAlarm(
                            PlatformAlarmRequest(
                                key = "visit_${c.id}_custom",
                                title = "$customMin Menit Lagi: $label",
                                message = "Bersiap menuju jadwal konsultasi Anda.",
                                triggerAtEpochMillis = triggerAt,
                                useAlarmSound = s.alarmSoundModeEnabled
                            )
                        )
                    }
                }
                if (s.visitReminderEnabled) {
                    val parts = KmpDateUtils.toLocalParts(c.appointmentAtMillis)
                    val h1Trigger = KmpDateUtils.fromLocalParts(parts.year, parts.month, parts.day, s.visitReminderHour, 0) - 86_400_000L
                    if (h1Trigger > nowMillis) {
                        scheduler.scheduleAlarm(
                            PlatformAlarmRequest(
                                key = "visit_${c.id}_h1",
                                title = "Besok Kontrol: $label",
                                message = "Cek kembali pertanyaan yang ingin Anda ajukan.",
                                triggerAtEpochMillis = h1Trigger,
                                useAlarmSound = false
                            )
                        )
                    }
                }
            }
        }
    }
}
