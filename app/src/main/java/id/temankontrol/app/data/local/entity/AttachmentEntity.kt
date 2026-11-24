package id.temankontrol.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = ConsultationEntity::class,
            parentColumns = ["id"],
            childColumns = ["consultation_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["consultation_id", "kind"]),
        Index(value = ["question_id"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "consultation_id")
    val consultationId: String,

    @ColumnInfo(name = "question_id")
    val questionId: String?,

    @ColumnInfo(name = "target")
    val target: String,

    @ColumnInfo(name = "kind")
    val kind: String,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long?,

    @ColumnInfo(name = "caption")
    val caption: String?,

    @ColumnInfo(name = "uri_string")
    val uriString: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
