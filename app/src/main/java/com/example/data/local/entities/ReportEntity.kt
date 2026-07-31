package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val reporterName: String,
    val itemType: String, // NOTE, PRODUCT, USER
    val itemId: String,
    val itemTitle: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
