package id.temankontrol.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import id.temankontrol.app.data.local.dao.*
import id.temankontrol.app.data.local.entity.*

@Database(
    entities = [
        ConsultationEntity::class,
        QuestionEntity::class,
        ConsultationNoteEntity::class,
        FollowUpActionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun consultationDao(): ConsultationDao
    abstract fun questionDao(): QuestionDao
    abstract fun noteDao(): NoteDao
    abstract fun followUpDao(): FollowUpDao
}
