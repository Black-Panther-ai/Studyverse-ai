package com.example.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object AndroidDownloadManagerHelper {

    private const val TAG = "DownloadManagerHelper"

    fun downloadPdfWithManager(
        context: Context,
        downloadUrl: String,
        title: String,
        fileName: String,
        onComplete: (File?) -> Unit = {}
    ): Long {
        return try {
            val sanitizedFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .let { if (!it.endsWith(".pdf", ignoreCase = true)) "$it.pdf" else it }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(downloadUrl)

            val request = DownloadManager.Request(uri).apply {
                setTitle(title)
                setDescription("Downloading $sanitizedFileName via StudySwap AI...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, sanitizedFileName)
                setMimeType("application/pdf")
                setAllowedOverRoaming(true)
                setAllowedOverMetered(true)
            }

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "Download started for $sanitizedFileName", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Enqueued Download ID: $downloadId for $sanitizedFileName from $downloadUrl")

            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                    if (id == downloadId) {
                        try {
                            recvContext?.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error unregistering receiver: ${e.message}")
                        }

                        val resolvedFile = copyUriToCache(context, downloadManager, downloadId, sanitizedFileName)
                        Log.d(TAG, "Download complete for ID $downloadId. Cache file: ${resolvedFile?.absolutePath}")
                        onComplete(resolvedFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

            downloadId
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager exception: ${e.message}", e)
            Toast.makeText(context, "Failed to start download: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
            onComplete(null)
            -1L
        }
    }

    private fun copyUriToCache(
        context: Context,
        downloadManager: DownloadManager,
        downloadId: Long,
        targetFileName: String
    ): File? {
        return try {
            val pfd = downloadManager.openDownloadedFile(downloadId) ?: return null
            val inputStream = java.io.FileInputStream(pfd.fileDescriptor)
            val cacheFolder = File(context.cacheDir, "study_downloads").apply { mkdirs() }
            val cacheFile = File(cacheFolder, targetFileName)
            
            inputStream.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            pfd.close()
            Log.d(TAG, "Successfully copied download ID $downloadId to cache: ${cacheFile.absolutePath}")
            cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy downloaded file to cache: ${e.message}", e)
            null
        }
    }

    fun sharePdfFile(context: Context, file: File, title: String) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Shared from StudySwap AI: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF Note via"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share PDF: ${e.message}", e)
            Toast.makeText(context, "Could not share file: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
