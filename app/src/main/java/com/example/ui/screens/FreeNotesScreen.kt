package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.NoteEntity
import com.example.ui.components.AdBanner
import com.example.ui.components.AdNative
import com.example.ui.components.AppFooter
import com.example.ui.components.NoteCard
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel

@Composable
fun FreeNotesScreen(
    viewModel: MainViewModel,
    onNoteSelect: (NoteEntity) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val freeNotes by viewModel.freeNotes.collectAsState()
    val myBookmarks by viewModel.myBookmarks.collectAsState()
    val bookmarkIds = remember(myBookmarks) { myBookmarks.map { it.itemId }.toSet() }

    var searchInput by remember { mutableStateOf("") }

    val filteredNotes = remember(freeNotes, searchInput) {
        freeNotes.filter { note ->
            searchInput.isBlank() ||
                    note.title.contains(searchInput, ignoreCase = true) ||
                    note.subject.contains(searchInput, ignoreCase = true) ||
                    note.college.contains(searchInput, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("free_notes_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Free Handwritten Notes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Shared freely by top students across colleges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = { Text("Search subject, course, or college...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("free_notes_search_input"),
                    singleLine = true
                )
            }
        }

        if (filteredNotes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (freeNotes.isEmpty()) "No notes uploaded" else "No free notes found matching search.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            itemsIndexed(filteredNotes) { index, note ->
                Column {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        NoteCard(
                            note = note,
                            isBookmarked = bookmarkIds.contains(note.id),
                            onNoteClick = { onNoteSelect(note) },
                            onBookmarkToggle = {
                                viewModel.toggleBookmark(note.id, note.title, "NOTE", note.subject, note.price)
                            },
                            onActionClick = {
                                viewModel.downloadFreeNote(note, context)
                            }
                        )
                    }

                    if ((index + 1) % 4 == 0 && (index + 1) % 8 != 0) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            AdNative()
                        }
                    } else if ((index + 1) % 8 == 0) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            AdBanner()
                        }
                    }
                }
            }
        }

        item {
            AppFooter()
        }
    }
}
