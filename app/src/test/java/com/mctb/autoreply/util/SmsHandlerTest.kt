package com.mctb.autoreply.util

import android.content.Context
import android.telephony.SmsManager
import com.mctb.autoreply.data.AppPreferences
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SmsHandler.
 * Tests the business logic for auto-reply SMS sending.
 */
class SmsHandlerTest {

    private lateinit var context: Context
    private lateinit var appPreferences: AppPreferences
    private lateinit var smsHandler: SmsHandler

    @Before
    fun setup() {
        // Mock Android context
        context = mockk(relaxed = true)

        // Mock AppPreferences
        appPreferences = mockk(relaxed = true)

        // Mock static method for AppPreferences constructor
        mockkConstructor(AppPreferences::class)
        every { anyConstructed<AppPreferences>().isEnabledSync() } returns true
        every { anyConstructed<AppPreferences>().isWithinActiveHours() } returns true
        every { anyConstructed<AppPreferences>().hasReachedLimit() } returns false
        every { anyConstructed<AppPreferences>().canSendToNumber(any()) } returns true
        every { anyConstructed<AppPreferences>().getMessageSync() } returns "Test message"
        every { anyConstructed<AppPreferences>().getAutoTextCountSync() } returns 0
        every { anyConstructed<AppPreferences>().incrementAutoTextCount() } just Runs
        every { anyConstructed<AppPreferences>().recordTextSent(any()) } just Runs

        // Create handler
        smsHandler = SmsHandler(context)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // ====================================
    // Phone Number Validation Tests
    // ====================================

    @Test
    fun `isValidPhoneNumber returns true for normal phone numbers`() = runTest {
        // Use reflection to access private method for testing
        val method = SmsHandler::class.java.getDeclaredMethod("isValidPhoneNumber", String::class.java)
        method.isAccessible = true

        val validNumbers = listOf(
            "+1234567890",
            "1234567890",
            "+1 (234) 567-8900",
            "234-567-8900",
            "(234) 567-8900"
        )

        validNumbers.forEach { number ->
            val result = method.invoke(smsHandler, number) as Boolean
            assertTrue("Expected $number to be valid", result)
        }
    }

    @Test
    fun `isValidPhoneNumber returns false for unknown numbers`() = runTest {
        val method = SmsHandler::class.java.getDeclaredMethod("isValidPhoneNumber", String::class.java)
        method.isAccessible = true

        val invalidNumbers = listOf(
            "-1",
            "-2",
            "0",
            "UNKNOWN",
            "PRIVATE",
            "ANONYMOUS",
            "BLOCKED",
            "RESTRICTED",
            "unknown",  // Lowercase
            "private",  // Lowercase
            " UNKNOWN ", // With spaces
            ""  // Empty
        )

        invalidNumbers.forEach { number ->
            val result = method.invoke(smsHandler, number) as Boolean
            assertFalse("Expected '$number' to be invalid", result)
        }
    }

    @Test
    fun `isValidPhoneNumber returns false for numbers with too few digits`() = runTest {
        val method = SmsHandler::class.java.getDeclaredMethod("isValidPhoneNumber", String::class.java)
        method.isAccessible = true

        val invalidNumbers = listOf(
            "12",        // Only 2 digits
            "AB",        // No digits
            "()--",      // Only formatting, no digits
            "   "        // Only spaces
        )

        invalidNumbers.forEach { number ->
            val result = method.invoke(smsHandler, number) as Boolean
            assertFalse("Expected '$number' to be invalid (too few digits)", result)
        }
    }

    // ====================================
    // Business Logic Integration Tests
    // ====================================

    @Test
    fun `processMissedCall returns false when app is disabled`() = runTest {
        every { anyConstructed<AppPreferences>().isEnabledSync() } returns false

        // Mock SmsManager to track if SMS is sent
        val smsManager = mockk<SmsManager>(relaxed = true)
        mockkStatic(SmsManager::class)
        every { SmsManager.getDefault() } returns smsManager

        val result = smsHandler.processMissedCall("+1234567890")

        assertFalse("Should not send SMS when app is disabled", result)
        verify(exactly = 0) { smsManager.sendTextMessage(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `processMissedCall returns false when outside active hours`() = runTest {
        every { anyConstructed<AppPreferences>().isWithinActiveHours() } returns false

        val smsManager = mockk<SmsManager>(relaxed = true)
        mockkStatic(SmsManager::class)
        every { SmsManager.getDefault() } returns smsManager

        val result = smsHandler.processMissedCall("+1234567890")

        assertFalse("Should not send SMS outside active hours", result)
        verify(exactly = 0) { smsManager.sendTextMessage(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `processMissedCall returns false when limit reached`() = runTest {
        every { anyConstructed<AppPreferences>().hasReachedLimit() } returns true

        val smsManager = mockk<SmsManager>(relaxed = true)
        mockkStatic(SmsManager::class)
        every { SmsManager.getDefault() } returns smsManager

        val result = smsHandler.processMissedCall("+1234567890")

        assertFalse("Should not send SMS when limit reached", result)
        verify(exactly = 0) { smsManager.sendTextMessage(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `processMissedCall returns false when debounce prevents sending`() = runTest {
        every { anyConstructed<AppPreferences>().canSendToNumber(any()) } returns false

        val smsManager = mockk<SmsManager>(relaxed = true)
        mockkStatic(SmsManager::class)
        every { SmsManager.getDefault() } returns smsManager

        val result = smsHandler.processMissedCall("+1234567890")

        assertFalse("Should not send SMS during debounce period", result)
        verify(exactly = 0) { smsManager.sendTextMessage(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `processMissedCall returns false when message is blank`() = runTest {
        every { anyConstructed<AppPreferences>().getMessageSync() } returns ""

        val smsManager = mockk<SmsManager>(relaxed = true)
        mockkStatic(SmsManager::class)
        every { SmsManager.getDefault() } returns smsManager

        val result = smsHandler.processMissedCall("+1234567890")

        assertFalse("Should not send SMS when message is blank", result)
        verify(exactly = 0) { smsManager.sendTextMessage(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `processMissedCall returns false for invalid phone numbers`() = runTest {
        val smsManager = mockk<SmsManager>(relaxed = true)
        mockkStatic(SmsManager::class)
        every { SmsManager.getDefault() } returns smsManager

        val invalidNumbers = listOf("-1", "UNKNOWN", "PRIVATE", "")

        invalidNumbers.forEach { number ->
            val result = smsHandler.processMissedCall(number)
            assertFalse("Should not send SMS to invalid number: $number", result)
        }

        verify(exactly = 0) { smsManager.sendTextMessage(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `processMissedCall increments counter and records timestamp on success`() = runTest {
        val smsManager = mockk<SmsManager>(relaxed = true)
        mockkStatic(SmsManager::class)
        every { SmsManager.getDefault() } returns smsManager

        val phoneNumber = "+1234567890"
        smsHandler.processMissedCall(phoneNumber)

        verify(exactly = 1) { anyConstructed<AppPreferences>().incrementAutoTextCount() }
        verify(exactly = 1) { anyConstructed<AppPreferences>().recordTextSent(phoneNumber) }
    }

    @Test
    fun `processMissedCall handles exceptions gracefully`() = runTest {
        // Simulate an exception in the validation chain
        every { anyConstructed<AppPreferences>().isEnabledSync() } throws RuntimeException("Test exception")

        val result = smsHandler.processMissedCall("+1234567890")

        assertFalse("Should return false on exception", result)
        // Should not crash
    }
}
