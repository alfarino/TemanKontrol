package id.temankontrol.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.temankontrol.app.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE consultation_id = :consultationId ORDER BY created_at DESC")
    fun observeByConsultation(consultationId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments ORDER BY created_at DESC")
    fun observeAll(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments")
    suspend fun getAll(): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE question_id = :questionId")
    suspend fun getByQuestionId(questionId: String): List<AttachmentEntity>

    @Query("SELECT COUNT(*) FROM attachments WHERE consultation_id = :consultationId AND kind = :kind")
    suspend fun countByConsultationAndKind(consultationId: String, kind: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attachment: AttachmentEntity): Long

    @Query("UPDATE attachments SET caption = :caption WHERE id = :id")
    suspend fun updateCaption(id: String, caption: String?): Int

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: String): Int
}
