package com.example.payment

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class InstamojoResult {
    data class Success(
        val paymentId: String,
        val paymentRequestId: String,
        val amount: Double,
        val status: String = "SUCCESS"
    ) : InstamojoResult()

    data class Failure(
        val reason: String
    ) : InstamojoResult()
}

object InstamojoPaymentHelper {
    private const val TAG = "PaymentGateway"

    val apiKey: String = ""
    val authToken: String = ""
    val saltKey: String = ""

    fun isConfigured(): Boolean {
        return false
    }

    /**
     * Phase 0 Security Containment: Client-side payment gateway execution is disabled.
     * Payments will be handled via server-side Razorpay integration in a future phase.
     */
    suspend fun createAndProcessPayment(
        context: Context?,
        buyerName: String,
        buyerEmail: String,
        buyerPhone: String,
        amount: Double,
        noteTitle: String,
        noteId: String,
        simulateOutcome: Boolean = false
    ): InstamojoResult = withContext(Dispatchers.IO) {
        Log.w(TAG, "Payment attempt blocked: Client-side payments are disabled.")
        InstamojoResult.Failure("Payments are not configured yet.")
    }
}
