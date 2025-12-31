package com.mctb.autoreply.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.mctb.autoreply.util.SmsHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

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
        // Track call state across broadcasts
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var isIncoming = false
        private var incomingNumber: String? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            Timber.e("CallReceiver received null context")
            return
        }

        if (intent == null) {
            Timber.e("CallReceiver received null intent")
            return
        }

        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            Timber.d("Ignoring non-phone-state intent: %s", intent.action)
            return
        }

        try {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Timber.d("Phone state changed: %s, Number: %s", state, phoneNumber ?: "hidden")

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Call is ringing (incoming call)
                    isIncoming = true
                    incomingNumber = phoneNumber
                    lastState = TelephonyManager.CALL_STATE_RINGING
                    Timber.d("Incoming call detected")
                }

                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call was answered
                    lastState = TelephonyManager.CALL_STATE_OFFHOOK
                    if (isIncoming) {
                        Timber.d("Incoming call was answered")
                    }
                }

                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call ended
                    if (isIncoming && lastState == TelephonyManager.CALL_STATE_RINGING) {
                        // Call went from RINGING to IDLE without OFFHOOK = missed call
                        val missedNumber = incomingNumber
                        if (missedNumber != null) {
                            Timber.i("Missed call detected, processing auto-reply")
                            handleMissedCall(context, missedNumber)
                        } else {
                            Timber.w("Missed call detected but phone number is null")
                        }
                    }

                    // Reset state
                    lastState = TelephonyManager.CALL_STATE_IDLE
                    isIncoming = false
                    incomingNumber = null
                }

                else -> {
                    Timber.w("Unknown phone state: %s", state)
                }
            }

        } catch (e: SecurityException) {
            Timber.e(e, "Security exception - missing READ_PHONE_STATE permission")
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error in CallReceiver.onReceive")
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
                    Timber.i("Auto-reply sent successfully for missed call")
                } else {
                    Timber.d("Auto-reply not sent for missed call (validation or limit check failed)")
                }
            } catch (e: SecurityException) {
                Timber.e(e, "Security exception - missing permissions for SMS handling")
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error handling missed call")
            }
        }
    }
}
