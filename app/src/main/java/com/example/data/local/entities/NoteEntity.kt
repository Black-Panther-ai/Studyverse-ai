package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isFree: Boolean = true,
    val isDigitalMarketplace: Boolean = false,
    val price: Double = 0.0,
    val subject: String,
    val semester: String,
    val course: String,
    val college: String,
    val state: String = "Delhi",
    val pdfUriOrUrl: String,
    val pdfFileName: String = "HandwrittenNotes.pdf",
    val sampleImageUrls: String = "", // Comma-separated preview image URIs/URLs
    val pageCount: Int = 30,
    val rating: Float = 4.9f,
    val downloadsCount: Int = 0,
    val commentsCount: Int = 0,
    val authorId: String,
    val authorName: String,
    val authorCollege: String,
    val copyrightDeclared: Boolean = true,
    val isApprovedByAdmin: Boolean = true,
    val tags: String = "Handwritten, Exam Prep, Topper Notes"
)
