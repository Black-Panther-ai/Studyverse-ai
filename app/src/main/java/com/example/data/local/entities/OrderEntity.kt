package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val buyerId: String,
    val buyerName: String,
    val sellerId: String,
    val itemId: String,
    val itemTitle: String,
    val itemType: String, // DIGITAL_NOTE, FREE_NOTE, PHYSICAL_PRODUCT
    val price: Double,
    val status: String = "COMPLETED", // COMPLETED, FAILED, PENDING
    val paymentId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val watermarkedDownloadUrl: String = ""
)
