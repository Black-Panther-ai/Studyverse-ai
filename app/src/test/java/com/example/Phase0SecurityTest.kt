package com.example

import com.example.payment.InstamojoPaymentHelper
import com.example.payment.InstamojoResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase0SecurityTest {

    @Test
    fun testInstamojoCredentialsAreRemoved() {
        assertEquals("", InstamojoPaymentHelper.apiKey)
        assertEquals("", InstamojoPaymentHelper.authToken)
    }

    @Test
    fun testPaymentHelperReturnsNotConfiguredStatus() = runBlocking {
        val result = InstamojoPaymentHelper.createAndProcessPayment(
            context = null,
            buyerName = "Test Buyer",
            buyerEmail = "test@example.com",
            buyerPhone = "9876543210",
            amount = 100.0,
            noteTitle = "Test Note",
            noteId = "note_123"
        )
        assertTrue(result is InstamojoResult.Failure)
        val failure = result as InstamojoResult.Failure
        assertEquals("Payments are not configured yet.", failure.reason)
    }
}
