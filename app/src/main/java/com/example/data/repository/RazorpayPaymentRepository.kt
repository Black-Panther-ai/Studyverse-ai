package com.example.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RazorpayPaymentRepository(
    private val baseUrl: String = "http://10.0.2.2:8080",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private suspend fun getFirebaseIdToken(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("User is not authenticated.")
        val tokenResult = currentUser.getIdToken(true).await()
        return tokenResult.token ?: throw IllegalStateException("Failed to obtain Firebase ID token.")
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
        val idToken = getFirebaseIdToken()
        val jsonBody = JSONObject().apply { put("listingId", listingId) }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$baseUrl/api/v1/payments/create-order")
            .addHeader("Authorization", "Bearer $idToken")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseText = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorJson = try { JSONObject(responseText) } catch (e: Exception) { null }
            val errorMsg = errorJson?.optString("message") ?: "Failed to initialize payment order (HTTP ${response.code})"
            throw IllegalStateException(errorMsg)
        }

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
        val idToken = getFirebaseIdToken()
        val jsonBody = JSONObject().apply {
            put("internalOrderId", internalOrderId)
            put("razorpayOrderId", razorpayOrderId)
            put("razorpayPaymentId", razorpayPaymentId)
            put("razorpaySignature", razorpaySignature)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$baseUrl/api/v1/payments/verify")
            .addHeader("Authorization", "Bearer $idToken")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseText = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorJson = try { JSONObject(responseText) } catch (e: Exception) { null }
            val errorMsg = errorJson?.optString("message") ?: "Payment verification failed (HTTP ${response.code})"
            throw IllegalStateException(errorMsg)
        }

        val json = JSONObject(responseText)
        return VerifyPaymentResponse(
            status = json.getString("status"),
            message = json.optString("message", "Payment verified.")
        )
    }
}
