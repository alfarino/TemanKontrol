package id.temankontrol.app.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.temankontrol.app.data.local.AppDatabase
import id.temankontrol.app.data.local.Migrations
import id.temankontrol.app.data.local.dao.*
import id.temankontrol.app.data.preferences.userPreferencesDataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "teman_kontrol.db"
        )
            .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideConsultationDao(database: AppDatabase): ConsultationDao = database.consultationDao()

    @Provides
    fun provideQuestionDao(database: AppDatabase): QuestionDao = database.questionDao()

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideFollowUpDao(database: AppDatabase): FollowUpDao = database.followUpDao()

    @Provides
    fun provideReminderLogDao(database: AppDatabase): ReminderLogDao = database.reminderLogDao()

    @Provides
    fun provideAttachmentDao(database: AppDatabase): AttachmentDao = database.attachmentDao()

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.userPreferencesDataStore
}
