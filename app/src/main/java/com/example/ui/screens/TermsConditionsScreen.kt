package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
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
fun TermsConditionsScreen(
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
                IconButton(onClick = onBack, modifier = Modifier.testTag("terms_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Gavel, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Terms & Conditions",
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
                .testTag("terms_conditions_screen"),
            contentPadding = PaddingValues(16.dp, bottom = 80.dp)
        ) {
            item {
                Text(
                    text = "Terms of Service Agreement",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryBlue
                )
                Text(
                    text = "Effective Date: July 2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                TermsSection(
                    title = "1. User Accounts & Eligibility",
                    content = """
                        By registering or accessing StudySwap AI, you confirm that you are a student or educational professional. You are responsible for keeping your account login credentials secure and for all activities that occur under your account.
                    """.trimIndent()
                )

                TermsSection(
                    title = "2. Marketplace & Peer Selling Rules",
                    content = """
                        • Sellers must accurately describe physical study items (books, uniforms, shoes, lab equipment, calculators).
                        • Physical transactions take place directly between buyer and seller at college pickup locations. StudySwap AI acts as a directory platform for physical gear listings.
                        • Digital notes purchased via Instamojo are instantly delivered in PDF format with custom watermark headers verifying the buyer's identity.
                    """.trimIndent()
                )

                TermsSection(
                    title = "3. Digital Notes & Copyright Policy",
                    content = """
                        • Uploaded study notes must be original student summaries, lecture notes, or personal revision guides.
                        • Uploading copyrighted textbook scans, official university exam question papers owned by examination boards without authorization, or pirated e-books is strictly forbidden.
                        • Violators are subject to permanent account termination and report to relevant academic institutions.
                    """.trimIndent()
                )

                TermsSection(
                    title = "4. Refund & Payment Rules",
                    content = """
                        • Instamojo payment processing for digital notes is completed securely.
                        • Because digital PDF notes are delivered immediately upon purchase, digital sales are non-refundable unless a corrupted or blank file is proven.
                        • Contact support@studyswap.ai within 48 hours for transaction disputes.
                    """.trimIndent()
                )

                TermsSection(
                    title = "5. Account Suspension & Termination",
                    content = """
                        StudySwap AI reserves the right to suspend or terminate accounts that post inappropriate content, attempt fraudulent payment activities, or harass other student members.
                    """.trimIndent()
                )
            }
        }
    }
}

@Composable
fun TermsSection(title: String, content: String) {
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
