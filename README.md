# Missed Call Auto-Reply (MCTB)

A simple Android app that automatically sends SMS replies when you miss a phone call. Designed for tradespeople and busy professionals who can't afford to lose leads.

## What It Does

When someone calls you and you don't answer:
- The app detects the missed call
- Instantly sends a predefined SMS to that caller
- Only works during your configured work hours
- Prevents awkward silence and keeps leads warm

**That's it.** No cloud, no login, no complexity.

## Key Features

✅ **Auto-SMS on Missed Calls** - Instant text reply when you can't answer
✅ **Work Hours** - Set active hours (e.g., 8 AM - 6 PM) or always-on
✅ **Custom Message** - Edit your auto-reply text
✅ **Free Tier** - 5 free auto-texts, then upgrade
✅ **Smart Debounce** - Won't spam the same number (30-min cooldown)
✅ **Background Service** - Works even when app is closed
✅ **No Backend** - Everything runs locally on your device

## Screenshots

*(Coming soon)*

## Requirements

- Android 8.0 (Oreo) or higher
- Permissions:
  - Read phone state
  - Read call log
  - Send SMS
  - Run in background
  - Disable battery optimization

## Installation

### Option 1: Build from Source

1. Clone this repository:
   ```bash
   git clone https://github.com/blunts954-png/mctb.git
   cd mctb
   ```

2. Open in Android Studio:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory
   - Wait for Gradle sync to complete

3. Build and run:
   - Connect your Android device or start an emulator
   - Click "Run" (▶️) in Android Studio
   - Select your device

### Option 2: Install APK

*(Coming soon - APK releases will be available in GitHub Releases)*

## How to Use

### First-Time Setup

1. **Grant Permissions**
   - On first launch, the app will request necessary permissions
   - Tap "Grant Permissions" and allow all requested permissions
   - **Important:** Disable battery optimization when prompted (ensures the app works reliably)

2. **Configure Your Message**
   - Tap "Edit Message"
   - Customize your auto-reply text (max 160 characters)
   - Default: *"Hi! I missed your call and I'm working right now. I'll call you back as soon as I can. Thanks!"*
   - Tap "Save"

3. **Set Active Hours**
   - Tap "Active Hours"
   - Choose your work hours (e.g., 8:00 AM - 6:00 PM)
   - Or toggle "Always On" for 24/7 coverage
   - Tap "Save"

4. **Enable Auto-Reply**
   - Return to home screen
   - Toggle the "Enable Auto-Reply" switch **ON**
   - You'll see "Auto-reply active during work hours"

### Daily Use

- **That's it!** Just leave the app enabled
- The app runs in the background
- When you miss a call, it automatically sends your message
- Check "Usage" to see how many auto-texts you've sent

### Free Tier & Upgrade

- You get **5 free auto-texts**
- After that, the app will stop sending until you upgrade
- Tap "Usage" → "Upgrade to Unlimited" to remove the limit
- *(Note: Payment integration coming soon - currently demo mode)*

## Technical Architecture

### Core Components

1. **CallReceiver** (`BroadcastReceiver`)
   - Listens for `PHONE_STATE` changes
   - Detects missed calls (RINGING → IDLE without OFFHOOK)
   - Triggers auto-reply logic

2. **CallMonitorService** (`Foreground Service`)
   - Keeps the app alive in the background
   - Shows persistent notification
   - Survives battery optimization

3. **SmsSender**
   - Validates conditions before sending
   - Handles SMS via Android's `SmsManager`
   - Implements debounce logic (30-min cooldown per number)

4. **AppPreferences** (`SharedPreferences`)
   - Stores all settings locally
   - No cloud sync, no accounts
   - Includes: message, hours, usage count, debounce timestamps

### Permission Handling

The app requires several permissions:

- `READ_PHONE_STATE` - Detect incoming calls
- `READ_CALL_LOG` - Identify missed calls
- `SEND_SMS` - Send auto-reply texts
- `RECEIVE_BOOT_COMPLETED` - Restart service after reboot
- `FOREGROUND_SERVICE` - Run background service
- `POST_NOTIFICATIONS` - Show service notification (Android 13+)
- Battery optimization exemption - Prevent Android from killing the service

