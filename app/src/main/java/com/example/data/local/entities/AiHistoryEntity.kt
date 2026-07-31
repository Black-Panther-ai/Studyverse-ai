package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_history")
data class AiHistoryEntity(
    @PrimaryKey val id: String,
    val prompt: String,
    val result: String,
    val type: String, // CHAT, EXPLAIN, SUMMARIZE, QUIZ, FLASHCARD, MCQ, REVISION, PLANNER
    val timestamp: Long = System.currentTimeMillis()
)
