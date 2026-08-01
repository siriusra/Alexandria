package com.alexandria.app.di

import android.content.Context
import com.alexandria.app.data.local.AlexandriaDatabase
import com.alexandria.app.data.local.BookCharacterDao
import com.alexandria.app.data.local.BookDao
import com.alexandria.app.data.local.CoverCacheDao
import com.alexandria.app.data.local.PreferencesManager
import com.alexandria.app.data.remote.PortadaResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AlexandriaDatabase {
        return AlexandriaDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideBookDao(database: AlexandriaDatabase): BookDao {
        return database.bookDao()
    }

    @Provides
    @Singleton
    fun provideCoverCacheDao(database: AlexandriaDatabase): CoverCacheDao {
        return database.coverCacheDao()
    }

    @Provides
    @Singleton
    fun provideBookCharacterDao(database: AlexandriaDatabase): BookCharacterDao {
        return database.bookCharacterDao()
    }

    @Provides
    @Singleton
    fun providePortadaResolver(): PortadaResolver {
        return PortadaResolver()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
}