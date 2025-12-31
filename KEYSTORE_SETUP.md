# Keystore Setup Guide

This guide explains how to generate a signing keystore and configure the project to build signed release APKs.

## Why You Need a Keystore

Android requires all APKs to be digitally signed before they can be installed. For release builds (especially for Play Store distribution), you need to create your own keystore file.

## Step 1: Generate a Keystore

### Using Command Line

Run the following command in your terminal:

```bash
keytool -genkey -v -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release
```

### What This Does:

- **-genkey**: Creates a new key pair
- **-keystore release.jks**: Output file name (you can change this)
- **-keyalg RSA**: Uses RSA algorithm
- **-keysize 2048**: 2048-bit key (good balance of security and compatibility)
- **-validity 10000**: Valid for ~27 years
- **-alias release**: Key alias (you can change this)

### Interactive Prompts:

You'll be asked for:

1. **Keystore password**: Choose a strong password (remember this!)
2. **Key password**: Can be same as keystore password or different
3. **Name and organization details**: Fill in your information
4. **Confirmation**: Type "yes" when asked to confirm

### Example Session:

```
Enter keystore password: [your_password]
Re-enter new password: [your_password]
What is your first and last name?
  [Unknown]:  Your Name
What is the name of your organizational unit?
  [Unknown]:  Development
What is the name of your organization?
  [Unknown]:  MCTB
What is the name of your City or Locality?
  [Unknown]:  Your City
What is the name of your State or Province?
  [Unknown]:  Your State
What is the two-letter country code for this unit?
  [Unknown]:  US
Is CN=Your Name, OU=Development, O=MCTB, L=Your City, ST=Your State, C=US correct?
  [no]:  yes

Enter key password for <release>
        (RETURN if same as keystore password): [press Enter or type different password]
```

## Step 2: Create keystore.properties File

Create a file named `keystore.properties` in the project root directory:

```
storeFile=/absolute/path/to/your/release.jks
storePassword=your_keystore_password
keyAlias=release
keyPassword=your_key_password
```

### Important Notes:

- **Use absolute paths** for `storeFile` (e.g., `/home/user/mctb/release.jks`)
- **Don't commit** `keystore.properties` to version control
- The file is already listed in `.gitignore`

### Example keystore.properties:

```properties
storeFile=/home/user/mctb/release.jks
storePassword=myStrongPassword123
keyAlias=release
keyPassword=myStrongPassword123
```

## Step 3: Secure Your Keystore

### Critical Security Steps:

1. **Backup your keystore file** to a secure location
   - If you lose it, you cannot update your app on Play Store!
   - Store copies in multiple secure locations (encrypted USB drive, password manager, etc.)

2. **Never commit to version control**
   - `.gitignore` already excludes `*.jks` and `keystore.properties`
   - Verify: `git status` should not show these files

3. **Store passwords securely**
   - Use a password manager
   - Don't share passwords in plain text

4. **Restrict file permissions** (Linux/Mac):
   ```bash
   chmod 600 release.jks
   chmod 600 keystore.properties
   ```

## Step 4: Build Release APK

### Option 1: Command Line

```bash
# Clean build
./gradlew clean

# Build release APK
./gradlew assembleRelease
```

Output location: `app/build/outputs/apk/release/app-release.apk`

### Option 2: Android Studio

1. **Build → Generate Signed Bundle / APK**
2. Select **APK**
3. **Choose existing keystore** and browse to your `release.jks`
4. Enter passwords
5. Select **release** build variant
6. Click **Finish**

## Step 5: Verify Your APK

### Check Signing Info:

```bash
# Using apksigner (recommended)
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk

# Using jarsigner (alternative)
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

### Expected Output:

You should see:
- ✅ "Verified using v1 scheme (JAR signing)"
- ✅ "Verified using v2 scheme (APK Signature Scheme v2)"
- ✅ Certificate details matching your keystore info

## Troubleshooting

### "keystore.properties not found"

**Solution:** The build will succeed but APK won't be signed. Create the file as described in Step 2.

### "Keystore password incorrect"

**Solution:** Double-check your `keystore.properties` file for typos.

### "Key not found: release"

**Solution:** Verify the `keyAlias` in `keystore.properties` matches the alias you used when creating the keystore.

### "Cannot find keystore file"

**Solution:** Ensure `storeFile` path in `keystore.properties` is absolute and correct.

### Build succeeds but APK not signed

**Cause:** `keystore.properties` file not found or invalid.

**Solution:** Verify the file exists in project root and contains correct properties.

## Build Variants

The project has two build types:

### Debug Build (default)

- **Package:** `com.mctb.autoreply.debug`
- **Signed with:** Debug keystore (auto-generated)
- **Minified:** No
- **Use for:** Development and testing

```bash
./gradlew assembleDebug
```

### Release Build

- **Package:** `com.mctb.autoreply`
- **Signed with:** Your release keystore
- **Minified:** Yes (R8/ProGuard enabled)
- **Use for:** Production, Play Store

```bash
./gradlew assembleRelease
```

## Play Store Preparation

### First-Time Upload:

1. Generate keystore (this guide)
2. Build signed release APK
3. Upload APK to Play Console
4. Google manages signing key going forward (App Signing by Google Play)

### App Updates:

- Use the **same keystore** for all updates
- If you lose the keystore, you cannot update the app!
- Backup is critical

## CI/CD Integration

For automated builds, store secrets as environment variables:

```bash
export KEYSTORE_FILE="/path/to/release.jks"
export KEYSTORE_PASSWORD="your_password"
export KEY_ALIAS="release"
export KEY_PASSWORD="your_password"
```

Update `build.gradle.kts` to read from environment variables if `keystore.properties` doesn't exist.

## Additional Resources

- [Android: Sign your app](https://developer.android.com/studio/publish/app-signing)
- [keytool documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)
- [Play Console: App signing](https://support.google.com/googleplay/android-developer/answer/9842756)

---

**Remember: Your keystore is irreplaceable. Back it up securely!**
