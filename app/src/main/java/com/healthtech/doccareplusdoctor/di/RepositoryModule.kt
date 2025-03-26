package  com.healthtech.doccareplusdoctor.di

import com.healthtech.doccareplusdoctor.data.repository.AuthRepositoryImpl
import com.healthtech.doccareplusdoctor.data.repository.StorageRepositoryImpl
import com.healthtech.doccareplusdoctor.domain.repository.AuthRepository
import com.healthtech.doccareplusdoctor.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository


    @Binds
    @Singleton
    abstract fun bindStorageRepository(
        storageRepositoryImpl: StorageRepositoryImpl
    ): StorageRepository
}