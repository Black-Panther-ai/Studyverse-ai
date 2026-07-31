package com.example.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Listing(
    val id: String = "",
    val sellerId: String = "",
    val sellerDisplayName: String = "",
    val categoryId: String = "",
    val title: String = "",
    val description: String = "",
    val pricePaise: Long = 0L,
    val originalPricePaise: Long = 0L,
    val listingType: String = "physical", // "physical", "digital_note", "ebook"
    val condition: String = "Good", // "New", "Like New", "Good", "Fair"
    val collegeName: String = "",
    val city: String = "",
    val imageUrls: List<String> = emptyList(),
    val digitalFilePath: String = "",
    val status: String = "active", // "draft", "active", "sold", "removed"
    val isApproved: Boolean = true,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
) {
    val rupeesPrice: Double get() = pricePaise / 100.0
    val rupeesOriginalPrice: Double get() = originalPricePaise / 100.0

    companion object {
        fun rupeesToPaise(rupees: Double): Long = (rupees * 100.0).toLong()
    }
}
