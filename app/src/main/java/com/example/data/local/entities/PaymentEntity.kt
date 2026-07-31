package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val buyerId: String,
    val buyerName: String,
    val itemId: String,
    val itemTitle: String,
    val amount: Double,
    val gateway: String = "Instamojo",
    val instamojoPaymentId: String = "",
    val status: String, // SUCCESS, FAILED, PENDING
    val failureReason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
