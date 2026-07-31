package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryBlue

@Composable
fun WatermarkDialog(
    noteTitle: String,
    buyerName: String,
    orderId: String,
    purchaseDate: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("watermark_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Licensed Digital Download",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = noteTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(12.dp))

                // PDF Preview Card with Watermark Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFCBD5E1), shape = RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Page Dummy Content
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "PAGE 1 / 45 - $noteTitle",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Introduction to Core Concepts\nHandwritten notes verified for university semester exams. All rights reserved by StudySwap AI.",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }

                    // Diagonal Watermark Box
                    Box(
                        modifier = Modifier
                            .rotate(-20f)
                            .background(Color(0xCC000000), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LICENSED TO: $buyerName",
                                color = Color.Yellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "ORDER: $orderId | $purchaseDate",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "STUDYSWAP AI COPYRIGHT PROTECTED",
                                color = Color.Green,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "🔒 Security Note: A personalized watermark containing your name, order ID, and timestamp is embedded into every page to prevent unauthorized redistribution.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    val pdfFile = com.example.util.PdfDownloadHelper.generateAndSavePdf(
                        context = context,
                        noteTitle = noteTitle,
                        buyerName = buyerName,
                        orderId = orderId,
                        authorName = "Verified Note Author"
                    )
                    if (pdfFile != null && pdfFile.exists()) {
                        com.example.util.PdfDownloadHelper.openPdfFile(context, pdfFile)
                        onDownload()
                    } else {
                        android.widget.Toast.makeText(context, "Failed to generate PDF file.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.testTag("download_watermarked_pdf_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save & Open PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
