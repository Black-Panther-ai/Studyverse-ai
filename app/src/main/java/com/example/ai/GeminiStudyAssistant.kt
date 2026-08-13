package com.example.ai

import com.example.util.NetworkConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiStudyAssistant {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun getFirebaseIdToken(forceRefresh: Boolean = false): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("User is not authenticated with Firebase.")
        val tokenResult = currentUser.getIdToken(forceRefresh).await()
        return tokenResult.token ?: throw IllegalStateException("Failed to obtain Firebase ID token.")
    }

    suspend fun generateAiContent(prompt: String, tool: String): String = withContext(Dispatchers.IO) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isEmpty()) {
            throw IllegalArgumentException("Prompt cannot be empty.")
        }

        val token = getFirebaseIdToken()
        val jsonBody = JSONObject().apply {
            put("prompt", trimmedPrompt)
            put("mode", tool)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val endpointUrl = "${NetworkConfig.normalizeUrl(NetworkConfig.baseUrl)}/api/v1/ai/generate"

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            throw IOException("Network error: Cannot connect to the server. Please check your internet connection.", e)
        }

        val responseString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorJson = try { JSONObject(responseString) } catch (e: Exception) { null }
            val errorMsg = errorJson?.optString("message") 
                ?: when (response.code) {
                    400 -> "Bad Request: Invalid prompt parameters."
                    401 -> "Unauthorized: Please log in again."
                    403 -> "Forbidden: You do not have permission to access the AI assistant."
                    404 -> "AI service endpoint not found."
                    408 -> "AI generation timed out. Please try a shorter request."
                    429 -> "Rate limit exceeded. Please wait a moment before trying again."
                    in 500..599 -> "Server error: Gemini AI service is temporarily unavailable."
                    else -> "HTTP error ${response.code}"
                }
            throw IOException(errorMsg)
        }

        try {
            val jsonRes = JSONObject(responseString)
            if (jsonRes.optBoolean("success", false)) {
                return@withContext jsonRes.getString("content")
            } else {
                val errorMsg = jsonRes.optString("message", "Failed to generate AI response.")
                throw IOException(errorMsg)
            }
        } catch (e: Exception) {
            throw IOException("Failed to parse server AI response: ${e.message}", e)
        }
    }
}
