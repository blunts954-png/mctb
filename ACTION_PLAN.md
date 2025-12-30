# Production Readiness - Action Plan
**Priority-Ordered Task List**

> See [PRODUCTION_READINESS.md](./PRODUCTION_READINESS.md) for detailed analysis

---

## 🔴 CRITICAL - Must Fix Before ANY Release

### 1. Release Build Configuration
**Effort:** 2-4 hours
```bash
# Generate keystore
keytool -genkey -v -keystore release.keystore -alias mctb-release \
  -keyalg RSA -keysize 2048 -validity 10000

# Store credentials in key.properties (DON'T COMMIT)
echo "KEYSTORE_FILE=release.keystore" > key.properties
echo "KEYSTORE_PASSWORD=your_password" >> key.properties
echo "KEY_ALIAS=mctb-release" >> key.properties
echo "KEY_PASSWORD=your_password" >> key.properties
```

**Files to modify:**
- `app/build.gradle.kts` - Add signingConfigs
- `.gitignore` - Add `*.keystore` and `key.properties`

---

### 2. Enable ProGuard/R8
**Effort:** 4-6 hours (including testing)

**Changes needed:**
```kotlin
// app/build.gradle.kts
buildTypes {
    release {
        isMinifyEnabled = true  // ✅ Change this
        isShrinkResources = true  // ✅ Add this
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

**Update `proguard-rules.pro`:**
```proguard
# Keep source file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# DataStore
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class androidx.datastore.*.** { *; }

# Navigation
-keep class androidx.navigation.fragment.NavHostFragment { *; }

# Keep application class
-keep class com.mctb.autoreply.** { *; }
```

**Testing Required:**
- [ ] Build release APK
- [ ] Install and test on device
- [ ] Verify all features work
- [ ] Check crash reports show correct line numbers

---

### 3. Add Gradle Wrapper JAR
**Effort:** 5 minutes
```bash
gradle wrapper --gradle-version 8.9
git add gradle/wrapper/gradle-wrapper.jar
git commit -m "Add gradle wrapper JAR for build reproducibility"
```

---

### 4. Write Privacy Policy
**Effort:** 2-3 hours

**Create file:** `privacy-policy.html`

**Host on:** GitHub Pages or your website

**Required sections:**
1. What data is collected (phone numbers, usage stats)
2. How data is used (send SMS, track usage)
3. Data storage (local device only, never sent to servers)
4. No third-party sharing
5. User rights (delete data = uninstall app)
6. Contact information

**Template:**
```markdown
# Privacy Policy for Auto-Reply

Last Updated: [DATE]

## Data Collection
- Phone numbers of missed callers (stored locally only)
- SMS usage count (stored locally only)
- App settings (stored locally only)

## Data Usage
- Phone numbers are used ONLY to send auto-reply SMS
- Usage count tracks free tier limit
- No data is transmitted to servers
- No analytics or tracking

## Data Storage
- All data stored on your device using Android DataStore
- No cloud storage
- No server communication

## Data Sharing
- We do not share any data with third parties
- No advertising networks
- No analytics services

## Your Rights
- Delete all data by uninstalling the app
- Data is local to your device only

## Contact
Email: [your-email@domain.com]
```

**Add to app:** Link in settings screen

---

### 5. Integrate Firebase Crashlytics
**Effort:** 2-3 hours

**Step 1:** Add Firebase to project
```bash
# Download google-services.json from Firebase Console
# Place in app/ directory
```

**Step 2:** Update `build.gradle.kts`
```kotlin
// build.gradle.kts (project level)
plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}

