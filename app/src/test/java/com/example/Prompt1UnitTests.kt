package com.example

import com.example.data.model.Listing
import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Prompt1UnitTests {

    @Test
    fun testRupeesToPaiseConversion() {
        assertEquals(0L, Listing.rupeesToPaise(0.0))
        assertEquals(4900L, Listing.rupeesToPaise(49.0))
        assertEquals(25000L, Listing.rupeesToPaise(250.0))
        assertEquals(99900L, Listing.rupeesToPaise(999.0))
    }

    @Test
    fun testListingRupeesGetters() {
        val listing = Listing(
            pricePaise = 4900L,
            originalPricePaise = 10000L
        )
        assertEquals(49.0, listing.rupeesPrice, 0.001)
        assertEquals(100.0, listing.rupeesOriginalPrice, 0.001)
    }

    @Test
    fun testUserProfileDefaults() {
        val profile = UserProfile(
            uid = "user_123",
            email = "student@example.com",
            displayName = "Student User"
        )
        assertTrue(profile.canBuy)
        assertFalse(profile.canSell)
        assertEquals("active", profile.accountStatus)
    }

    @Test
    fun testEmailPatternValidation() {
        val validEmail = "student@college.edu"
        val invalidEmail = "student-not-an-email"

        assertTrue(android.util.Patterns.EMAIL_ADDRESS.matcher(validEmail).matches())
        assertFalse(android.util.Patterns.EMAIL_ADDRESS.matcher(invalidEmail).matches())
    }
}
