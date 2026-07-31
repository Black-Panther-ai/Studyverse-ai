package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val userType: String = "Student", // Student or Admin
    val role: String = "Student", // Student or Admin
    val collegeName: String = "",
    val course: String = "",
    val semester: String = "",
    val state: String = "",
    val profilePicture: String = "",
    val phoneWhatsApp: String = "",
    val upiQrUrl: String = "",
    val rating: Float = 5.0f,
    val totalEarnings: Double = 0.0,
    val notesUploadedCount: Int = 0,
    val createdDate: String = "2026-07-24",
    val isDisabled: Boolean = false
)
