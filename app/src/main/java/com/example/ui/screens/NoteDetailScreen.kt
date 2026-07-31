package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entities.NoteEntity
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel

@Composable
fun NoteDetailScreen(
    note: NoteEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isProcessingPayment by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("note_detail_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button_note_detail")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (note.isFree) "Free Study Notes" else "Premium Notes Store",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Surface(
                    color = if (note.isFree) AccentGreen.copy(alpha = 0.12f) else PrimaryBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${note.subject} • ${note.course}",
                        color = if (note.isFree) AccentGreen else PrimaryBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Author: ${note.authorName} • ${note.authorCollege} (${note.semester})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preview Images Section if available
                if (note.sampleImageUrls.isNotBlank()) {
                    Text("Sample Note Previews", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val previewUrls = note.sampleImageUrls.split(",").filter { it.isNotBlank() }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(previewUrls) { imgUrl ->
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = "Sample Preview",
                                modifier = Modifier
                                    .size(110.dp, 140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Stats Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Type", fontSize = 11.sp, color = Color.Gray)
                            Text(if (note.isFree) "FREE" else "PREMIUM", fontWeight = FontWeight.Bold, color = if (note.isFree) AccentGreen else PrimaryBlue)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Price", fontSize = 11.sp, color = Color.Gray)
                            Text(if (note.isFree) "₹0" else "₹${note.price.toInt()}", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rating", fontSize = 11.sp, color = Color.Gray)
                            Text("${note.rating} ★", fontWeight = FontWeight.Bold, color = Color(0xFFFFB800))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Downloads", fontSize = 11.sp, color = Color.Gray)
                            Text("${note.downloadsCount}", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Copyright Verified & Watermarked", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AccentGreen)
                            Text("Original student author notes. Instant PDF download after download/payment.", fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Bar
                Button(
                    onClick = {
                        if (note.isFree) {
                            viewModel.downloadFreeNote(note)
                        } else {
                            viewModel.purchaseDigitalNoteWithInstamojo(context, note, simulateSuccess = true)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("note_detail_action_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (note.isFree) AccentGreen else PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (note.isFree) Icons.Default.Download else Icons.Default.LockOpen,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (note.isFree) "Download Free Notes (PDF)" else "Pay ₹${note.price.toInt()} via Instamojo Gateway",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
