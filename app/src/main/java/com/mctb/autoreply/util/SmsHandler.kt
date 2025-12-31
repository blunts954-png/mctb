package com.mctb.autoreply.util

import android.content.Context
import android.telephony.SmsManager
import com.mctb.autoreply.data.AppPreferences
import timber.log.Timber

/**
 * Handles SMS sending with validation, debounce, and limit enforcement.
 * This class encapsulates all the business logic for determining whether
 * to send an auto-reply and actually sending it.
 */
class SmsHandler(private val context: Context) {

    private val prefs = AppPreferences(context)

    /**
     * Process a missed call and send auto-reply if appropriate.
     * This is the main entry point called by the CallReceiver.
     *
     * @param phoneNumber The phone number that missed called
     * @return true if SMS was sent, false otherwise
     */
    suspend fun processMissedCall(phoneNumber: String): Boolean {
        try {
            Timber.i("Processing missed call from: %s", phoneNumber)

            // Validate phone number
            if (!isValidPhoneNumber(phoneNumber)) {
                Timber.w("Invalid or blocked phone number: %s", phoneNumber)
                return false
            }

            // Check if app is enabled
            if (!prefs.isEnabledSync()) {
                Timber.d("Auto-reply is disabled")
                return false
            }

            // Check active hours
            if (!prefs.isWithinActiveHours()) {
                Timber.d("Outside active hours")
                return false
            }

            // Check free tier limit
            if (prefs.hasReachedLimit()) {
                Timber.w("Free tier limit reached (count: %d)", prefs.getAutoTextCountSync())
                return false
            }

            // Check debounce
            if (!prefs.canSendToNumber(phoneNumber)) {
                Timber.d("Recently texted this number, skipping (debounce)")
                return false
            }

            // Get message
            val message = prefs.getMessageSync()
            if (message.isBlank()) {
                Timber.w("Message is blank, cannot send")
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
                Timber.i("Auto-reply sent successfully. Total count: %d", count)
            } else {
                Timber.w("Failed to send SMS to %s", phoneNumber)
            }

            return success

        } catch (e: SecurityException) {
            Timber.e(e, "Security exception - missing permissions for SMS")
            return false
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid arguments when processing call from %s", phoneNumber)
            return false
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error processing missed call from %s", phoneNumber)
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
                    ?: throw IllegalStateException("SmsManager service not available")
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Validate message length (SMS limit is 160 characters for single message)
            if (message.length > 160) {
                Timber.w("Message exceeds 160 characters, may be sent as multiple SMS")
            }

            // Send the message
            smsManager.sendTextMessage(
                phoneNumber,
                null, // Use default SMS center
                message,
                null, // No sent intent for now
                null  // No delivery intent for now
            )

            Timber.i("SMS sent successfully to %s", phoneNumber)
            true

        } catch (e: SecurityException) {
            Timber.e(e, "Security exception - missing SEND_SMS permission")
            false
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid phone number or message: %s", phoneNumber)
            false
        } catch (e: IllegalStateException) {
            Timber.e(e, "SMS manager not available")
            false
        } catch (e: Exception) {
            Timber.e(e, "Failed to send SMS to %s", phoneNumber)
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
