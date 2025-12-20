package com.mctb.autoreply

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver that starts the monitoring service when device boots
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, checking if service should start")

            val prefs = AppPreferences(context)
            if (prefs.isEnabled) {
                Log.i(TAG, "Starting CallMonitorService after boot")
                CallMonitorService.start(context)
            }
        }
    }
}
