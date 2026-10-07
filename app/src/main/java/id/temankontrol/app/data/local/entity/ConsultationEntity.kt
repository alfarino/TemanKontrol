package id.temankontrol.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "consultations",
    indices = [
        Index(value = ["status", "appointment_at"]),
        Index(value = ["appointment_at"])
    ]
)
data class ConsultationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "appointment_at")
    val appointmentAt: Long,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "facility_name")
    val facilityName: String?,

    @ColumnInfo(name = "provider_name")
    val providerName: String?,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
