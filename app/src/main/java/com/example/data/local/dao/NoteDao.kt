package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY downloadsCount DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isFree = 1 ORDER BY downloadsCount DESC")
    fun getFreeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDigitalMarketplace = 1 ORDER BY rating DESC")
    fun getDigitalNotesStore(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: String): Flow<NoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}
