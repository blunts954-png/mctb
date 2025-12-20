package com.mctb.autoreply

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages app settings using SharedPreferences
 * Stores: enabled state, message, active hours, usage count, debounce timestamps
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "AutoReplyPrefs"

        // Keys
        private const val KEY_ENABLED = "enabled"
        private const val KEY_MESSAGE = "message"
        private const val KEY_START_HOUR = "start_hour"
        private const val KEY_START_MINUTE = "start_minute"
        private const val KEY_END_HOUR = "end_hour"
        private const val KEY_END_MINUTE = "end_minute"
        private const val KEY_ALWAYS_ON = "always_on"
        private const val KEY_AUTO_TEXT_COUNT = "auto_text_count"
        private const val KEY_IS_UNLIMITED = "is_unlimited"
        private const val KEY_LAST_TEXT_PREFIX = "last_text_"

        // Defaults
        const val DEFAULT_MESSAGE = "Hi! I missed your call and I'm working right now. I'll call you back as soon as I can. Thanks!"
        const val DEFAULT_START_HOUR = 8
        const val DEFAULT_START_MINUTE = 0
        const val DEFAULT_END_HOUR = 18
        const val DEFAULT_END_MINUTE = 0
        const val FREE_TIER_LIMIT = 5
        const val DEBOUNCE_WINDOW_MS = 30 * 60 * 1000L // 30 minutes
    }

    // Enabled state
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    // Auto-reply message
    var message: String
        get() = prefs.getString(KEY_MESSAGE, DEFAULT_MESSAGE) ?: DEFAULT_MESSAGE
        set(value) = prefs.edit().putString(KEY_MESSAGE, value).apply()

    // Active hours - Start
    var startHour: Int
        get() = prefs.getInt(KEY_START_HOUR, DEFAULT_START_HOUR)
        set(value) = prefs.edit().putInt(KEY_START_HOUR, value).apply()

    var startMinute: Int
        get() = prefs.getInt(KEY_START_MINUTE, DEFAULT_START_MINUTE)
        set(value) = prefs.edit().putInt(KEY_START_MINUTE, value).apply()

    // Active hours - End
    var endHour: Int
        get() = prefs.getInt(KEY_END_HOUR, DEFAULT_END_HOUR)
        set(value) = prefs.edit().putInt(KEY_END_HOUR, value).apply()

    var endMinute: Int
        get() = prefs.getInt(KEY_END_MINUTE, DEFAULT_END_MINUTE)
        set(value) = prefs.edit().putInt(KEY_END_MINUTE, value).apply()

    // Always on toggle
    var isAlwaysOn: Boolean
        get() = prefs.getBoolean(KEY_ALWAYS_ON, false)
        set(value) = prefs.edit().putBoolean(KEY_ALWAYS_ON, value).apply()

    // Usage tracking
    var autoTextCount: Int
        get() = prefs.getInt(KEY_AUTO_TEXT_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_AUTO_TEXT_COUNT, value).apply()

    var isUnlimited: Boolean
        get() = prefs.getBoolean(KEY_IS_UNLIMITED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_UNLIMITED, value).apply()

    /**
     * Check if we've reached the free tier limit
     */
    fun hasReachedLimit(): Boolean {
        return !isUnlimited && autoTextCount >= FREE_TIER_LIMIT
    }

    /**
     * Increment the auto-text count
     */
    fun incrementAutoTextCount() {
        autoTextCount = autoTextCount + 1
    }

    /**
     * Check if we can send an auto-text to this number (debounce check)
     */
    fun canSendToNumber(phoneNumber: String): Boolean {
        val lastTextTime = getLastTextTime(phoneNumber)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastTextTime) > DEBOUNCE_WINDOW_MS
    }

    /**
     * Record that we sent a text to this number
     */
    fun recordTextSent(phoneNumber: String) {
        val key = KEY_LAST_TEXT_PREFIX + phoneNumber.filter { it.isDigit() }
        prefs.edit().putLong(key, System.currentTimeMillis()).apply()
    }

    /**
     * Get the last time we texted this number
     */
    private fun getLastTextTime(phoneNumber: String): Long {
        val key = KEY_LAST_TEXT_PREFIX + phoneNumber.filter { it.isDigit() }
        return prefs.getLong(key, 0L)
    }

    /**
     * Check if current time is within active hours
     */
    fun isWithinActiveHours(): Boolean {
        if (isAlwaysOn) return true

        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        val startTimeInMinutes = startHour * 60 + startMinute
        val endTimeInMinutes = endHour * 60 + endMinute

        return if (startTimeInMinutes < endTimeInMinutes) {
            // Normal case: e.g., 8:00 AM to 6:00 PM
            currentTimeInMinutes in startTimeInMinutes until endTimeInMinutes
        } else {
            // Crosses midnight: e.g., 10:00 PM to 2:00 AM
            currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes < endTimeInMinutes
        }
    }

    /**
     * Reset message to default
     */
    fun resetMessageToDefault() {
        message = DEFAULT_MESSAGE
    }

    /**
     * Get formatted start time string
     */
    fun getFormattedStartTime(): String {
        return String.format("%02d:%02d", startHour, startMinute)
    }

    /**
     * Get formatted end time string
     */
    fun getFormattedEndTime(): String {
        return String.format("%02d:%02d", endHour, endMinute)
    }
}
