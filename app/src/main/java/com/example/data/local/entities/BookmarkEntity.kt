package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val itemType: String, // NOTE, PRODUCT
    val itemId: String,
    val title: String,
    val categoryOrSubject: String,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
)
