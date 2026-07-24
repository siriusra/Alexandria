package com.alexandria.app.di

import android.content.Context
import com.alexandria.app.data.local.AlexandriaDatabase
import com.alexandria.app.data.local.BookDao
import com.alexandria.app.data.remote.DuckDuckGoCoverService
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
    fun provideDuckDuckGoCoverService(): DuckDuckGoCoverService {
        return DuckDuckGoCoverService()
    }
}
