package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.ui.components.WatermarkDialog
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BuyerDashboardScreen(
    viewModel: MainViewModel
) {
    val myOrders by viewModel.myOrders.collectAsState()
    val syncState by viewModel.dashboardSyncState.collectAsState()
    val myPayments by viewModel.myPayments.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var activeTab by remember { mutableStateOf("Purchases") } // "Purchases", "Downloads", "PaymentHistory"
    var searchQuery by remember { mutableStateOf("") }
    var showWatermarkForOrder by remember { mutableStateOf<com.example.data.local.entities.OrderEntity?>(null) }

    val myPurchases = remember(myOrders) { myOrders.filter { it.price > 0 } }
    val myDownloads = myOrders

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("buyer_dashboard_screen"),
        contentPadding = PaddingValues(16.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Buyer Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Manage your purchases, instant downloads, and payments.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stat Summary Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("My Total Purchases", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        Text("${myPurchases.size} Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("Total Downloads", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        Text("${myDownloads.size} Items", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Razorpay", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeTab == "Purchases",
                    onClick = { activeTab = "Purchases" },
                    label = { Text("My Purchases (${myPurchases.size})", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = activeTab == "Downloads",
                    onClick = { activeTab = "Downloads" },
                    label = { Text("My Downloads (${myDownloads.size})", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = activeTab == "PaymentHistory",
                    onClick = { activeTab = "PaymentHistory" },
                    label = { Text("Payments (${myPayments.size})", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search dashboard...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 1. Loading State
        if (syncState == com.example.ui.viewmodel.DashboardSyncState.LOADING) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
        }

        // 2. Retry State
        if (syncState == com.example.ui.viewmodel.DashboardSyncState.ERROR) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error synchronizing purchases from Firestore.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.syncPurchasesFromFirestore() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Retry Sync", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (activeTab == "Purchases") {
            val filtered = myPurchases.filter { it.itemTitle.contains(searchQuery, ignoreCase = true) }
            if (filtered.isEmpty()) {
                if (syncState == com.example.ui.viewmodel.DashboardSyncState.SUCCESS) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Purchases Yet", fontWeight = FontWeight.Bold)
                                Text("Premium digital notes purchased will appear here.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            } else {
                items(filtered) { order ->
                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(order.timestamp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Surface(color = PrimaryBlue.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                                    Text("PAID DIGITAL NOTE", color = PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Text("₹${order.price.toInt()}", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(order.itemTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Order ID: ${order.id} • $dateStr", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showWatermarkForOrder = order },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download Note PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else if (activeTab == "Downloads") {
            val filtered = myDownloads.filter { it.itemTitle.contains(searchQuery, ignoreCase = true) }
            if (filtered.isEmpty()) {
                if (syncState == com.example.ui.viewmodel.DashboardSyncState.SUCCESS) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Downloads Yet", fontWeight = FontWeight.Bold)
                                Text("All downloaded free notes and paid materials will appear here.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            } else {
                items(filtered) { order ->
                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(order.timestamp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Surface(
                                    color = if (order.price > 0) PrimaryBlue.copy(alpha = 0.12f) else AccentGreen.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        if (order.price > 0) "PURCHASED NOTE" else "FREE NOTE",
                                        color = if (order.price > 0) PrimaryBlue else AccentGreen,
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(dateStr, fontSize = 11.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(order.itemTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showWatermarkForOrder = order },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download Again (PDF)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Payment History Tab
            val filtered = myPayments.filter { it.itemTitle.contains(searchQuery, ignoreCase = true) }
            if (filtered.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Payment Logs", fontWeight = FontWeight.Bold)
                            Text("All transaction logs (Success / Failed) will appear here.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(filtered) { pay ->
                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(pay.timestamp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Surface(
                                    color = if (pay.status == "SUCCESS") AccentGreen.copy(alpha = 0.12f) else Color.Red.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        pay.status,
                                        color = if (pay.status == "SUCCESS") AccentGreen else Color.Red,
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text("₹${pay.amount.toInt()} via ${pay.gateway}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(pay.itemTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Payment ID: ${pay.instamojoPaymentId.ifEmpty { pay.id }} • $dateStr", fontSize = 10.sp, color = Color.Gray)
                            if (pay.failureReason.isNotBlank()) {
                                Text("Reason: ${pay.failureReason}", fontSize = 10.sp, color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    showWatermarkForOrder?.let { order ->
        val dateStr = remember(order) { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(order.timestamp)) }
        WatermarkDialog(
            noteTitle = order.itemTitle,
            buyerName = currentUser?.name ?: "Student User",
            orderId = order.id,
            purchaseDate = dateStr,
            onDismiss = { showWatermarkForOrder = null },
            onDownload = {
                viewModel.uiMessage.value = "Watermarked S3 PDF Download Complete!"
            },
            onDownloadPdf = { context, onComplete ->
                viewModel.downloadPurchasedNote(
                    context = context,
                    orderId = order.id,
                    listingId = order.itemId,
                    noteTitle = order.itemTitle,
                    onComplete = onComplete
                )
            }
        )
    }
}
