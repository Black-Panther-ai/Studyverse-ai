package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.PrimaryBlue
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppPdfViewerDialog(
    pdfFile: File,
    title: String,
    onDismiss: () -> Unit
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1.0f) }
    var isDarkModeInverted by remember { mutableStateOf(false) }

    // Load PDF Renderer
    DisposableEffect(pdfFile) {
        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null

        try {
            if (pdfFile.exists() && pdfFile.length() > 0) {
                fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor)
                pageCount = pdfRenderer.pageCount
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Render Page on demand
    LaunchedEffect(pdfFile, pageIndex) {
        if (!pdfFile.exists() || pageCount == 0) return@LaunchedEffect

        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            if (pageIndex in 0 until renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                // Render at high resolution (2x screen density scale)
                val width = page.width * 2
                val height = page.height * 2
                val pageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap = pageBitmap
                page.close()
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("in_app_pdf_viewer_dialog"),
            color = if (isDarkModeInverted) Color(0xFF121212) else Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Surface(
                    color = PrimaryBlue,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("pdf_viewer_close")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = if (pageCount > 0) "Page ${pageIndex + 1} of $pageCount" else "Loading document...",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // Dark Mode Reading Toggle
                        IconButton(onClick = { isDarkModeInverted = !isDarkModeInverted }) {
                            Icon(
                                imageVector = if (isDarkModeInverted) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Reader Theme",
                                tint = Color.White
                            )
                        }

                        // Zoom Out Button
                        IconButton(onClick = { scale = (scale - 0.25f).coerceAtLeast(1.0f) }) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                        }

                        // Zoom In Button
                        IconButton(onClick = { scale = (scale + 0.25f).coerceAtMost(3.0f) }) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                        }
                    }
                }

                // PDF Page Canvas View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(if (isDarkModeInverted) Color(0xFF1E1E1E) else Color(0xFFE2E8F0))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1.0f, 3.0f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val currentBitmap = bitmap
                    if (currentBitmap != null) {
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = "PDF Page ${pageIndex + 1}",
                            modifier = Modifier
                                .fillMaxWidth(fraction = scale.coerceAtLeast(1.0f))
                                .wrapContentHeight(),
                            colorFilter = if (isDarkModeInverted) ColorFilter.colorMatrix(
                                ColorMatrix(
                                    floatArrayOf(
                                        -1f, 0f, 0f, 0f, 255f,
                                        0f, -1f, 0f, 0f, 255f,
                                        0f, 0f, -1f, 0f, 255f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            ) else null
                        )
                    } else {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }

                // Bottom Page Control Navigation Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (pageIndex > 0) pageIndex-- },
                            enabled = pageIndex > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = CircleShape
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev", fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "${pageIndex + 1} / $pageCount",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Button(
                            onClick = { if (pageIndex < pageCount - 1) pageIndex++ },
                            enabled = pageIndex < pageCount - 1,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = CircleShape
                        ) {
                            Text("Next", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                        }
                    }
                }
            }
        }
    }
}
