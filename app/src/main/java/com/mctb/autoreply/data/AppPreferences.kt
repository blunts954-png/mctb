package com.mctb.autoreply.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * DataStore-based preferences manager for app settings.
 * Provides type-safe access to all app configuration and state.
 */
class AppPreferences(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

        // Preference keys
        private val KEY_ENABLED = booleanPreferencesKey("enabled")
        private val KEY_MESSAGE = stringPreferencesKey("message")
        private val KEY_START_HOUR = intPreferencesKey("start_hour")
        private val KEY_START_MINUTE = intPreferencesKey("start_minute")
        private val KEY_END_HOUR = intPreferencesKey("end_hour")
        private val KEY_END_MINUTE = intPreferencesKey("end_minute")
        private val KEY_ALWAYS_ON = booleanPreferencesKey("always_on")
        private val KEY_AUTO_TEXT_COUNT = intPreferencesKey("auto_text_count")
        private val KEY_IS_UNLIMITED = booleanPreferencesKey("is_unlimited")
        private val KEY_MASTER_MODE = booleanPreferencesKey("master_mode") // Hidden developer mode

        // Default values
        const val DEFAULT_MESSAGE = "Hi! I missed your call and I'm working right now. I'll call you back as soon as I can. Thanks!"
        const val DEFAULT_START_HOUR = 8
        const val DEFAULT_START_MINUTE = 0
        const val DEFAULT_END_HOUR = 18
        const val DEFAULT_END_MINUTE = 0
        const val FREE_TIER_LIMIT = 5
        const val DEBOUNCE_WINDOW_MS = 30 * 60 * 1000L // 30 minutes
    }

    // Flows for reactive UI updates
    val isEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_ENABLED] ?: false }
    val message: Flow<String> = context.dataStore.data.map { it[KEY_MESSAGE] ?: DEFAULT_MESSAGE }
    val startHour: Flow<Int> = context.dataStore.data.map { it[KEY_START_HOUR] ?: DEFAULT_START_HOUR }
    val startMinute: Flow<Int> = context.dataStore.data.map { it[KEY_START_MINUTE] ?: DEFAULT_START_MINUTE }
    val endHour: Flow<Int> = context.dataStore.data.map { it[KEY_END_HOUR] ?: DEFAULT_END_HOUR }
    val endMinute: Flow<Int> = context.dataStore.data.map { it[KEY_END_MINUTE] ?: DEFAULT_END_MINUTE }
    val isAlwaysOn: Flow<Boolean> = context.dataStore.data.map { it[KEY_ALWAYS_ON] ?: false }
    val autoTextCount: Flow<Int> = context.dataStore.data.map { it[KEY_AUTO_TEXT_COUNT] ?: 0 }
    val isUnlimited: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_UNLIMITED] ?: false }
    val isMasterMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_MASTER_MODE] ?: false }

    // Combined flow for usage status
    val usageStatus: Flow<UsageStatus> = context.dataStore.data.map { prefs ->
        val count = prefs[KEY_AUTO_TEXT_COUNT] ?: 0
        val unlimited = prefs[KEY_IS_UNLIMITED] ?: false
        val masterMode = prefs[KEY_MASTER_MODE] ?: false
        UsageStatus(count, unlimited, masterMode)
    }

    // Suspend functions for write operations
    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setMessage(message: String) {
        context.dataStore.edit { it[KEY_MESSAGE] = message }
    }

    suspend fun resetMessageToDefault() {
        context.dataStore.edit { it[KEY_MESSAGE] = DEFAULT_MESSAGE }
    }

    suspend fun setActiveHours(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_START_HOUR] = startHour
            prefs[KEY_START_MINUTE] = startMinute
            prefs[KEY_END_HOUR] = endHour
            prefs[KEY_END_MINUTE] = endMinute
        }
    }

    suspend fun setAlwaysOn(alwaysOn: Boolean) {
        context.dataStore.edit { it[KEY_ALWAYS_ON] = alwaysOn }
    }

    suspend fun incrementAutoTextCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_AUTO_TEXT_COUNT] ?: 0
            prefs[KEY_AUTO_TEXT_COUNT] = current + 1
        }
    }

    suspend fun setUnlimited(unlimited: Boolean) {
        context.dataStore.edit { it[KEY_IS_UNLIMITED] = unlimited }
    }

    suspend fun setMasterMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MASTER_MODE] = enabled }
    }

    // Synchronous read helpers for background service/receiver
    suspend fun isEnabledSync(): Boolean {
        var enabled = false
        context.dataStore.data.map { it[KEY_ENABLED] ?: false }.collect { enabled = it }
        return enabled
    }

    suspend fun getMessageSync(): String {
        var msg = DEFAULT_MESSAGE
        context.dataStore.data.map { it[KEY_MESSAGE] ?: DEFAULT_MESSAGE }.collect { msg = it }
        return msg
    }

    suspend fun isAlwaysOnSync(): Boolean {
        var alwaysOn = false
        context.dataStore.data.map { it[KEY_ALWAYS_ON] ?: false }.collect { alwaysOn = it }
        return alwaysOn
    }

    suspend fun isUnlimitedSync(): Boolean {
        var unlimited = false
        context.dataStore.data.map { it[KEY_IS_UNLIMITED] ?: false }.collect { unlimited = it }
        return unlimited
    }

    suspend fun isMasterModeSync(): Boolean {
        var masterMode = false
        context.dataStore.data.map { it[KEY_MASTER_MODE] ?: false }.collect { masterMode = it }
        return masterMode
    }

    suspend fun getAutoTextCountSync(): Int {
        var count = 0
        context.dataStore.data.map { it[KEY_AUTO_TEXT_COUNT] ?: 0 }.collect { count = it }
        return count
    }

    /**
     * Check if we've reached the free tier limit.
     * Master mode bypasses all limits.
     */
    suspend fun hasReachedLimit(): Boolean {
        // Master mode has no limits
        if (isMasterModeSync()) return false

        val unlimited = isUnlimitedSync()
        val count = getAutoTextCountSync()
        return !unlimited && count >= FREE_TIER_LIMIT
    }

    /**
     * Check if current time is within active hours.
     */
    suspend fun isWithinActiveHours(): Boolean {
        if (isAlwaysOnSync()) return true

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        var start = 0
        var end = 0
        context.dataStore.data.map { prefs ->
            val startH = prefs[KEY_START_HOUR] ?: DEFAULT_START_HOUR
            val startM = prefs[KEY_START_MINUTE] ?: DEFAULT_START_MINUTE
            val endH = prefs[KEY_END_HOUR] ?: DEFAULT_END_HOUR
            val endM = prefs[KEY_END_MINUTE] ?: DEFAULT_END_MINUTE
            start = startH * 60 + startM
            end = endH * 60 + endM
        }.collect { }

        return if (start < end) {
            // Normal case: e.g., 8:00 AM to 6:00 PM
            currentTimeInMinutes in start until end
        } else {
            // Crosses midnight: e.g., 10:00 PM to 2:00 AM
            currentTimeInMinutes >= start || currentTimeInMinutes < end
        }
    }

    /**
     * Debounce tracking - store last text time per phone number.
     */
    suspend fun canSendToNumber(phoneNumber: String): Boolean {
        val lastTextTime = getLastTextTime(phoneNumber)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastTextTime) > DEBOUNCE_WINDOW_MS
    }

    suspend fun recordTextSent(phoneNumber: String) {
        val key = longPreferencesKey("last_text_${phoneNumber.filter { it.isDigit() }}")
        context.dataStore.edit { it[key] = System.currentTimeMillis() }
    }

    private suspend fun getLastTextTime(phoneNumber: String): Long {
        val key = longPreferencesKey("last_text_${phoneNumber.filter { it.isDigit() }}")
        var time = 0L
        context.dataStore.data.map { it[key] ?: 0L }.collect { time = it }
        return time
    }
}

/**
 * Data class representing usage status.
 */
data class UsageStatus(
    val count: Int,
    val isUnlimited: Boolean,
    val isMasterMode: Boolean = false
) {
    val hasReachedLimit: Boolean
        get() = !isMasterMode && !isUnlimited && count >= AppPreferences.FREE_TIER_LIMIT
}
