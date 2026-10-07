package id.temankontrol.app.data.local.entity

import androidx.room.ColumnInfo

data class ConsultationSummaryTuple(
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "appointment_at")
    val appointmentAt: Long,

    @ColumnInfo(name = "facility_name")
    val facilityName: String?,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "question_count")
    val questionCount: Int,

    @ColumnInfo(name = "discussed_count")
    val discussedCount: Int,

    @ColumnInfo(name = "open_action_count")
    val openActionCount: Int
)