// app/build.gradle.kts
plugins {
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
}
```

**Step 3:** Add crash logging
```kotlin
// In SmsHandler.kt
catch (e: Exception) {
    FirebaseCrashlytics.getInstance().apply {
        setCustomKey("phone_number_valid", isValidPhoneNumber(phoneNumber))
        setCustomKey("app_enabled", prefs.isEnabledSync())
        recordException(e)
    }
    Log.e(TAG, "Error sending SMS", e)
}
```

---

### 6. Create App Icon
**Effort:** 2-4 hours

**Tools:**
- Use Android Studio Image Asset wizard
- OR use online tool: https://romannurik.github.io/AndroidAssetStudio/

**Requirements:**
- Foreground layer (108x108dp safe zone)
- Background layer (solid color or gradient)
- No text in icon (accessibility)
- Simple, recognizable design

**Deliverables:**
- `res/mipmap-*/ic_launcher.png` (all densities)
- `res/mipmap-*/ic_launcher_round.png`
- `res/mipmap-anydpi-v26/ic_launcher.xml`

**Also create for Play Store:**
- 512x512 PNG (no transparency)

---

### 7. Write Critical Tests
**Effort:** 8-10 hours

**Test Files to Create:**

**`app/src/test/java/com/mctb/autoreply/AppPreferencesTest.kt`:**
```kotlin
@Test
fun `debounce prevents duplicate SMS within 30 minutes`() = runTest {
    // Test implementation
}

@Test
fun `free tier limit enforced correctly`() = runTest {
    // Test implementation
}

@Test
fun `active hours validation works correctly`() = runTest {
    // Test active hours logic
}
```

**`app/src/test/java/com/mctb/autoreply/SmsHandlerTest.kt`:**
```kotlin
@Test
fun `invalid phone numbers are rejected`() {
    val handler = SmsHandler(context)
    assertFalse(handler.isValidPhoneNumber("-1"))
    assertFalse(handler.isValidPhoneNumber("UNKNOWN"))
    assertTrue(handler.isValidPhoneNumber("1234567890"))
}
```

**Add test dependencies:**
```kotlin
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
}
```

**Run tests:**
```bash
./gradlew test
```

---

### 8. Play Store Listing Assets
**Effort:** 4-6 hours

**Screenshots (minimum 2, ideally 4):**
1. Home screen with app enabled
2. Message editor screen
3. Active hours configuration
4. Usage/upgrade screen

**Use:**
- Real device or emulator
- Clean UI (no debug data)
- Representative content

**Other assets:**
- Feature graphic (1024x500)
- Short description (80 chars max)
- Full description (up to 4000 chars)
- App category: Tools or Productivity
- Content rating: Everyone

---

## 🟡 HIGH PRIORITY - Should Fix Before Release

### 9. Production Logging Strategy
**Effort:** 2-3 hours

**Create:** `app/src/main/java/com/mctb/autoreply/util/Logger.kt`
```kotlin
object Logger {
    private const val ENABLED = BuildConfig.DEBUG

    fun d(tag: String, message: String) {
        if (ENABLED) Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        if (ENABLED) Log.i(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (ENABLED) Log.e(tag, message, throwable)
        // Always report errors to Crashlytics
        throwable?.let { FirebaseCrashlytics.getInstance().recordException(it) }
    }

    // NEVER log phone numbers in production
    fun dSafe(tag: String, message: String) {
        if (ENABLED) Log.d(tag, message)
        // Production: Log sanitized version
        else Log.d(tag, message.replace(Regex("\\d{3,}"), "***"))
    }
}
```

**Replace all `Log.*` calls with `Logger.*`**

---

### 10. SMS Delivery Tracking
**Effort:** 3-4 hours

**Update `SmsHandler.sendSms()`:**
```kotlin
private fun sendSms(phoneNumber: String, message: String): Boolean {
    return try {
        val sentIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent("SMS_SENT"),
            PendingIntent.FLAG_IMMUTABLE
        )

        val deliveredIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent("SMS_DELIVERED"),
            PendingIntent.FLAG_IMMUTABLE
        )

        smsManager.sendTextMessage(
            phoneNumber,
            null,
            message,
            sentIntent,     // Track send status
            deliveredIntent // Track delivery
        )

        true
    } catch (e: Exception) {
        FirebaseCrashlytics.getInstance().recordException(e)
        false
    }
}
```

**Create receiver for SMS status:**
```kotlin
class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                // SMS sent successfully
            }
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                // Show notification to user
            }
        }
    }
}
```

---

### 11. Set Up CI/CD
**Effort:** 2-3 hours

**Create:** `.github/workflows/android.yml`
```yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build debug APK
      run: ./gradlew assembleDebug

    - name: Run unit tests
      run: ./gradlew test

    - name: Upload build artifacts
      uses: actions/upload-artifact@v4
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

