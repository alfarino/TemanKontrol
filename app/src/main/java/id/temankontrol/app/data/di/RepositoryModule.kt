package id.temankontrol.app.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.temankontrol.app.data.repository.*
import id.temankontrol.app.data.suggestion.StaticSuggestionCatalog
import id.temankontrol.app.domain.repository.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindConsultationRepository(
        impl: RoomConsultationRepository
    ): ConsultationRepository

    @Binds
    @Singleton
    abstract fun bindQuestionRepository(
        impl: RoomQuestionRepository
    ): QuestionRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        impl: RoomNoteRepository
    ): NoteRepository

    @Binds
    @Singleton
    abstract fun bindFollowUpRepository(
        impl: RoomFollowUpRepository
    ): FollowUpRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: RoomPreferencesRepository
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindSuggestionCatalog(
        impl: StaticSuggestionCatalog
    ): SuggestionCatalog
}
