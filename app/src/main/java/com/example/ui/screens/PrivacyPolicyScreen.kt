package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryBlue

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("privacy_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Privacy Policy",
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
                .testTag("privacy_policy_screen"),
            contentPadding = PaddingValues(16.dp, bottom = 80.dp)
        ) {
            item {
                Text(
                    text = "StudySwap AI Privacy Policy",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryBlue
                )
                Text(
                    text = "Last updated: July 2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                PolicySection(
                    title = "1. Information We Collect",
                    content = """
                        We collect information you provide directly to us when creating an account, uploading study materials, or communicating with sellers:
                        • Account Information: Name, Email address, College/University, Course, Semester, State, Mobile/WhatsApp Number.
                        • Uploaded Files: Digital notes, sample previews, PDF documents, and product photos.
                        • Marketplace Listings: Product title, category, description, price, condition, pickup location.
                        • Payment Information: Transaction metadata processed securely via Instamojo payment gateway. We do not store full credit card or UPI PINs.
                        • Technical Data: Device IP address, app operating system logs, and anonymous analytics.
                    """.trimIndent()
                )

                PolicySection(
                    title = "2. How We Use Data",
                    content = """
                        We use the information we collect to:
                        • Facilitate student-to-student marketplace transactions and digital note sharing.
                        • Authenticate users and prevent unauthorized access or spam accounts.
                        • Watermark downloaded PDFs with student verification details to protect author copyright.
                        • Process Instamojo payments and generate digital receipts.
                        • Provide Gemini AI study assistance features.
                    """.trimIndent()
                )

                PolicySection(
                    title = "3. Data Security",
                    content = """
                        We implement administrative, technical, and physical safeguards designed to protect your personal data against accidental, unlawful, or unauthorized destruction, loss, alteration, access, disclosure, or use. All communications are encrypted over HTTPS/TLS protocols.
                    """.trimIndent()
                )

                PolicySection(
                    title = "4. Content Ownership & Copyright",
                    content = """
                        • Students retain original copyright over the handwritten and typed notes they author.
                        • By uploading digital notes to StudySwap AI, authors grant a limited license to distribute watermarked copies to verified buyers/downloaders.
                        • Copyright infringement, plagiarized textbooks, or unauthorized distribution of proprietary publisher materials is strictly prohibited and leads to immediate account ban.
                    """.trimIndent()
                )

                PolicySection(
                    title = "5. User Rights & Contact Information",
                    content = """
                        You have the right to access, update, or request deletion of your personal account data at any time through the Profile settings.
                        For privacy inquiries, copyright complaints, or account support, contact us at: support@studyswap.ai
                    """.trimIndent()
                )
            }
        }
    }
}

@Composable
fun PolicySection(title: String, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}
