package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AiStudyAssistantScreen(
    viewModel: MainViewModel
) {
    val promptInput by viewModel.aiPromptInput.collectAsState()
    val responseOutput by viewModel.aiResponseOutput.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()
    val activeTool by viewModel.activeAiTool.collectAsState()
    val history by viewModel.aiHistory.collectAsState()

    val aiTools = listOf(
        "Explain" to Icons.Default.Lightbulb,
        "Summarize" to Icons.Default.Summarize,
        "Quiz" to Icons.Default.Quiz,
        "Flashcard" to Icons.Default.Style,
        "MCQ" to Icons.Default.FormatListNumbered,
        "Revision" to Icons.Default.MenuBook,
        "Planner" to Icons.Default.CalendarMonth
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_study_assistant_screen")
            .padding(bottom = 80.dp)
    ) {
        // Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PrimaryBlue)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini AI Study Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Instant topic explanations, note summaries, interactive quizzes, flashcards, and exam study planners.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // Tools Selector Chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            items(aiTools) { (toolName, icon) ->
                val isSelected = activeTool == toolName
                Surface(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.activeAiTool.value = toolName }
                        .testTag("ai_tool_$toolName"),
                    color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = toolName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Prompt Input Field Bento Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { viewModel.aiPromptInput.value = it },
                    placeholder = {
                        val placeholder = when (activeTool) {
                            "Explain" -> "e.g. Explain Process Synchronization in Operating Systems with diagram..."
                            "Summarize" -> "Paste notes or textbook text to summarize into key points..."
                            "Quiz" -> "e.g. Generate a quiz on Data Structures & Algorithms..."
                            "Flashcard" -> "e.g. Active recall flashcards for Human Physiology..."
                            "MCQ" -> "e.g. High-yield MCQs for GATE Computer Science..."
                            "Revision" -> "e.g. Quick revision cheat sheet for Financial Management..."
                            "Planner" -> "e.g. 7-day exam revision plan for B.Tech Semester 5..."
                            else -> "Ask StudySwap AI anything..."
                        }
                        Text(placeholder, fontSize = 13.sp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("ai_prompt_input_field"),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.executeAiAssistantTool(promptInput, activeTool)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("execute_ai_button"),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini AI is generating...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate with Gemini AI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI Response Output Display
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (responseOutput.isNotEmpty()) {
                item {
                    Text(
                        text = "AI Result ($activeTool)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = responseOutput,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recent AI Study Logs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }

                items(history.take(5)) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = PrimaryBlue.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.type,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = PrimaryBlue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
