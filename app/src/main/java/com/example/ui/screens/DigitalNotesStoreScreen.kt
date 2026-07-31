package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
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
import com.example.data.local.entities.NoteEntity
import com.example.ui.components.AdBanner
import com.example.ui.components.AdNative
import com.example.ui.components.AppFooter
import com.example.ui.components.NoteCard
import com.example.ui.components.WatermarkDialog
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DigitalNotesStoreScreen(
    viewModel: MainViewModel,
    onNoteSelect: (NoteEntity) -> Unit
) {
    val digitalNotes by viewModel.digitalNotesStore.collectAsState()
    val myBookmarks by viewModel.myBookmarks.collectAsState()
    val bookmarkIds = remember(myBookmarks) { myBookmarks.map { it.itemId }.toSet() }
    val currentUser by viewModel.currentUser.collectAsState()

    var searchInput by remember { mutableStateOf("") }
    var selectedNoteForPurchase by remember { mutableStateOf<NoteEntity?>(null) }
    var showWatermarkDialog by remember { mutableStateOf<NoteEntity?>(null) }

    val filteredNotes = remember(digitalNotes, searchInput) {
        digitalNotes.filter { note ->
            searchInput.isBlank() ||
                    note.title.contains(searchInput, ignoreCase = true) ||
                    note.subject.contains(searchInput, ignoreCase = true) ||
                    note.authorName.contains(searchInput, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("digital_notes_store_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Original Notes Store",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = AccentGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verified Authors", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Purchase original handwritten notes directly from toppers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = { Text("Search digital notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("digital_notes_search_input"),
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
                            text = if (digitalNotes.isEmpty()) "No notes uploaded" else "No digital notes found matching search.",
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
                                selectedNoteForPurchase = note
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

    // Purchase Modal / Sheet
    selectedNoteForPurchase?.let { note ->
        AlertDialog(
            onDismissRequest = { selectedNoteForPurchase = null },
            modifier = Modifier.testTag("purchase_digital_note_modal"),
            title = {
                Text(
                    text = "Purchase Digital Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = note.title,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Author: ${note.authorName} (${note.authorCollege})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Price: ₹${note.price.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGreen
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Watermark Guarantee: Purchased PDF includes an embedded security watermark with your name (${currentUser?.name ?: "Buyer"}) and Order ID.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val n = note
                        viewModel.purchaseDigitalNote(n)
                        selectedNoteForPurchase = null
                        showWatermarkDialog = n
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.testTag("confirm_purchase_button")
                ) {
                    Text("Confirm & Unlock Watermarked PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedNoteForPurchase = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Watermark PDF Viewer Dialog
    showWatermarkDialog?.let { note ->
        val dateStr = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()) }
        WatermarkDialog(
            noteTitle = note.title,
            buyerName = currentUser?.name ?: "Student User",
            orderId = "ORD_${System.currentTimeMillis().toString().takeLast(6)}",
            purchaseDate = dateStr,
            onDismiss = { showWatermarkDialog = null },
            onDownload = {
                viewModel.uiMessage.value = "Downloaded watermarked PDF to device!"
            }
        )
    }
}
