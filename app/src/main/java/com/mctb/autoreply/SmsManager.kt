package com.mctb.autoreply

import android.content.Context
import android.telephony.SmsManager
import android.util.Log

/**
 * Handles sending SMS messages
 */
class SmsSender(private val context: Context) {

    companion object {
        private const val TAG = "SmsSender"
    }

    /**
     * Send an SMS message to the specified phone number
     * @param phoneNumber The recipient's phone number
     * @param message The message to send
     * @return true if message was sent successfully, false otherwise
     */
    fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            // Validate phone number
            if (phoneNumber.isBlank()) {
                Log.w(TAG, "Cannot send SMS: phone number is blank")
                return false
            }

            // Validate message
            if (message.isBlank()) {
                Log.w(TAG, "Cannot send SMS: message is blank")
                return false
            }

            // Get SMS manager
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Send SMS
            smsManager.sendTextMessage(
                phoneNumber,
                null, // Use default SMS center
                message,
                null, // No sent intent
                null  // No delivery intent
            )

            Log.i(TAG, "SMS sent successfully to $phoneNumber")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
            false
        }
    }

    /**
     * Check if we should send an auto-reply for this missed call
     * This performs all the necessary checks:
     * - App is enabled
     * - Within active hours
     * - Haven't reached free tier limit
     * - Haven't texted this number recently (debounce)
     */
    fun shouldSendAutoReply(phoneNumber: String, prefs: AppPreferences): Boolean {
        // Check if app is enabled
        if (!prefs.isEnabled) {
            Log.d(TAG, "Auto-reply disabled")
            return false
        }

        // Check if within active hours
        if (!prefs.isWithinActiveHours()) {
            Log.d(TAG, "Outside active hours")
            return false
        }

        // Check if reached free tier limit
        if (prefs.hasReachedLimit()) {
            Log.d(TAG, "Reached free tier limit")
            return false
        }

        // Check if we can send to this number (debounce)
        if (!prefs.canSendToNumber(phoneNumber)) {
            Log.d(TAG, "Recently texted this number, skipping (debounce)")
            return false
        }

        // Check for invalid/private numbers
        if (isInvalidNumber(phoneNumber)) {
            Log.d(TAG, "Invalid or private number, skipping")
            return false
        }

        return true
    }

    /**
     * Process a missed call and send auto-reply if appropriate
     */
    fun processMissedCall(phoneNumber: String): Boolean {
        val prefs = AppPreferences(context)

        // Check if we should send
        if (!shouldSendAutoReply(phoneNumber, prefs)) {
            return false
        }

        // Get the message
        val message = prefs.message

        // Send the SMS
        val success = sendSms(phoneNumber, message)

        if (success) {
            // Increment counter
            prefs.incrementAutoTextCount()

            // Record that we texted this number
            prefs.recordTextSent(phoneNumber)

            Log.i(TAG, "Auto-reply sent successfully. Count: ${prefs.autoTextCount}")
        }

        return success
    }

    /**
     * Check if phone number is invalid or should be skipped
     */
    private fun isInvalidNumber(phoneNumber: String): Boolean {
        // Filter out null, empty, or placeholder numbers
        if (phoneNumber.isBlank()) return true

        // Common patterns for unknown/private callers
        val invalidPatterns = listOf(
            "-1",           // Unknown number
            "-2",           // Private number
            "0",            // Invalid
            "UNKNOWN",      // Unknown caller
            "PRIVATE",      // Private caller
            "ANONYMOUS"     // Anonymous caller
        )

        val cleanNumber = phoneNumber.uppercase().trim()
        return invalidPatterns.any { cleanNumber.contains(it) }
    }
}
