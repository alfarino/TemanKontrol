package id.temankontrol.app.domain.validation

import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.ConsultationInput
import id.temankontrol.app.domain.model.FollowUpInput
import id.temankontrol.app.domain.model.NoteInput
import id.temankontrol.app.domain.model.QuestionInput
import java.time.LocalDate

object Validation {

    const val MAX_CONSULTATION_TITLE_LENGTH = 80
    const val MAX_CONSULTATION_REASON_LENGTH = 500
    const val MAX_OPTIONAL_FIELD_LENGTH = 100
    const val MAX_QUESTION_LENGTH = 250
    const val MAX_ANSWER_LENGTH = 1000
    const val MAX_CAPTION_LENGTH = 100
    const val MIN_BACKUP_PASSWORD_LENGTH = 8
    const val MAX_NOTE_FIELD_LENGTH = 2000
    const val MAX_FOLLOW_UP_ACTION_LENGTH = 250

    private val CLOSED_QUESTION_STARTERS = setOf(
        "apakah",
        "bolehkah",
        "bisakah",
        "haruskah",
        "perlukah",
        "benarkah",
        "sudahkah",
        "adakah"
    )

    fun normalizeSingleLine(text: String): String =
        text.trim().replace("\\s+".toRegex(), " ")

    fun normalizeMultiline(text: String): String =
        text.trim()

    fun normalizeQuestionForDuplicateCheck(text: String): String =
        normalizeSingleLine(text).lowercase()

    // VR-01: Judul kunjungan
    fun validateConsultationTitle(title: String): AppResult<String> {
        val normalized = normalizeSingleLine(title)
        return when {
            normalized.isEmpty() -> AppResult.Failure(AppError.Validation("title", "Judul kunjungan wajib diisi."))
            normalized.length > MAX_CONSULTATION_TITLE_LENGTH -> AppResult.Failure(
                AppError.Validation("title", "Judul kunjungan maksimal $MAX_CONSULTATION_TITLE_LENGTH karakter.")
            )
            else -> AppResult.Success(normalized)
        }
    }

    // VR-03: Alasan kunjungan
    fun validateConsultationReason(reason: String): AppResult<String> {
        val normalized = normalizeMultiline(reason)
        return when {
            normalized.isEmpty() -> AppResult.Failure(AppError.Validation("reason", "Alasan kunjungan wajib diisi."))
            normalized.length > MAX_CONSULTATION_REASON_LENGTH -> AppResult.Failure(
                AppError.Validation("reason", "Alasan kunjungan maksimal $MAX_CONSULTATION_REASON_LENGTH karakter.")
            )
            else -> AppResult.Success(normalized)
        }
    }

    // VR-04 & VR-05: Optional text fields (facility, provider)
    fun validateOptionalField(field: String, fieldName: String, value: String?): AppResult<String?> {
        if (value.isNullOrBlank()) return AppResult.Success(null)
        val normalized = normalizeSingleLine(value)
        return if (normalized.length > MAX_OPTIONAL_FIELD_LENGTH) {
            AppResult.Failure(AppError.Validation(field, "$fieldName maksimal $MAX_OPTIONAL_FIELD_LENGTH karakter."))
        } else {
            AppResult.Success(normalized)
        }
    }

    fun validateConsultationInput(input: ConsultationInput): AppResult<ConsultationInput> {
        val titleResult = validateConsultationTitle(input.title)
        if (titleResult is AppResult.Failure) return titleResult

        val reasonResult = validateConsultationReason(input.reason)
        if (reasonResult is AppResult.Failure) return reasonResult

        val facilityResult = validateOptionalField("facilityName", "Nama fasilitas", input.facilityName)
        if (facilityResult is AppResult.Failure) return facilityResult

        val providerResult = validateOptionalField("providerName", "Nama tenaga kesehatan", input.providerName)
        if (providerResult is AppResult.Failure) return providerResult

        return AppResult.Success(
            input.copy(
                title = (titleResult as AppResult.Success).data,
                reason = (reasonResult as AppResult.Success).data,
                facilityName = (facilityResult as AppResult.Success).data,
                providerName = (providerResult as AppResult.Success).data
            )
        )
    }

    // VR-06: Teks pertanyaan
    fun validateQuestionText(text: String): AppResult<String> {
        val normalized = normalizeSingleLine(text)
        return when {
            normalized.isEmpty() -> AppResult.Failure(AppError.Validation("question", "Pertanyaan wajib diisi."))
            normalized.length > MAX_QUESTION_LENGTH -> AppResult.Failure(
                AppError.Validation("question", "Pertanyaan maksimal $MAX_QUESTION_LENGTH karakter.")
            )
            else -> AppResult.Success(normalized)
        }
    }

    // VR-07: Prioritas tidak lagi dibatasi maksimal 3 (sesuai revisi masukan pengguna)
    @Suppress("UNUSED_PARAMETER")
    fun validatePriorityLimit(currentPriorityCount: Int, addingPriority: Boolean): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    // VR-13: Jawaban yang dicatat pengguna (opsional, maks 1.000 karakter)
    fun validateAnswerText(answer: String?): AppResult<String?> {
        if (answer.isNullOrBlank()) return AppResult.Success(null)
        val normalized = normalizeMultiline(answer)
        return if (normalized.length > MAX_ANSWER_LENGTH) {
            AppResult.Failure(AppError.Validation("answer", "Jawaban maksimal $MAX_ANSWER_LENGTH karakter."))
        } else {
            AppResult.Success(normalized)
        }
    }

