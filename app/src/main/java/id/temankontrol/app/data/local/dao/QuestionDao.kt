package id.temankontrol.app.data.local.dao

import androidx.room.*
import id.temankontrol.app.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Query("""
        SELECT * FROM questions 
        WHERE consultation_id = :consultationId 
        ORDER BY is_priority DESC, created_at ASC
    """)
    fun observeByConsultation(consultationId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: String): QuestionEntity?

    @Query("""
        SELECT COUNT(*) FROM questions 
        WHERE consultation_id = :consultationId AND is_priority = 1 AND (:excludeQuestionId IS NULL OR id != :excludeQuestionId)
    """)
    suspend fun priorityCount(consultationId: String, excludeQuestionId: String?): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(question: QuestionEntity): Long

    @Update
    suspend fun update(question: QuestionEntity): Int

    @Query("UPDATE questions SET is_discussed = :discussed, updated_at = :updatedAt WHERE id = :id")
    suspend fun setDiscussed(id: String, discussed: Boolean, updatedAt: Long): Int

    @Query("UPDATE questions SET answer = :answer, answer_updated_at = :answerUpdatedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateAnswer(id: String, answer: String?, answerUpdatedAt: Long?, updatedAt: Long): Int

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM questions")
    suspend fun getAll(): List<QuestionEntity>
}
