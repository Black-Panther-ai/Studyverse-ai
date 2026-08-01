package com.example.data.repository

import com.example.util.NetworkConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RazorpayPaymentRepository(
    private val baseUrl: String = NetworkConfig.baseUrl,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private suspend fun getFirebaseIdToken(forceRefresh: Boolean = false): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("User is not authenticated.")
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
            val errorMsg = errorJson?.optString("message") ?: "API request failed (HTTP ${response.code})"
            throw IllegalStateException(errorMsg)
        }

        return responseText
    }

    data class CreateOrderResponse(
        val internalOrderId: String,
        val razorpayOrderId: String,
        val razorpayKeyId: String,
        val amountPaise: Long,
        val currency: String,
        val listingTitle: String
    )

    suspend fun createOrder(listingId: String): CreateOrderResponse {
        val jsonBody = JSONObject().apply { put("listingId", listingId) }
        val endpointUrl = "${NetworkConfig.normalizeUrl(baseUrl)}/api/v1/payments/create-order"
        val responseText = executeProtectedRequest(endpointUrl, jsonBody)

        val json = JSONObject(responseText)
        return CreateOrderResponse(
            internalOrderId = json.getString("internalOrderId"),
            razorpayOrderId = json.getString("razorpayOrderId"),
            razorpayKeyId = json.getString("razorpayKeyId"),
            amountPaise = json.getLong("amountPaise"),
            currency = json.getString("currency"),
            listingTitle = json.getString("listingTitle")
        )
    }

    data class VerifyPaymentResponse(
        val status: String,
        val message: String
    )

    suspend fun verifyPayment(
        internalOrderId: String,
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String
    ): VerifyPaymentResponse {
        val jsonBody = JSONObject().apply {
            put("internalOrderId", internalOrderId)
            put("razorpayOrderId", razorpayOrderId)
            put("razorpayPaymentId", razorpayPaymentId)
            put("razorpaySignature", razorpaySignature)
        }

        val endpointUrl = "${NetworkConfig.normalizeUrl(baseUrl)}/api/v1/payments/verify"
        val responseText = executeProtectedRequest(endpointUrl, jsonBody)

        val json = JSONObject(responseText)
        return VerifyPaymentResponse(
            status = json.getString("status"),
            message = json.optString("message", "Payment verified.")
        )
    }
}
