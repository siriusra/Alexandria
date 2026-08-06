package com.alexandria.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.alexandria.app.data.local.BookCharacterDao
import com.alexandria.app.data.local.CoverCacheDao
import com.alexandria.app.data.local.MetadataCacheDao
import com.alexandria.app.data.local.entity.BookCharacterEntity
import com.alexandria.app.data.local.entity.BookEntity
import com.alexandria.app.data.local.entity.CoverCacheEntity
import com.alexandria.app.data.local.entity.MetadataCacheEntity

@Database(
    entities = [BookEntity::class, CoverCacheEntity::class, BookCharacterEntity::class, MetadataCacheEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AlexandriaDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun coverCacheDao(): CoverCacheDao
    abstract fun bookCharacterDao(): BookCharacterDao
    abstract fun metadataCacheDao(): MetadataCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AlexandriaDatabase? = null

        val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE books ADD COLUMN currentPage INTEGER NOT NULL DEFAULT 0")
        }

        val MIGRATION_2_3 = Migration(2, 3) { db ->
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS cover_cache (
                    isbn TEXT NOT NULL PRIMARY KEY,
                    coverUrl TEXT NOT NULL,
                    source TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
        }

        val MIGRATION_3_4 = Migration(3, 4) { db ->
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS book_characters (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    bookId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    iconType TEXT NOT NULL,
                    iconKey TEXT NOT NULL,
                    isFavorite INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_book_characters_bookId ON book_characters(bookId)")
        }

        val MIGRATION_4_5 = Migration(4, 5) { db ->
            db.execSQL("ALTER TABLE books ADD COLUMN description TEXT")
        }

        val MIGRATION_5_6 = Migration(5, 6) { db ->
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS metadata_cache (
                    isbn TEXT NOT NULL PRIMARY KEY,
                    description TEXT,
                    averageRating REAL,
                    ratingsCount INTEGER,
                    source TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    ttlMs INTEGER NOT NULL
                )
            """.trimIndent())
        }

        fun getDatabase(context: Context): AlexandriaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlexandriaDatabase::class.java,
                    "alexandria_database"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}