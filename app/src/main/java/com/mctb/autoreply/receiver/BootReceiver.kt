package com.mctb.autoreply.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mctb.autoreply.data.AppPreferences
import com.mctb.autoreply.service.CallMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles device boot completion.
 *
 * When the device boots, this receiver checks if auto-reply was enabled
 * before shutdown. If it was, it restarts the CallMonitorService to
 * resume monitoring for missed calls.
 *
 * This ensures the app continues working after a device reboot without
 * requiring user intervention.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = AppPreferences(context)

                    // Check if auto-reply was enabled before reboot
                    val wasEnabled = prefs.isEnabledSync()

                    if (wasEnabled) {
                        Log.i(TAG, "Auto-reply was enabled, restarting service")
                        CallMonitorService.start(context)
                    } else {
                        Log.d(TAG, "Auto-reply was disabled, not starting service")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in boot receiver", e)
                }
            }
        }
    }
}
