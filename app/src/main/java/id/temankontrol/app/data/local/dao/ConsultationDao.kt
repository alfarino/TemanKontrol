package id.temankontrol.app.data.local.dao

import androidx.room.*
import id.temankontrol.app.data.local.entity.ConsultationEntity
import id.temankontrol.app.data.local.entity.ConsultationSummaryTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsultationDao {

    @Query("""
        SELECT 
            c.id AS id,
            c.title AS title,
            c.appointment_at AS appointment_at,
            c.facility_name AS facility_name,
            c.status AS status,
            (SELECT COUNT(*) FROM questions q WHERE q.consultation_id = c.id) AS question_count,
            (SELECT COUNT(*) FROM questions q WHERE q.consultation_id = c.id AND q.is_discussed = 1) AS discussed_count,
            (SELECT COUNT(*) FROM follow_up_actions f WHERE f.consultation_id = c.id AND f.is_completed = 0) AS open_action_count
        FROM consultations c
        WHERE c.status = 'SCHEDULED' AND c.appointment_at >= :startOfTodayMillis
        ORDER BY c.appointment_at ASC
        LIMIT :limit
    """)
    fun observeUpcoming(startOfTodayMillis: Long, limit: Int): Flow<List<ConsultationSummaryTuple>>

    @Query("""
        SELECT COUNT(*) FROM consultations 
        WHERE status = 'SCHEDULED' AND appointment_at < :startOfTodayMillis
    """)
    fun observePastDueCount(startOfTodayMillis: Long): Flow<Int>

    @Query("""
        SELECT 
            c.id AS id,
            c.title AS title,
            c.appointment_at AS appointment_at,
            c.facility_name AS facility_name,
            c.status AS status,
            (SELECT COUNT(*) FROM questions q WHERE q.consultation_id = c.id) AS question_count,
            (SELECT COUNT(*) FROM questions q WHERE q.consultation_id = c.id AND q.is_discussed = 1) AS discussed_count,
            (SELECT COUNT(*) FROM follow_up_actions f WHERE f.consultation_id = c.id AND f.is_completed = 0) AS open_action_count
        FROM consultations c
        WHERE (:statusFilter IS NULL OR c.status = :statusFilter)
        ORDER BY c.appointment_at DESC
    """)
    fun observeHistory(statusFilter: String?): Flow<List<ConsultationSummaryTuple>>

    @Query("SELECT * FROM consultations WHERE id = :id")
    fun observeById(id: String): Flow<ConsultationEntity?>

    @Query("SELECT * FROM consultations WHERE id = :id")
    suspend fun getById(id: String): ConsultationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(consultation: ConsultationEntity): Long

    @Upsert
    suspend fun upsertSync(consultation: ConsultationEntity): Long

    @Query("DELETE FROM consultations")
    suspend fun deleteAll(): Int

    @Update
    suspend fun update(consultation: ConsultationEntity): Int

    @Query("UPDATE consultations SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun setStatus(id: String, status: String, updatedAt: Long): Int

    @Query("DELETE FROM consultations WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT COUNT(*) FROM questions WHERE consultation_id = :id")
    suspend fun getQuestionCount(id: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM consultation_notes WHERE consultation_id = :id)")
    suspend fun hasNote(id: String): Boolean

    @Query("SELECT COUNT(*) FROM follow_up_actions WHERE consultation_id = :id")
    suspend fun getActionCount(id: String): Int

    @Query("SELECT COUNT(*) FROM consultations")
    suspend fun countAll(): Int

    @Query("SELECT * FROM consultations")
    suspend fun getAll(): List<ConsultationEntity>
}
