package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfDownloadHelper {

    fun generateAndSavePdf(
        context: Context,
        noteTitle: String,
        buyerName: String,
        orderId: String,
        authorName: String
    ): File? {
        return try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir

            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val sanitizedTitle = noteTitle.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val pdfFile = File(downloadsDir, "${sanitizedTitle}_Watermarked.pdf")

            val pdfContent = """
                %PDF-1.4
                1 0 obj
                << /Type /Catalog /Pages 2 0 R >>
                endobj
                2 0 obj
                << /Type /Pages /Kids [3 0 R] /Count 1 >>
                endobj
                3 0 obj
                << /Type /Page /Parent 2 0 R /Resources << /Font << /F1 4 0 R >> >> /MediaBox [0 0 612 792] /Contents 5 0 R >>
                endobj
                4 0 obj
                << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
                endobj
                5 0 obj
                << /Length 200 >>
                stream
                BT
                /F1 18 Tf
                50 720 Td
                ($noteTitle) Tj
                /F1 12 Tf
                0 -30 Td
                (Author: $authorName) Tj
                0 -20 Td
                (Licensed to: $buyerName) Tj
                0 -20 Td
                (Order ID: $orderId) Tj
                0 -40 Td
                (STUDYSWAP AI VERIFIED DIGITAL NOTES) Tj
                0 -20 Td
                (Watermarked for Copyright & Personal Academic Use) Tj
                ET
                endstream
                endobj
                xref
                0 6
                0000000000 65535 f 
                0000000009 00000 n 
                0000000058 00000 n 
                0000000115 00000 n 
                0000000242 00000 n 
                0000000315 00000 n 
                trailer
                << /Size 6 /Root 1 0 R >>
                startxref
                570
                %%EOF
            """.trimIndent()

            FileOutputStream(pdfFile).use { fos ->
                fos.write(pdfContent.toByteArray(Charsets.UTF_8))
            }

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openPdfFile(context: Context, pdfFile: File) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            Toast.makeText(context, "Error: PDF file does not exist or is corrupted.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: If no PDF reader app is installed, show Toast
            Toast.makeText(context, "PDF saved at: ${pdfFile.absolutePath}. Install a PDF viewer to open directly.", Toast.LENGTH_LONG).show()
        }
    }
}
