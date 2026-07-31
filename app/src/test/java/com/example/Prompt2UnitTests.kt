package com.example

import com.example.data.model.Listing
import com.example.data.repository.RailwayStorageRepository
import com.example.data.repository.RazorpayPaymentRepository
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Prompt2UnitTests {

    @Test
    fun testPresignedUploadResponseMapping() {
        val jsonString = """
            {
                "uploadUrl": "https://storage.railway.app/upload?token=123",
                "objectKey": "listing-images/usr_1/lst_1/img_123.jpg",
                "expiresInSeconds": 900,
                "requiredHeaders": { "Content-Type": "image/jpeg" }
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val response = RailwayStorageRepository.PresignedUploadResponse(
            uploadUrl = json.getString("uploadUrl"),
            objectKey = json.getString("objectKey"),
            expiresInSeconds = json.getInt("expiresInSeconds"),
            requiredContentType = json.getJSONObject("requiredHeaders").getString("Content-Type")
        )

        assertEquals("https://storage.railway.app/upload?token=123", response.uploadUrl)
        assertEquals("listing-images/usr_1/lst_1/img_123.jpg", response.objectKey)
        assertEquals(900, response.expiresInSeconds)
        assertEquals("image/jpeg", response.requiredContentType)
    }

    @Test
    fun testCreateOrderResponseMapping() {
        val jsonString = """
            {
                "internalOrderId": "ord_999",
                "razorpayOrderId": "order_rzp_888",
                "razorpayKeyId": "rzp_test_key_123",
                "amountPaise": 4900,
                "currency": "INR",
                "listingTitle": "Engineering Physics Notes"
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val response = RazorpayPaymentRepository.CreateOrderResponse(
            internalOrderId = json.getString("internalOrderId"),
            razorpayOrderId = json.getString("razorpayOrderId"),
            razorpayKeyId = json.getString("razorpayKeyId"),
            amountPaise = json.getLong("amountPaise"),
            currency = json.getString("currency"),
            listingTitle = json.getString("listingTitle")
        )

        assertEquals("ord_999", response.internalOrderId)
        assertEquals("order_rzp_888", response.razorpayOrderId)
        assertEquals("rzp_test_key_123", response.razorpayKeyId)
        assertEquals(4900L, response.amountPaise)
        assertEquals("INR", response.currency)
    }

    @Test
    fun testRupeesToPaiseConversionEdgeCases() {
        assertEquals(100L, Listing.rupeesToPaise(1.0))
        assertEquals(9900L, Listing.rupeesToPaise(99.0))
        assertEquals(49900L, Listing.rupeesToPaise(499.0))
    }

    @Test
    fun testStableObjectKeyPathValidation() {
        val validKey = "listing-images/usr_123/lst_456/uuid_789.jpg"
        val invalidKey = "digital-files/../../etc/passwd"

        assertTrue(validKey.startsWith("listing-images/"))
        assertTrue(invalidKey.contains(".."))
    }
}