### Edge Cases Handled

✅ Unknown/private numbers → Skipped
✅ Repeated calls from same number → 30-min debounce
✅ User declines call → Still treated as missed (sends text)
✅ Outside active hours → No text sent
✅ App disabled → No text sent
✅ Free tier limit reached → Auto-reply stops until upgrade

## Project Structure

```
app/src/main/
├── java/com/mctb/autoreply/
│   ├── MainActivity.kt              # Home screen with toggle
│   ├── MessageEditorActivity.kt     # Edit auto-reply message
│   ├── ActiveHoursActivity.kt       # Set work hours
│   ├── UsageActivity.kt             # View usage & upgrade
│   ├── CallReceiver.kt              # Detects missed calls
│   ├── CallMonitorService.kt        # Foreground service
│   ├── SmsSender.kt                 # Sends SMS
│   ├── AppPreferences.kt            # Local data storage
│   └── BootReceiver.kt              # Restart on boot
├── res/
│   ├── layout/                      # UI layouts
│   ├── values/                      # Strings, colors, themes
│   └── xml/                         # Backup rules
└── AndroidManifest.xml              # Permissions & components
```

## Development

### Building

```bash
./gradlew assembleDebug
```

### Running Tests

```bash
./gradlew test
```

### Creating a Release Build

1. Generate a keystore (first time only):
   ```bash
   keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mctb
   ```

2. Build release APK:
   ```bash
   ./gradlew assembleRelease
   ```

## Roadmap

**v1.0 (Current MVP)**
- ✅ Missed call detection
- ✅ Auto-SMS reply
- ✅ Work hours configuration
- ✅ Free tier (5 texts)
- ✅ Background service

**v1.1 (Planned)**
- [ ] Google Play Billing integration for upgrade
- [ ] Better UI/UX polish
- [ ] Dark mode
- [ ] Call history log (who was texted when)
- [ ] Quick reply templates

**v1.2 (Future)**
- [ ] Multiple phone number support (dual SIM)
- [ ] Customizable debounce window
- [ ] Statistics dashboard
- [ ] Backup/restore settings

## FAQ

**Q: Does this answer calls for me?**
A: No. It only detects missed calls and sends a text.

**Q: Does it work when my phone is off?**
A: No. The phone must be on and the app must be running.

**Q: What if I manually decline a call?**
A: It's still treated as a missed call, so a text will be sent.

**Q: Will this drain my battery?**
A: Minimal impact. The app uses an event-driven approach (no polling) and only activates when calls occur.

**Q: Is my data sent anywhere?**
A: No. Everything is stored locally. No cloud, no tracking, no analytics.

**Q: Can I use this for spam filtering?**
A: Not recommended. The app texts every missed call during active hours (except debounced numbers).

## Troubleshooting

**Auto-reply not working:**
1. Check that all permissions are granted (Settings → Apps → Missed Call Auto-Reply → Permissions)
2. Disable battery optimization (Settings → Battery → Battery Optimization → Missed Call Auto-Reply → Don't optimize)
3. Ensure the toggle is ON in the app
4. Check that you're within active hours (or "Always On" is enabled)
5. Verify you haven't reached the 5-text free tier limit

**Service keeps stopping:**
- Some manufacturers (Xiaomi, Huawei, OnePlus) have aggressive battery management
- Add the app to the "Protected Apps" list in your device settings
- Disable any "Battery Saver" or "Power Saving" modes for this app

## Privacy & Security

- **No data collection**: We don't collect, store, or transmit any data
- **Local-only**: All settings and logs stay on your device
- **No analytics**: No Google Analytics, Firebase, or any tracking
- **No ads**: This is a clean, ad-free tool
- **Open source**: Code is public for full transparency

## License

MIT License - see [LICENSE](LICENSE) file for details

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Support

For issues, questions, or feature requests:
- Open an issue on GitHub
- Email: support@mctb.app *(coming soon)*

## Credits

Built with:
- Kotlin
- Android SDK
- Material Design Components

---

**Made for tradespeople who can't afford to miss a lead.**
