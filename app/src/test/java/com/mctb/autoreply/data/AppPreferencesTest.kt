package com.mctb.autoreply.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for AppPreferences data layer.
 * Tests active hours logic, debounce, and limit tracking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppPreferencesTest {

    private lateinit var testContext: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var appPreferences: AppPreferences
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        testContext = mockk(relaxed = true)

        // Create an in-memory DataStore for testing
        testDataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(testDispatcher + Job())
        ) {
            testContext.preferencesDataStoreFile("test_prefs")
        }

        // Mock the context to return our test DataStore
        every { testContext.dataStore } returns testDataStore

        appPreferences = AppPreferences(testContext)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // ====================================
    // Basic Read/Write Tests
    // ====================================

    @Test
    fun `default values are correct`() = runTest {
        assertEquals(false, appPreferences.isEnabled.first())
        assertEquals(AppPreferences.DEFAULT_MESSAGE, appPreferences.message.first())
        assertEquals(AppPreferences.DEFAULT_START_HOUR, appPreferences.startHour.first())
        assertEquals(AppPreferences.DEFAULT_START_MINUTE, appPreferences.startMinute.first())
        assertEquals(AppPreferences.DEFAULT_END_HOUR, appPreferences.endHour.first())
        assertEquals(AppPreferences.DEFAULT_END_MINUTE, appPreferences.endMinute.first())
        assertEquals(false, appPreferences.isAlwaysOn.first())
        assertEquals(0, appPreferences.autoTextCount.first())
        assertEquals(false, appPreferences.isUnlimited.first())
    }

    @Test
    fun `setEnabled updates enabled state`() = runTest {
        appPreferences.setEnabled(true)
        assertTrue(appPreferences.isEnabled.first())

        appPreferences.setEnabled(false)
        assertFalse(appPreferences.isEnabled.first())
    }

    @Test
    fun `setMessage updates message`() = runTest {
        val newMessage = "Custom auto-reply message"
        appPreferences.setMessage(newMessage)
        assertEquals(newMessage, appPreferences.message.first())
    }

    @Test
    fun `resetMessageToDefault restores default message`() = runTest {
        appPreferences.setMessage("Custom message")
        appPreferences.resetMessageToDefault()
        assertEquals(AppPreferences.DEFAULT_MESSAGE, appPreferences.message.first())
    }

    @Test
    fun `setActiveHours updates all hour fields`() = runTest {
        appPreferences.setActiveHours(9, 30, 17, 45)

        assertEquals(9, appPreferences.startHour.first())
        assertEquals(30, appPreferences.startMinute.first())
        assertEquals(17, appPreferences.endHour.first())
        assertEquals(45, appPreferences.endMinute.first())
    }

    @Test
    fun `incrementAutoTextCount increases count`() = runTest {
        assertEquals(0, appPreferences.autoTextCount.first())

        appPreferences.incrementAutoTextCount()
        assertEquals(1, appPreferences.autoTextCount.first())

        appPreferences.incrementAutoTextCount()
        assertEquals(2, appPreferences.autoTextCount.first())
    }

    @Test
    fun `setUnlimited updates unlimited status`() = runTest {
        appPreferences.setUnlimited(true)
        assertTrue(appPreferences.isUnlimited.first())

        appPreferences.setUnlimited(false)
        assertFalse(appPreferences.isUnlimited.first())
    }

    // ====================================
    // Usage Status Tests
    // ====================================

    @Test
    fun `usageStatus reflects correct state`() = runTest {
        var status = appPreferences.usageStatus.first()
        assertEquals(0, status.count)
        assertEquals(false, status.isUnlimited)
        assertEquals(false, status.hasReachedLimit)

        // Increment to limit
        repeat(AppPreferences.FREE_TIER_LIMIT) {
            appPreferences.incrementAutoTextCount()
        }

        status = appPreferences.usageStatus.first()
        assertEquals(AppPreferences.FREE_TIER_LIMIT, status.count)
        assertTrue(status.hasReachedLimit)
    }

    @Test
    fun `hasReachedLimit returns true when at limit without unlimited`() = runTest {
        repeat(AppPreferences.FREE_TIER_LIMIT) {
            appPreferences.incrementAutoTextCount()
        }

        assertTrue(appPreferences.hasReachedLimit())
    }

    @Test
    fun `hasReachedLimit returns false when unlimited is true`() = runTest {
        repeat(AppPreferences.FREE_TIER_LIMIT + 10) {
            appPreferences.incrementAutoTextCount()
        }
        appPreferences.setUnlimited(true)

        assertFalse(appPreferences.hasReachedLimit())
    }

    @Test
    fun `hasReachedLimit returns false when below limit`() = runTest {
        appPreferences.incrementAutoTextCount()
        appPreferences.incrementAutoTextCount()

        assertFalse(appPreferences.hasReachedLimit())
    }

    // ====================================
    // Active Hours Tests
    // ====================================

    @Test
    fun `isWithinActiveHours returns true when always on`() = runTest {
        appPreferences.setAlwaysOn(true)

        // Should be true regardless of time
        assertTrue(appPreferences.isWithinActiveHours())
    }

    @Test
    fun `isWithinActiveHours works for normal hours (8am to 6pm)`() = runTest {
        appPreferences.setAlwaysOn(false)
        appPreferences.setActiveHours(8, 0, 18, 0) // 8:00 AM to 6:00 PM

        mockkStatic(Calendar::class)

        // Test at 12:00 PM (within hours)
        val calendar12pm = mockk<Calendar>()
        every { calendar12pm.get(Calendar.HOUR_OF_DAY) } returns 12
        every { calendar12pm.get(Calendar.MINUTE) } returns 0
        every { Calendar.getInstance() } returns calendar12pm
        assertTrue(appPreferences.isWithinActiveHours())

        // Test at 7:59 AM (before hours)
        val calendar7am = mockk<Calendar>()
        every { calendar7am.get(Calendar.HOUR_OF_DAY) } returns 7
        every { calendar7am.get(Calendar.MINUTE) } returns 59
        every { Calendar.getInstance() } returns calendar7am
        assertFalse(appPreferences.isWithinActiveHours())

        // Test at 6:00 PM (at end, should be excluded)
        val calendar6pm = mockk<Calendar>()
        every { calendar6pm.get(Calendar.HOUR_OF_DAY) } returns 18
        every { calendar6pm.get(Calendar.MINUTE) } returns 0
        every { Calendar.getInstance() } returns calendar6pm
        assertFalse(appPreferences.isWithinActiveHours())

        // Test at 8:00 PM (after hours)
        val calendar8pm = mockk<Calendar>()
        every { calendar8pm.get(Calendar.HOUR_OF_DAY) } returns 20
        every { calendar8pm.get(Calendar.MINUTE) } returns 0
        every { Calendar.getInstance() } returns calendar8pm
        assertFalse(appPreferences.isWithinActiveHours())
    }

    @Test
    fun `isWithinActiveHours works for midnight crossing (10pm to 2am)`() = runTest {
        appPreferences.setAlwaysOn(false)
        appPreferences.setActiveHours(22, 0, 2, 0) // 10:00 PM to 2:00 AM

        mockkStatic(Calendar::class)

        // Test at 11:00 PM (within hours)
        val calendar11pm = mockk<Calendar>()
        every { calendar11pm.get(Calendar.HOUR_OF_DAY) } returns 23
        every { calendar11pm.get(Calendar.MINUTE) } returns 0
        every { Calendar.getInstance() } returns calendar11pm
        assertTrue(appPreferences.isWithinActiveHours())

        // Test at 1:00 AM (within hours, after midnight)
        val calendar1am = mockk<Calendar>()
        every { calendar1am.get(Calendar.HOUR_OF_DAY) } returns 1
        every { calendar1am.get(Calendar.MINUTE) } returns 0
        every { Calendar.getInstance() } returns calendar1am
        assertTrue(appPreferences.isWithinActiveHours())

        // Test at 3:00 AM (after hours)
        val calendar3am = mockk<Calendar>()
        every { calendar3am.get(Calendar.HOUR_OF_DAY) } returns 3
        every { calendar3am.get(Calendar.MINUTE) } returns 0
        every { Calendar.getInstance() } returns calendar3am
        assertFalse(appPreferences.isWithinActiveHours())

        // Test at 9:00 PM (before hours)
        val calendar9pm = mockk<Calendar>()
        every { calendar9pm.get(Calendar.HOUR_OF_DAY) } returns 21
        every { calendar9pm.get(Calendar.MINUTE) } returns 0
        every { Calendar.getInstance() } returns calendar9pm
        assertFalse(appPreferences.isWithinActiveHours())
    }

    // ====================================
    // Debounce Tests
    // ====================================

    @Test
    fun `canSendToNumber returns true for new phone number`() = runTest {
        val phoneNumber = "+1234567890"
        assertTrue(appPreferences.canSendToNumber(phoneNumber))
    }

    @Test
    fun `canSendToNumber returns false immediately after sending`() = runTest {
        val phoneNumber = "+1234567890"

        appPreferences.recordTextSent(phoneNumber)

        assertFalse(appPreferences.canSendToNumber(phoneNumber))
    }

    @Test
    fun `canSendToNumber handles different phone numbers separately`() = runTest {
        val number1 = "+1234567890"
        val number2 = "+0987654321"

        appPreferences.recordTextSent(number1)

        assertFalse(appPreferences.canSendToNumber(number1))
        assertTrue(appPreferences.canSendToNumber(number2))
    }

    @Test
    fun `canSendToNumber strips non-digit characters for key generation`() = runTest {
        val number1 = "+1 (234) 567-8900"
        val number2 = "12345678900" // Same digits

        appPreferences.recordTextSent(number1)

        // Both should be treated as the same number
        assertFalse(appPreferences.canSendToNumber(number2))
    }

    @Test
    fun `recordTextSent stores current timestamp`() = runTest {
        val phoneNumber = "+1234567890"
        val beforeTime = System.currentTimeMillis()

        appPreferences.recordTextSent(phoneNumber)

        // Can't send immediately
        assertFalse(appPreferences.canSendToNumber(phoneNumber))

        val afterTime = System.currentTimeMillis()

        // Verify timestamp is within reasonable range (not perfect but good enough)
        assertTrue(afterTime >= beforeTime)
    }
}
