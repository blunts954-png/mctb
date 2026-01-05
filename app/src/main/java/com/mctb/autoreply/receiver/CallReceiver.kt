package com.mctb.autoreply.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.mctb.autoreply.util.SmsHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that listens for phone state changes to detect missed calls.
 *
 * This receiver is triggered by the PHONE_STATE intent and tracks call state
 * transitions to identify when a call goes from RINGING to IDLE without ever
 * reaching OFFHOOK (answered), which indicates a missed call.
 *
 * When a missed call is detected, it delegates to SmsHandler to send the auto-reply.
 */
class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallReceiver"

        // Track call state across broadcasts
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var isIncoming = false
        private var incomingNumber: String? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "=== onReceive TRIGGERED === Action: ${intent?.action}")

        if (context == null || intent == null) {
            Log.e(TAG, "Context or Intent is null!")
            return
        }

        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            Log.w(TAG, "Wrong action: ${intent.action}")
            return
        }

        try {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Log.i(TAG, "=== PHONE STATE: $state | Number from intent: $phoneNumber ===")

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Call is ringing (incoming call)
                    isIncoming = true
                    incomingNumber = phoneNumber
                    lastState = TelephonyManager.CALL_STATE_RINGING
                    Log.d(TAG, "Incoming call from: $phoneNumber")
                }

                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call was answered
                    lastState = TelephonyManager.CALL_STATE_OFFHOOK
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
                    lastState = TelephonyManager.CALL_STATE_IDLE
                    isIncoming = false
                    incomingNumber = null
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onReceive", e)
        }
    }

    /**
     * Handle a missed call by sending an auto-reply SMS.
     * Uses a coroutine to avoid blocking the BroadcastReceiver.
     */
    private fun handleMissedCall(context: Context, phoneNumber: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val smsHandler = SmsHandler(context)
                val success = smsHandler.processMissedCall(phoneNumber)

                if (success) {
                    Log.i(TAG, "Auto-reply sent for missed call from: $phoneNumber")
                } else {
                    Log.d(TAG, "Auto-reply not sent for missed call from: $phoneNumber")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling missed call", e)
            }
        }
    }
}
