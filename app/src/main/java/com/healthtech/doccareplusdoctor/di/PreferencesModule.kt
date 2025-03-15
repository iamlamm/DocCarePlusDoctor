package  com.healthtech.doccareplusdoctor.di

import android.content.Context
import com.healthtech.doccareplusdoctor.data.local.preferences.DoctorPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): DoctorPreferences {
        return DoctorPreferences(context)
    }
}