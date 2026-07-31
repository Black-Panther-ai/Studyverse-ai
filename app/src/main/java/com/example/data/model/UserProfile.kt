package com.example.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val phone: String = "",
    val collegeName: String = "",
    val course: String = "",
    val semester: String = "",
    val canBuy: Boolean = true,
    val canSell: Boolean = false,
    val accountStatus: String = "active",
    val profileImageUrl: String = "",
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)
