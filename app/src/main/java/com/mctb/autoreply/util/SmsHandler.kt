package com.mctb.autoreply.util

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.mctb.autoreply.data.AppPreferences

/**
 * Handles SMS sending with validation, debounce, and limit enforcement.
 * This class encapsulates all the business logic for determining whether
 * to send an auto-reply and actually sending it.
 */
class SmsHandler(private val context: Context) {

    private val prefs = AppPreferences(context)

    companion object {
        private const val TAG = "SmsHandler"
    }

    /**
     * Process a missed call and send auto-reply if appropriate.
     * This is the main entry point called by the CallReceiver.
     *
     * @param phoneNumber The phone number that missed called
     * @return true if SMS was sent, false otherwise
     */
    suspend fun processMissedCall(phoneNumber: String): Boolean {
        try {
            Log.i(TAG, "Processing missed call from: $phoneNumber")

            // Validate phone number
            if (!isValidPhoneNumber(phoneNumber)) {
                Log.w(TAG, "Invalid or blocked phone number: $phoneNumber")
                return false
            }

            // Check if app is enabled
            if (!prefs.isEnabledSync()) {
                Log.d(TAG, "Auto-reply is disabled")
                return false
            }

            // Check active hours
            if (!prefs.isWithinActiveHours()) {
                Log.d(TAG, "Outside active hours")
                return false
            }

            // Check free tier limit
            if (prefs.hasReachedLimit()) {
                Log.w(TAG, "Free tier limit reached")
                return false
            }

            // Check debounce
            if (!prefs.canSendToNumber(phoneNumber)) {
                Log.d(TAG, "Recently texted this number, skipping (debounce)")
                return false
            }

            // Get message
            val message = prefs.getMessageSync()
            if (message.isBlank()) {
                Log.w(TAG, "Message is blank, cannot send")
                return false
            }

            // Send SMS
            val success = sendSms(phoneNumber, message)

            if (success) {
                // Increment usage counter
                prefs.incrementAutoTextCount()

                // Record that we texted this number
                prefs.recordTextSent(phoneNumber)

                val count = prefs.getAutoTextCountSync()
                Log.i(TAG, "Auto-reply sent successfully. Total count: $count")
            }

            return success

        } catch (e: Exception) {
            Log.e(TAG, "Error processing missed call", e)
            return false
        }
    }

    /**
     * Send SMS using Android SmsManager.
     *
     * @param phoneNumber Recipient phone number
     * @param message Message text
     * @return true if sent successfully, false otherwise
     */
    private fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Send the message
            smsManager.sendTextMessage(
                phoneNumber,
                null, // Use default SMS center
                message,
                null, // No sent intent for now
                null  // No delivery intent for now
            )

            Log.i(TAG, "SMS sent to $phoneNumber")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
            false
        }
    }

    /**
     * Validate that the phone number is valid and not blocked/unknown.
     * Filters out common patterns for unknown/private callers.
     *
     * @param phoneNumber The phone number to validate
     * @return true if valid, false if should be skipped
     */
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false

        // List of patterns that indicate unknown/private/blocked numbers
        val invalidPatterns = listOf(
            "-1",           // Unknown number marker
            "-2",           // Private number marker
            "0",            // Invalid zero
            "UNKNOWN",      // Unknown caller text
            "PRIVATE",      // Private caller text
            "ANONYMOUS",    // Anonymous caller text
            "BLOCKED",      // Blocked caller text
            "RESTRICTED"    // Restricted caller text
        )

        val cleanNumber = phoneNumber.uppercase().trim()

        // Check if number matches any invalid pattern
        if (invalidPatterns.any { cleanNumber.contains(it) }) {
            return false
        }

        // Number must contain at least some digits
        if (phoneNumber.count { it.isDigit() } < 3) {
            return false
        }

        return true
    }
}
