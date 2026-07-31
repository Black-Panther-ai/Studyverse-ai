package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE userId = :userId ORDER BY timestamp DESC")
    fun getBookmarksByUser(userId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE userId = :userId AND itemId = :itemId)")
    fun isBookmarked(userId: String, itemId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE userId = :userId AND itemId = :itemId")
    suspend fun removeBookmark(userId: String, itemId: String)
}
