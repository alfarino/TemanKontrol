package id.temankontrol.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.temankontrol.app.data.local.entity.ReminderLogEntity

@Dao
interface ReminderLogDao {

    @Query("SELECT EXISTS(SELECT 1 FROM reminder_log WHERE `key` = :key)")
    suspend fun exists(key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ReminderLogEntity): Long

    @Query("DELETE FROM reminder_log WHERE fired_at < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM reminder_log")
    suspend fun clearAll(): Int
}
