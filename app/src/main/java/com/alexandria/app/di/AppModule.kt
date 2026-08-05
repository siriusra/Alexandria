package com.alexandria.app.di

import android.content.Context
import com.alexandria.app.data.local.AlexandriaDatabase
import com.alexandria.app.data.local.BookCharacterDao
import com.alexandria.app.data.local.BookDao
import com.alexandria.app.data.local.CoverCacheDao
import com.alexandria.app.data.local.CoverStore
import com.alexandria.app.data.local.MetadataCacheDao
import com.alexandria.app.data.local.PreferencesManager
import com.alexandria.app.data.remote.PortadaResolver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
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
    fun provideMetadataCacheDao(database: AlexandriaDatabase): MetadataCacheDao {
        return database.metadataCacheDao()
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

    @Provides
    @Singleton
    fun provideCoverStore(@ApplicationContext context: Context): CoverStore {
        return CoverStore(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return FirebaseFunctions.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        val defaults = mapOf(
            "use_cloud_resolution" to true,
            "metadata_ttl_days" to 7L,
            "cloud_resolve_timeout_ms" to 15_000L
        )
        return FirebaseRemoteConfig.getInstance().apply {
            setDefaultsAsync(defaults)
        }
    }
}