---

### 12. Firebase Analytics
**Effort:** 2 hours

**Already added with Crashlytics**, just add events:

```kotlin
// In HomeScreen.kt when enabling
FirebaseAnalytics.getInstance(context).logEvent("auto_reply_enabled", null)

// In SmsHandler.kt when SMS sent
FirebaseAnalytics.getInstance(context).logEvent("sms_sent", bundleOf(
    "source" to "missed_call"
))

// When limit reached
FirebaseAnalytics.getInstance(context).logEvent("free_tier_limit_reached", null)
```

---

## 🟠 MEDIUM PRIORITY - Can Fix Post-Launch

### 13. Implement Upgrade Flow
**Effort:** 1-2 days

**Add Google Play Billing:**
```kotlin
dependencies {
    implementation("com.android.billingclient:billing-ktx:6.1.0")
}
```

### 14. Backup/Restore Verification
**Effort:** 2-3 hours

**Verify DataStore backup:**
```xml
<!-- res/xml/backup_rules.xml -->
<full-backup-content>
    <include domain="file" path="datastore/" />
</full-backup-content>
```

### 15. Accessibility Audit
**Effort:** 3-4 hours

- Test with TalkBack
- Add missing content descriptions
- Test with large fonts
- Verify color contrast

---

## Timeline Summary

**Week 1: Critical Fixes**
- Days 1-2: Signing, ProGuard, Gradle wrapper
- Days 3-4: Privacy policy, Play Store assets, app icon
- Days 5-7: Crashlytics, logging, tests

**Week 2: High Priority**
- Days 1-2: SMS tracking, CI/CD
- Days 3-4: Analytics, manual QA
- Days 5-7: Bug fixes, polish

**Week 3: Testing & Submission**
- Days 1-3: Beta testing with 10-20 users
- Days 4-5: Address beta feedback
- Days 6-7: Final QA, Play Store submission

**Week 4: Post-Submission**
- Monitor for crashes
- Respond to reviews
- Plan first update

---

## Quick Start Commands

```bash
# 1. Set up Gradle wrapper
gradle wrapper --gradle-version 8.9

# 2. Generate keystore
keytool -genkey -v -keystore release.keystore -alias mctb-release \
  -keyalg RSA -keysize 2048 -validity 10000

# 3. Create key.properties
cat > key.properties <<EOF
KEYSTORE_FILE=release.keystore
KEYSTORE_PASSWORD=YOUR_PASSWORD
KEY_ALIAS=mctb-release
KEY_PASSWORD=YOUR_PASSWORD
EOF

# 4. Update .gitignore
echo "*.keystore" >> .gitignore
echo "key.properties" >> .gitignore

# 5. Build release
./gradlew assembleRelease

# 6. Run tests
./gradlew test

# 7. Check APK size
ls -lh app/build/outputs/apk/release/app-release.apk
```

---

## Success Metrics

**Before submitting to Play Store, verify:**
- [x] App builds successfully in release mode
- [x] ProGuard enabled and tested
- [x] App signed with release keystore
- [x] Tests run and pass (`./gradlew test`)
- [x] Crashlytics integrated and reporting
- [x] Privacy policy published and linked
- [x] Play Store listing complete
- [x] App tested on 3+ devices
- [x] App tested on Android 8.0 (min SDK)
- [x] App tested on Android 15 (target SDK)
- [x] No phone numbers in production logs
- [x] APK size < 10MB
- [x] No critical bugs found in testing
- [x] Battery optimization tested
- [x] Reboot persistence tested
- [x] SMS actually sends successfully
- [x] All permissions work correctly

---

## Next Steps

1. **Read:** [PRODUCTION_READINESS.md](./PRODUCTION_READINESS.md) for detailed analysis
2. **Start:** With CRITICAL items (1-8 above)
3. **Test:** After each change
4. **Commit:** Frequently with clear messages
5. **Deploy:** When all CRITICAL items are done

**Questions?** Review the detailed analysis in `PRODUCTION_READINESS.md`
