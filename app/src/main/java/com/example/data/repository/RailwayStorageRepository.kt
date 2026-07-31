package com.example.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit

class RailwayStorageRepository(
    private val baseUrl: String = "http://10.0.2.2:8080", // Default emulator localhost URL
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    private suspend fun getFirebaseIdToken(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("User is not authenticated with Firebase.")
        val tokenResult = currentUser.getIdToken(true).await()
        return tokenResult.token ?: throw IllegalStateException("Failed to obtain Firebase ID token.")
    }

    data class PresignedUploadResponse(
        val uploadUrl: String,
        val objectKey: String,
        val expiresInSeconds: Int,
        val requiredContentType: String
    )

    suspend fun requestPresignedUploadUrl(
        listingId: String,
        fileCategory: String, // "listing_image" or "digital_pdf"
        contentType: String,
        fileSizeBytes: Long
    ): PresignedUploadResponse {
        val idToken = getFirebaseIdToken()

        val jsonBody = JSONObject().apply {
            put("listingId", listingId)
            put("fileCategory", fileCategory)
            put("contentType", contentType)
            put("fileSizeBytes", fileSizeBytes)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$baseUrl/api/v1/storage/presign-upload")
            .addHeader("Authorization", "Bearer $idToken")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseText = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorJson = try { JSONObject(responseText) } catch (e: Exception) { null }
            val errorMsg = errorJson?.optString("message") ?: "HTTP ${response.code}: Upload presign failed"
            throw IllegalStateException(errorMsg)
        }

        val json = JSONObject(responseText)
        val headersJson = json.optJSONObject("requiredHeaders")
        val reqContentType = headersJson?.optString("Content-Type") ?: contentType

        return PresignedUploadResponse(
            uploadUrl = json.getString("uploadUrl"),
            objectKey = json.getString("objectKey"),
            expiresInSeconds = json.getInt("expiresInSeconds"),
            requiredContentType = reqContentType
        )
    }

    suspend fun uploadFileDirectlyToS3(
        uploadUrl: String,
        inputStream: InputStream,
        contentType: String,
        contentLength: Long
    ): Boolean {
        val bytes = inputStream.readBytes()
        val requestBody = bytes.toRequestBody(contentType.toMediaTypeOrNull())

        val request = Request.Builder()
            .url(uploadUrl)
            .put(requestBody)
            .addHeader("Content-Type", contentType)
            .build()

        val response = client.newCall(request).execute()
        return response.isSuccessful
    }

    suspend fun fetchPublicImageSignedUrls(objectKeys: List<String>): Map<String, String> {
        if (objectKeys.isEmpty()) return emptyMap()

        val jsonArray = JSONArray()
        objectKeys.forEach { jsonArray.put(it) }
        val jsonBody = JSONObject().apply { put("objectKeys", jsonArray) }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$baseUrl/api/v1/storage/image-urls")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseText = response.body?.string() ?: ""

        if (!response.isSuccessful) return emptyMap()

        val json = JSONObject(responseText)
        val urlsJson = json.optJSONObject("urls") ?: return emptyMap()

        val resultMap = mutableMapOf<String, String>()
        urlsJson.keys().forEach { key ->
            resultMap[key] = urlsJson.getString(key)
        }
        return resultMap
    }

    suspend fun requestPrivateDownloadUrl(listingId: String, orderId: String): String {
        val idToken = getFirebaseIdToken()

        val jsonBody = JSONObject().apply {
            put("listingId", listingId)
            put("orderId", orderId)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$baseUrl/api/v1/storage/private-download-url")
            .addHeader("Authorization", "Bearer $idToken")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseText = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorJson = try { JSONObject(responseText) } catch (e: Exception) { null }
            val errorMsg = errorJson?.optString("message") ?: "Failed to generate private download link."
            throw IllegalStateException(errorMsg)
        }

        val json = JSONObject(responseText)
        return json.getString("downloadUrl")
    }
}
