package id.temankontrol.app.data.local.mapper

import id.temankontrol.app.data.local.entity.*
import id.temankontrol.app.domain.model.*
import java.time.Instant
import java.time.LocalDate

fun ConsultationEntity.toDomain(): Consultation {
    return Consultation(
        id = ConsultationId(id),
        title = title,
        appointmentAt = Instant.ofEpochMilli(appointmentAt),
        reason = reason,
        facilityName = facilityName,
        providerName = providerName,
        status = ConsultationStatus.valueOf(status),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt)
    )
}

fun ConsultationSummaryTuple.toDomain(startOfTodayMillis: Long): ConsultationSummary {
    val statusEnum = ConsultationStatus.valueOf(status)
    return ConsultationSummary(
        id = ConsultationId(id),
        title = title,
        appointmentAt = Instant.ofEpochMilli(appointmentAt),
        facilityName = facilityName,
        status = statusEnum,
        questionCount = questionCount,
        discussedCount = discussedCount,
        openActionCount = openActionCount,
        isPastDue = statusEnum == ConsultationStatus.SCHEDULED && appointmentAt < startOfTodayMillis
    )
}

fun QuestionEntity.toDomain(): Question {
    return Question(
        id = id,
        consultationId = ConsultationId(consultationId),
        text = text,
        isPriority = isPriority,
        isDiscussed = isDiscussed,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        answer = answer,
        answerUpdatedAt = answerUpdatedAt?.let { Instant.ofEpochMilli(it) }
    )
}

fun AttachmentEntity.toDomain(): Attachment {
    return Attachment(
        id = id,
        consultationId = ConsultationId(consultationId),
        questionId = questionId,
        target = runCatching { AttachmentTarget.valueOf(target) }.getOrDefault(AttachmentTarget.ADDITIONAL_NOTE),
        kind = AttachmentKind.valueOf(kind),
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        durationMs = durationMs,
        caption = caption,
        uriString = uriString,
        createdAt = Instant.ofEpochMilli(createdAt)
    )
}

fun ConsultationNoteEntity.toDomain(): ConsultationNote {
    return ConsultationNote(
        consultationId = ConsultationId(consultationId),
        summary = summary,
        providerInstruction = providerInstruction,
        additionalNotes = additionalNotes,
        updatedAt = Instant.ofEpochMilli(updatedAt)
    )
}

fun FollowUpActionEntity.toDomain(): FollowUpAction {
    return FollowUpAction(
        id = id,
        consultationId = ConsultationId(consultationId),
        text = text,
        dueDate = dueDate?.let { LocalDate.ofEpochDay(it) },
        isCompleted = isCompleted,
        completedAt = completedAt?.let { Instant.ofEpochMilli(it) },
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt)
    )
}

fun OpenFollowUpTuple.toDomain(): OpenFollowUp {
    return OpenFollowUp(
        action = action.toDomain(),
        consultationTitle = consultationTitle
    )
}
