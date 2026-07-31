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
import com.example.data.local.entities.UserEntity
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminPanelScreen(
    viewModel: MainViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val allReports by viewModel.allReports.collectAsState()
    val allPayments by viewModel.allPayments.collectAsState()

    // Protected Route Check
    if (currentUser?.role != "Admin" && currentUser?.userType != "Admin") {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Access Denied", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Red)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Only authorized administrator accounts can access the Admin Panel.", fontSize = 12.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.switchTab(AppTab.HOME) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Return to Home")
                    }
                }
            }
        }
        return
    }

    var adminTab by remember { mutableStateOf("USERS") } // "USERS", "PRODUCTS", "NOTES", "PAYMENTS", "REPORTS"
    var adminSearchQuery by remember { mutableStateOf("") }
    var selectedUserProfile by remember { mutableStateOf<UserEntity?>(null) }

    val totalVolume = remember(allPayments) {
        allPayments.filter { it.status == "SUCCESS" }.sumOf { it.amount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_panel_screen"),
        contentPadding = PaddingValues(16.dp, bottom = 80.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Admin Control Panel",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                text = "System administration, Instamojo transaction logs, user controls, and reports.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Admin Stats Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.12f))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Users", fontSize = 11.sp, color = PrimaryBlue)
                            Text("${allUsers.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.12f))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Instamojo Volume", fontSize = 11.sp, color = AccentGreen)
                            Text("₹${totalVolume.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AccentGreen)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Listings & Notes", fontSize = 11.sp, color = Color.Gray)
                            Text("${allProducts.size + allNotes.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.12f))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Pending Reports", fontSize = 11.sp, color = Color.Red)
                            Text("${allReports.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sub Navigation Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("USERS", "PRODUCTS", "NOTES", "PAYMENTS", "REPORTS").forEach { tab ->
                    val isSelected = adminTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { adminTab = tab },
                        label = { Text(tab, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("admin_subtab_$tab")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = adminSearchQuery,
                onValueChange = { adminSearchQuery = it },
                label = { Text("Search $adminTab...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("admin_search_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        when (adminTab) {
            "USERS" -> {
                val filteredUsers = allUsers.filter {
                    it.name.contains(adminSearchQuery, ignoreCase = true) ||
                    it.email.contains(adminSearchQuery, ignoreCase = true) ||
                    it.collegeName.contains(adminSearchQuery, ignoreCase = true)
                }

                if (filteredUsers.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No users found.")
                            }
                        }
                    }
                } else {
                    items(filteredUsers) { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${user.name} (${user.role})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text("${user.email} • ${user.collegeName}", fontSize = 11.sp, color = Color.Gray)
                                        Text("Registered: ${user.createdDate}", fontSize = 10.sp, color = Color.Gray)
                                    }

                                    Row {
                                        TextButton(onClick = { selectedUserProfile = user }) {
                                            Text("Profile", fontSize = 11.sp)
                                        }

                                        IconButton(onClick = { viewModel.toggleDisableUser(user) }) {
                                            Icon(
                                                if (user.isDisabled) Icons.Default.Block else Icons.Default.CheckCircle,
                                                contentDescription = "Disable",
                                                tint = if (user.isDisabled) Color.Red else AccentGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        if (user.role != "Admin") {
                                            IconButton(onClick = { viewModel.deleteUser(user) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "PRODUCTS" -> {
                val filteredProducts = allProducts.filter {
                    it.title.contains(adminSearchQuery, ignoreCase = true) ||
                    it.category.contains(adminSearchQuery, ignoreCase = true)
                }

                if (filteredProducts.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No marketplace products found.")
                            }
                        }
                    }
                } else {
                    items(filteredProducts) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Seller: ${product.sellerName} • Category: ${product.category}", fontSize = 11.sp, color = Color.Gray)
                                    Text("Price: ₹${product.price.toInt()}", fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                                }

                                IconButton(onClick = { viewModel.deleteProduct(product) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            "NOTES" -> {
                val filteredNotes = allNotes.filter {
                    it.title.contains(adminSearchQuery, ignoreCase = true) ||
                    it.subject.contains(adminSearchQuery, ignoreCase = true)
                }

                if (filteredNotes.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No notes found.")
                            }
                        }
                    }
                } else {
                    items(filteredNotes) { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(note.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Author: ${note.authorName} • Subject: ${note.subject}", fontSize = 11.sp, color = Color.Gray)
                                }

                                IconButton(onClick = { viewModel.deleteNote(note) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            "PAYMENTS" -> {
                val filteredPayments = allPayments.filter {
                    it.itemTitle.contains(adminSearchQuery, ignoreCase = true) ||
                    it.buyerName.contains(adminSearchQuery, ignoreCase = true) ||
                    it.instamojoPaymentId.contains(adminSearchQuery, ignoreCase = true)
                }

                if (filteredPayments.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No Instamojo payment logs found.", fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(filteredPayments) { pay ->
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
                                        Text(pay.status, color = if (pay.status == "SUCCESS") AccentGreen else Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Text("₹${pay.amount.toInt()} via ${pay.gateway}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(pay.itemTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Buyer: ${pay.buyerName} • Payment ID: ${pay.instamojoPaymentId.ifEmpty { pay.id }}", fontSize = 11.sp, color = Color.Gray)
                                Text("Timestamp: $dateStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            "REPORTS" -> {
                if (allReports.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No active content or copyright reports.", fontWeight = FontWeight.Bold, color = AccentGreen)
                            }
                        }
                    }
                } else {
                    items(allReports) { report ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Report: ${report.itemTitle}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Red)
                                    Text("Reporter: ${report.reporterName} • Reason: ${report.reason}", fontSize = 11.sp, color = Color.DarkGray)
                                }

                                IconButton(onClick = { viewModel.deleteReport(report) }) {
                                    Icon(Icons.Default.Check, contentDescription = "Resolve", tint = AccentGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedUserProfile?.let { user ->
        AlertDialog(
            onDismissRequest = { selectedUserProfile = null },
            title = { Text("User Profile Details", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Name: ${user.name}", fontWeight = FontWeight.Bold)
                    Text("Email: ${user.email}")
                    Text("College: ${user.collegeName}")
                    Text("Course: ${user.course}")
                    Text("Semester: ${user.semester}")
                    Text("State: ${user.state}")
                    Text("Phone: ${user.phoneWhatsApp}")
                    Text("Role: ${user.role}")
                    Text("Created Date: ${user.createdDate}")
                    Text("Status: ${if (user.isDisabled) "Disabled" else "Active"}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedUserProfile = null }) {
                    Text("Close")
                }
            }
        )
    }
}
