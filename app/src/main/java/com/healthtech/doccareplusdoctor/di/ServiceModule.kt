package  com.healthtech.doccareplusdoctor.di

import com.healthtech.doccareplusdoctor.data.service.CloudinaryServiceImpl
import com.healthtech.doccareplusdoctor.data.service.NotificationServiceImpl
import com.healthtech.doccareplusdoctor.domain.service.CloudinaryService
import com.healthtech.doccareplusdoctor.domain.service.NotificationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    @Singleton
    abstract fun bindNotificationService(
        notificationServiceImpl: NotificationServiceImpl
    ): NotificationService

    @Binds
    @Singleton
    abstract fun bindCloudinaryService(
        cloudinaryServiceImpl: CloudinaryServiceImpl
    ): CloudinaryService
}