    // VR-14: Keterangan lampiran (opsional, maks 100 karakter)
    fun validateCaptionText(caption: String?): AppResult<String?> {
        if (caption.isNullOrBlank()) return AppResult.Success(null)
        val normalized = normalizeSingleLine(caption)
        return if (normalized.length > MAX_CAPTION_LENGTH) {
            AppResult.Failure(AppError.Validation("caption", "Keterangan maksimal $MAX_CAPTION_LENGTH karakter."))
        } else {
            AppResult.Success(normalized)
        }
    }

    // VR-17: Kata sandi cadangan
    fun validateBackupPassword(password: String, confirm: String): AppResult<Unit> {
        if (password.length < MIN_BACKUP_PASSWORD_LENGTH) {
            return AppResult.Failure(
                AppError.Validation("password", "Kata sandi minimal $MIN_BACKUP_PASSWORD_LENGTH karakter.")
            )
        }
        if (password != confirm) {
            return AppResult.Failure(
                AppError.Validation("confirmPassword", "Konfirmasi kata sandi tidak cocok.")
            )
        }
        return AppResult.Success(Unit)
    }

    // VR-18: Deteksi pertanyaan tertutup (petunjuk lembut, tidak memblokir)
    fun isLikelyClosedQuestion(text: String): Boolean {
        val trimmed = text.trim().lowercase()
        if (trimmed.isEmpty()) return false
        val firstWord = trimmed.split(Regex("\\s+|[,?.!]"), limit = 2).firstOrNull().orEmpty()
        if (firstWord in CLOSED_QUESTION_STARTERS) return true
        return trimmed.endsWith("ya/tidak?") ||
            trimmed.endsWith("ya atau tidak?") ||
            trimmed.endsWith(", ya?")
    }

    // VR-08: Duplikat pertanyaan
    fun validateNotDuplicateQuestion(
        newQuestionText: String,
        existingQuestionsNormalized: Set<String>
    ): AppResult<Unit> {
        val normalized = normalizeQuestionForDuplicateCheck(newQuestionText)
        return if (existingQuestionsNormalized.contains(normalized)) {
            AppResult.Failure(AppError.Duplicate)
        } else {
            AppResult.Success(Unit)
        }
    }

    // VR-09 & VR-10: Catatan hasil konsultasi
    fun validateNoteInput(input: NoteInput): AppResult<NoteInput> {
        val summaryNorm = input.summary?.let { normalizeMultiline(it) }?.ifBlank { null }
        val providerInstructionNorm = input.providerInstruction?.let { normalizeMultiline(it) }?.ifBlank { null }
        val additionalNotesNorm = input.additionalNotes?.let { normalizeMultiline(it) }?.ifBlank { null }

        if (summaryNorm == null && providerInstructionNorm == null && additionalNotesNorm == null) {
            return AppResult.Failure(AppError.Validation("note", "Isi minimal satu bagian catatan sebelum menyimpan."))
        }

        if (summaryNorm != null && summaryNorm.length > MAX_NOTE_FIELD_LENGTH) {
            return AppResult.Failure(AppError.Validation("summary", "Ringkasan maksimal $MAX_NOTE_FIELD_LENGTH karakter."))
        }
        if (providerInstructionNorm != null && providerInstructionNorm.length > MAX_NOTE_FIELD_LENGTH) {
            return AppResult.Failure(AppError.Validation("providerInstruction", "Arahan maksimal $MAX_NOTE_FIELD_LENGTH karakter."))
        }
        if (additionalNotesNorm != null && additionalNotesNorm.length > MAX_NOTE_FIELD_LENGTH) {
            return AppResult.Failure(AppError.Validation("additionalNotes", "Catatan tambahan maksimal $MAX_NOTE_FIELD_LENGTH karakter."))
        }

        return AppResult.Success(
            NoteInput(
                summary = summaryNorm,
                providerInstruction = providerInstructionNorm,
                additionalNotes = additionalNotesNorm
            )
        )
    }

    // VR-11: Teks tindak lanjut
    fun validateFollowUpText(text: String): AppResult<String> {
        val normalized = normalizeSingleLine(text)
        return when {
            normalized.isEmpty() -> AppResult.Failure(AppError.Validation("action", "Tindak lanjut wajib diisi."))
            normalized.length > MAX_FOLLOW_UP_ACTION_LENGTH -> AppResult.Failure(
                AppError.Validation("action", "Tindak lanjut maksimal $MAX_FOLLOW_UP_ACTION_LENGTH karakter.")
            )
            else -> AppResult.Success(normalized)
        }
    }

    // VR-12: Tenggat tindak lanjut (lampau diizinkan, mengembalikan status warning)
    fun isDueDatePast(dueDate: LocalDate?, today: LocalDate = LocalDate.now()): Boolean {
        return dueDate != null && dueDate.isBefore(today)
    }

    fun validateFollowUpInput(input: FollowUpInput): AppResult<FollowUpInput> {
        val textResult = validateFollowUpText(input.text)
        if (textResult is AppResult.Failure) return textResult

        return AppResult.Success(
            input.copy(text = (textResult as AppResult.Success).data)
        )
    }
}
