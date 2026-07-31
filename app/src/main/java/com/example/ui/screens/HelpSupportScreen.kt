package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue

@Composable
fun HelpSupportScreen(
    onBack: () -> Unit = {}
) {
    var queryText by remember { mutableStateOf("") }
    var querySubmitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("help_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Help & Support",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("help_support_screen"),
            contentPadding = PaddingValues(16.dp, bottom = 80.dp)
        ) {
            item {
                Text(
                    text = "How can we help you today?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Find answers to common student questions or send us a support ticket.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("24/7 Student Support Desk", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Email: support@studyswap.ai • WhatsApp: +91 98765 43210", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Frequently Asked Questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                FaqCard("How do digital PDF note downloads work?", "Once you purchase a digital note via Instamojo, it is instantly added to your My Downloads tab. Your PDF is automatically watermarked with your name and order ID for copyright protection.")
                FaqCard("How do physical item transactions work?", "Sellers list physical items like uniforms, books, and lab coats along with their campus pickup location and WhatsApp contact. Buyers contact sellers directly to arrange physical inspection and pickup.")
                FaqCard("What if a downloaded PDF is unreadable?", "If you encounter any file issues, submit a ticket below or email support@studyswap.ai with your Order ID for a replacement file or refund.")

                Spacer(modifier = Modifier.height(20.dp))

                Text("Send Support Ticket", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    label = { Text("Describe your query or issue...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("support_ticket_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (queryText.isNotBlank()) {
                            querySubmitted = true
                            queryText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("submit_ticket_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Submit Ticket", fontWeight = FontWeight.Bold)
                }

                if (querySubmitted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f))) {
                        Text(
                            text = "Thank you! Your support ticket has been received. Our team will contact you within 24 hours.",
                            color = AccentGreen,
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FaqCard(question: String, answer: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(question, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
            Spacer(modifier = Modifier.height(4.dp))
            Text(answer, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
