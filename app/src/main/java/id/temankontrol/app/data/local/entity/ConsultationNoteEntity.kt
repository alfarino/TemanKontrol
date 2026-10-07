package id.temankontrol.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "consultation_notes",
    foreignKeys = [
        ForeignKey(
            entity = ConsultationEntity::class,
            parentColumns = ["id"],
            childColumns = ["consultation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ConsultationNoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "consultation_id")
    val consultationId: String,

    @ColumnInfo(name = "summary")
    val summary: String?,

    @ColumnInfo(name = "provider_instruction")
    val providerInstruction: String?,

    @ColumnInfo(name = "additional_notes")
    val additionalNotes: String?,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
