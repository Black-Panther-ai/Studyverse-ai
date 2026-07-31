package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdDate DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY totalEarnings DESC LIMIT 10")
    fun getTopSellers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY notesUploadedCount DESC LIMIT 10")
    fun getTopContributors(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}
