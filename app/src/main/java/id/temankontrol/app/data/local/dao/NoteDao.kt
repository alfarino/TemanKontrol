package id.temankontrol.app.data.local.dao

import androidx.room.*
import id.temankontrol.app.data.local.entity.ConsultationNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM consultation_notes WHERE consultation_id = :consultationId")
    fun observe(consultationId: String): Flow<ConsultationNoteEntity?>

    @Query("SELECT * FROM consultation_notes WHERE consultation_id = :consultationId")
    suspend fun getByConsultationId(consultationId: String): ConsultationNoteEntity?

    @Upsert
    suspend fun upsert(note: ConsultationNoteEntity): Long

    @Query("DELETE FROM consultation_notes WHERE consultation_id = :consultationId")
    suspend fun delete(consultationId: String): Int
}
