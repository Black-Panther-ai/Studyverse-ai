package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String, // Books, Notebooks, School Shoes, School Uniform, College Uniform, Bag, Backpack, Lab Coat, Calculator, Stationery, Electronics, Others
    val condition: String = "Like New", // New, Like New, Good, Fair
    val price: Double,
    val originalPrice: Double = price * 1.3,
    val pickupLocation: String = "Hostel Campus / Main Gate",
    val city: String = "New Delhi",
    val state: String = "Delhi",
    val college: String = "",
    val semester: String = "All Semesters",
    val course: String = "All Courses",
    val photoUrls: String = "", // Comma-separated URIs/URLs
    val sellerId: String,
    val sellerName: String,
    val sellerMobile: String = "",
    val sellerWhatsApp: String = "",
    val sellerRating: Float = 5.0f,
    val viewsCount: Int = 0,
    val wishlistCount: Int = 0,
    val isSold: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)
