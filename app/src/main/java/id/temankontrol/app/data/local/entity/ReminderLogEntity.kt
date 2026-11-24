package id.temankontrol.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_log")
data class ReminderLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "fired_at")
    val firedAt: Long
)
