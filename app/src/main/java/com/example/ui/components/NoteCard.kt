package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.NoteEntity
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue

@Composable
fun NoteCard(
    note: NoteEntity,
    isBookmarked: Boolean,
    onNoteClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onNoteClick() }
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Subject Tag + Bookmark Button + Price Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = note.subject,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = PrimaryBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(32.dp).testTag("bookmark_button_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) PrimaryBlue else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    if (note.isFree) {
                        Surface(
                            color = AccentGreen,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "FREE",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "₹${note.price.toInt()}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = PrimaryBlue,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Author & College Info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${note.authorName} • ${note.college} (${note.semester})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Stats & Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${note.rating}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloads",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${note.downloadsCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (note.isFree) AccentGreen else PrimaryBlue
                    ),
                    modifier = Modifier.testTag("action_button_${note.id}")
                ) {
                    Icon(
                        imageVector = if (note.isFree) Icons.Default.Download else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (note.isFree) "Download PDF" else "Buy Notes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
