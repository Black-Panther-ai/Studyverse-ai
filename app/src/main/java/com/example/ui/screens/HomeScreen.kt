package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.ProductEntity
import com.example.ui.components.AdBanner
import com.example.ui.components.AdNative
import com.example.ui.components.AppFooter
import com.example.ui.components.CategoryChip
import com.example.ui.components.NoteCard
import com.example.ui.components.ProductCard
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToTab: (AppTab) -> Unit,
    onNoteSelect: (NoteEntity) -> Unit,
    onProductSelect: (ProductEntity) -> Unit
) {
    val freeNotes by viewModel.freeNotes.collectAsState()
    val digitalStore by viewModel.digitalNotesStore.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val myBookmarks by viewModel.myBookmarks.collectAsState()
    val bookmarkIds = remember(myBookmarks) { myBookmarks.map { it.itemId }.toSet() }

    val categories = listOf(
        "All", "Books", "Handwritten Notes", "Digital Notes", "Previous Year Papers",
        "Calculators", "Lab Coat", "Uniform", "Shoes", "Stationery", "Backpack", "Electronics"
    )

    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. HERO BENTO GRID CARD
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("bento_hero_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF0F172A))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "StudySwap AI • Peer Academic Hub",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Learn More. Spend Less.\nEarn From Your Knowledge.",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    lineHeight = 30.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Buy second-hand study products, download topper notes & prep for exams with Gemini AI.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    viewModel.searchQuery.value = it
                                },
                                placeholder = { Text("Search books, notes, PYQs, lab coats...", color = Color.Gray, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .testTag("home_search_bar"),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bento Action Cards (2 Columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToTab(AppTab.MARKETPLACE) }
                            .testTag("bento_action_marketplace"),
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Marketplace", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = PrimaryBlue)
                            Text("Books, lab coats, calculators", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToTab(AppTab.AI_ASSISTANT) }
                            .testTag("bento_action_ai_assistant"),
                        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Gemini AI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = AccentGreen)
                            Text("Summarizer, Quiz, Revision", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // CATEGORY CHIPS
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(categories) { category ->
                    CategoryChip(
                        label = category,
                        isSelected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        // 2. FREE NOTES SECTION
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Free Handwritten Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = { onNavigateToTab(AppTab.FREE_NOTES) }) {
                    Text("View All", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (freeNotes.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text("No notes uploaded", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            items(freeNotes.take(2)) { note ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    NoteCard(
                        note = note,
                        isBookmarked = bookmarkIds.contains(note.id),
                        onNoteClick = { onNoteSelect(note) },
                        onBookmarkToggle = { viewModel.toggleBookmark(note.id, note.title, "NOTE", note.subject, note.price) },
                        onActionClick = { viewModel.downloadFreeNote(note) }
                    )
                }
            }
        }

        // 3. ADSTERRA BANNER AD
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AdBanner()
            }
        }

        // 4. DIGITAL STORE SECTION
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Original Notes Store",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = { onNavigateToTab(AppTab.DIGITAL_STORE) }) {
                    Text("View Store", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (digitalStore.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text("No notes uploaded", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            items(digitalStore.take(2)) { note ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    NoteCard(
                        note = note,
                        isBookmarked = bookmarkIds.contains(note.id),
                        onNoteClick = { onNoteSelect(note) },
                        onBookmarkToggle = { viewModel.toggleBookmark(note.id, note.title, "NOTE", note.subject, note.price) },
                        onActionClick = { viewModel.purchaseDigitalNote(note) }
                    )
                }
            }
        }

        // 5. NATIVE AD
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                AdNative()
            }
        }

        // 6. PHYSICAL MARKETPLACE
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Student Marketplace",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = { onNavigateToTab(AppTab.MARKETPLACE) }) {
                    Text("Browse All", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (products.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text("No products available", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            items(products.take(3)) { product ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ProductCard(
                        product = product,
                        onProductClick = { onProductSelect(product) }
                    )
                }
            }
        }

        // 7. FOOTER
        item {
            AppFooter()
        }
    }
}
