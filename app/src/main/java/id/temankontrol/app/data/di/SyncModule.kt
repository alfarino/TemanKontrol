package id.temankontrol.app.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.temankontrol.app.data.remote.TemanKontrolApiClient
import id.temankontrol.app.data.sync.LocalSyncStorage
import id.temankontrol.app.data.sync.RoomLocalSyncStorage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindLocalSyncStorage(
        impl: RoomLocalSyncStorage
    ): LocalSyncStorage

    companion object {
        @Provides
        @Singleton
        fun provideApiClient(): TemanKontrolApiClient = TemanKontrolApiClient()
    }
}
