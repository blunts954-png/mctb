package com.mctb.autoreply

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that listens for phone state changes
 * Detects missed calls and triggers auto-reply SMS
 */
class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallReceiver"
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var isIncoming = false
        private var incomingNumber: String? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        try {
            // Get the current call state
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumberExtra = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Log.d(TAG, "Call state changed: $state, Number: $incomingNumberExtra")

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Incoming call is ringing
                    isIncoming = true
                    incomingNumber = incomingNumberExtra
                    Log.d(TAG, "Incoming call from: $incomingNumber")
                }

                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call was answered
                    if (isIncoming) {
                        Log.d(TAG, "Call answered")
                    }
                }

                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call ended
                    if (isIncoming && lastState == TelephonyManager.CALL_STATE_RINGING) {
                        // Call went from RINGING to IDLE without OFFHOOK = missed call
                        val missedNumber = incomingNumber
                        if (missedNumber != null) {
                            Log.i(TAG, "Missed call detected from: $missedNumber")
                            handleMissedCall(context, missedNumber)
                        }
                    }

                    // Reset state
                    isIncoming = false
                    incomingNumber = null
                }
            }

            // Update last state
            lastState = when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
                TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
                else -> TelephonyManager.CALL_STATE_IDLE
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onReceive", e)
        }
    }

    /**
     * Handle a missed call by sending auto-reply SMS
     */
    private fun handleMissedCall(context: Context, phoneNumber: String) {
        // Use coroutine to avoid blocking the BroadcastReceiver
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val smsSender = SmsSender(context)
                val success = smsSender.processMissedCall(phoneNumber)

                if (success) {
                    Log.i(TAG, "Auto-reply sent for missed call from: $phoneNumber")
                } else {
                    Log.w(TAG, "Auto-reply not sent for missed call from: $phoneNumber")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling missed call", e)
            }
        }
    }
}
