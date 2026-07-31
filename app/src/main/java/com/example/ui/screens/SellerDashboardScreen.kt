package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SellerDashboardScreen(
    viewModel: MainViewModel,
    onOpenUploadModal: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()

    var selectedTab by remember { mutableStateOf("Listings") } // "Listings", "PremiumNotes", "FreeNotes", "Orders"

    val myProducts = remember(allProducts, currentUser) {
        allProducts.filter { it.sellerId == currentUser?.id }
    }

    val myNotes = remember(allNotes, currentUser) {
        allNotes.filter { it.authorId == currentUser?.id }
    }

    val myPremiumNotes = remember(myNotes) { myNotes.filter { !it.isFree } }
    val myFreeNotes = remember(myNotes) { myNotes.filter { it.isFree } }

    val myOrdersReceived = remember(allOrders, currentUser) {
        allOrders.filter { it.sellerId == currentUser?.id }
    }

    val totalRevenue = remember(myOrdersReceived) {
        myOrdersReceived.sumOf { it.price }
    }

    val totalDownloadsCount = remember(myNotes) {
        myNotes.sumOf { it.downloadsCount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("seller_dashboard_screen"),
        contentPadding = PaddingValues(16.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Seller & Author Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Monitor revenue, note downloads, active listings, and orders received.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Revenue and Stat Overview Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Estimated Revenue", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text("₹${totalRevenue.toInt()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }

                        Button(
                            onClick = onOpenUploadModal,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.testTag("seller_upload_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("List / Upload", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Published Notes", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                            Text("${myNotes.size} (${myPremiumNotes.size} Paid / ${myFreeNotes.size} Free)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Total Downloads", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                            Text("$totalDownloadsCount Downloads", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Items Listed", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                            Text("${myProducts.size} Products", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedTab == "Listings",
                    onClick = { selectedTab = "Listings" },
                    label = { Text("Items (${myProducts.size})", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTab == "PremiumNotes",
                    onClick = { selectedTab = "PremiumNotes" },
                    label = { Text("Premium (${myPremiumNotes.size})", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTab == "FreeNotes",
                    onClick = { selectedTab = "FreeNotes" },
                    label = { Text("Free (${myFreeNotes.size})", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTab == "Orders",
                    onClick = { selectedTab = "Orders" },
                    label = { Text("Sales (${myOrdersReceived.size})", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (selectedTab == "Listings") {
            if (myProducts.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No physical campus products listed yet. Click List / Upload to add one.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(myProducts) { product ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(product.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Surface(
                                    color = if (product.isSold) Color.Red.copy(alpha = 0.12f) else AccentGreen.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        if (product.isSold) "SOLD" else "ACTIVE",
                                        color = if (product.isSold) Color.Red else AccentGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text("Category: ${product.category} • Price: ₹${product.price.toInt()} • Condition: ${product.condition}", fontSize = 11.sp, color = Color.Gray)
                            Text("Pickup Location: ${product.pickupLocation}, ${product.city}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Views: ${product.viewsCount}", fontSize = 11.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!product.isSold) {
                                        TextButton(onClick = { viewModel.markProductAsSold(product) }) {
                                            Text("Mark Sold", fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteProduct(product) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == "PremiumNotes") {
            if (myPremiumNotes.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No premium notes uploaded yet.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(myPremiumNotes) { note ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Subject: ${note.subject} • Price: ₹${note.price.toInt()}", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                Text("Downloads / Sales: ${note.downloadsCount} purchases", fontSize = 10.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { viewModel.deleteNote(note) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == "FreeNotes") {
            if (myFreeNotes.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No free shared notes uploaded yet.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(myFreeNotes) { note ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Subject: ${note.subject} • Downloads: ${note.downloadsCount}", fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.deleteNote(note) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        } else {
            // Sales Orders Received
            if (myOrdersReceived.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No orders received yet.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(myOrdersReceived) { order ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(order.itemTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("₹${order.price.toInt()}", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                            Text("Buyer: ${order.buyerName} • Status: ${order.status}", fontSize = 11.sp, color = Color.Gray)
                            Text("Order ID: ${order.id}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
