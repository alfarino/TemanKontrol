package id.temankontrol.app.data.local.dao

import androidx.room.*
import id.temankontrol.app.data.local.entity.FollowUpActionEntity
import id.temankontrol.app.data.local.entity.OpenFollowUpTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowUpDao {

    @Query("""
        SELECT * FROM follow_up_actions 
        WHERE consultation_id = :consultationId 
        ORDER BY is_completed ASC, 
                 CASE WHEN due_date IS NULL THEN 1 ELSE 0 END ASC, 
                 due_date ASC, 
                 updated_at DESC
    """)
    fun observeByConsultation(consultationId: String): Flow<List<FollowUpActionEntity>>

    @Query("""
        SELECT f.*, c.title AS consultationTitle
        FROM follow_up_actions f
        INNER JOIN consultations c ON f.consultation_id = c.id
        WHERE f.is_completed = 0
        ORDER BY CASE WHEN f.due_date < :todayEpochDay THEN 0 ELSE 1 END ASC,
                 CASE WHEN f.due_date IS NULL THEN 1 ELSE 0 END ASC,
                 f.due_date ASC,
                 f.created_at ASC
        LIMIT :limit
    """)
    fun observeOpen(todayEpochDay: Long, limit: Int): Flow<List<OpenFollowUpTuple>>

    @Query("SELECT * FROM follow_up_actions WHERE id = :id")
    suspend fun getById(id: String): FollowUpActionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(action: FollowUpActionEntity): Long

    @Upsert
    suspend fun upsertSync(action: FollowUpActionEntity): Long

    @Update
    suspend fun update(action: FollowUpActionEntity): Int

    @Query("UPDATE follow_up_actions SET is_completed = :completed, completed_at = :completedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean, completedAt: Long?, updatedAt: Long): Int

    @Query("DELETE FROM follow_up_actions WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM follow_up_actions")
    suspend fun getAll(): List<FollowUpActionEntity>
}
