package com.example.data.repository

import com.example.util.NetworkConfig
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
    private val baseUrl: String = NetworkConfig.baseUrl,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    private suspend fun getFirebaseIdToken(forceRefresh: Boolean = false): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("User is not authenticated with Firebase.")
        val tokenResult = currentUser.getIdToken(forceRefresh).await()
        return tokenResult.token ?: throw IllegalStateException("Failed to obtain Firebase ID token.")
    }

    private suspend fun executeProtectedRequest(
        url: String,
        jsonBody: JSONObject
    ): String {
        var idToken = getFirebaseIdToken(forceRefresh = false)

        val makeRequest = { token: String ->
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()
        }

        var response = client.newCall(makeRequest(idToken)).execute()

        // Requirement 7: Refreshes token and retries once after HTTP 401
        if (response.code == 401) {
            response.close()
            idToken = getFirebaseIdToken(forceRefresh = true)
            response = client.newCall(makeRequest(idToken)).execute()
        }

        val responseText = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorJson = try { JSONObject(responseText) } catch (e: Exception) { null }
            val errorMsg = errorJson?.optString("message") ?: "HTTP ${response.code}: API request failed"
            throw IllegalStateException(errorMsg)
        }

        return responseText
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
        val jsonBody = JSONObject().apply {
            put("listingId", listingId)
            put("fileCategory", fileCategory)
            put("contentType", contentType)
            put("fileSizeBytes", fileSizeBytes)
        }

        val endpointUrl = "${NetworkConfig.normalizeUrl(baseUrl)}/api/v1/storage/presign-upload"
        val responseText = executeProtectedRequest(endpointUrl, jsonBody)

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
        val endpointUrl = "${NetworkConfig.normalizeUrl(baseUrl)}/api/v1/storage/image-urls"
        val request = Request.Builder()
            .url(endpointUrl)
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
        val jsonBody = JSONObject().apply {
            put("listingId", listingId)
            put("orderId", orderId)
        }

        val endpointUrl = "${NetworkConfig.normalizeUrl(baseUrl)}/api/v1/storage/private-download-url"
        val responseText = executeProtectedRequest(endpointUrl, jsonBody)

        val json = JSONObject(responseText)
        return json.getString("downloadUrl")
    }
}
