package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        NoteEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        BookmarkEntity::class,
        AiHistoryEntity::class,
        ReportEntity::class,
        PaymentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun aiHistoryDao(): AiHistoryDao
    abstract fun reportDao(): ReportDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studyswap_database"
                )
                    // PRE-PRODUCTION ONLY: Permitted strictly during early development because local DB contains
                    // mock/test data only. Wipes database when schema changes (e.g. v3->v4 plaintext password removal).
                    // PHASE 8 BLOCKER: Must be replaced with explicit Migration(x, y) strategies prior to production release!
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
