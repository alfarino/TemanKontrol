package id.temankontrol.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = ConsultationEntity::class,
            parentColumns = ["id"],
            childColumns = ["consultation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["consultation_id", "text_normalized"], unique = true),
        Index(value = ["consultation_id", "is_priority"])
    ]
)
data class QuestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "consultation_id")
    val consultationId: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "text_normalized")
    val textNormalized: String,

    @ColumnInfo(name = "is_priority")
    val isPriority: Boolean,

    @ColumnInfo(name = "is_discussed")
    val isDiscussed: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "answer")
    val answer: String? = null,

    @ColumnInfo(name = "answer_updated_at")
    val answerUpdatedAt: Long? = null
)
