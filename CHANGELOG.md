# Changelog

All notable changes to the MCTB Auto-Reply Android application will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-01-15

### Production-Ready Release

This is the first production-ready release of MCTB Auto-Reply, featuring a complete rebuild with modern Android architecture and Material 3 design.

### Added

**Core Features:**
- Automatic SMS replies to missed calls
- Customizable auto-reply message (160 character limit)
- Active hours configuration (work hours or 24/7 mode)
- Smart debounce system (30-minute cooldown per number)
- Free tier with 5 auto-texts limit
- Unknown/private/blocked caller filtering
- Persistent foreground service for reliability
- Auto-restart after device reboot
- Battery optimization guidance

**User Interface:**
- Material 3 design with dynamic color theming
- Dark mode support (follows system settings)
- 4 main screens: Home, Message Editor, Active Hours, Usage
- Intuitive time pickers for active hours
- Real-time usage counter with visual progress
- Permission request flows with clear explanations

**Technical Infrastructure:**
- Production-ready build configuration with R8/ProGuard
- Comprehensive unit tests for core logic
- Timber logging framework (debug and production modes)
- Improved error handling throughout the app
- DataStore for reactive state management
- Jetpack Compose for modern UI
- Coroutines for async operations

**Documentation:**
- Comprehensive README with architecture details
- Build notes and setup instructions
- Privacy policy for Play Store compliance
- Keystore setup guide for release signing
- Inline code documentation

### Technical Specifications

- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 35
- **Kotlin:** 2.0.21
- **Gradle:** 8.9
- **AGP:** 8.7.0

### Dependencies

- Jetpack Compose BOM 2024.12.01
- Material 3
- Navigation Compose 2.8.5
- DataStore Preferences 1.1.1
- Coroutines 1.9.0
- Accompanist Permissions 0.36.0
- Timber 5.0.1

### Security & Privacy

- Minimal permissions (only required for core functionality)
- No analytics or tracking
- All data stored locally
- No network connections
- Open source MIT license

### Known Limitations

- English language only (i18n infrastructure ready)
- Single SIM support only
- Payment integration placeholder (no real payments)
- Manual battery optimization required
- Depends on device call state broadcasts (may vary by manufacturer)

### Future Enhancements

Potential features for future releases:
- Multi-language support (i18n)
- Dual-SIM support
- Cloud backup and sync
- Firebase Crashlytics integration
- Advanced scheduling rules
- Custom replies per contact
- Message templates

---

## Version History

- **1.0.0** - Production-ready release with full feature set
- **0.1.0** - Initial MVP implementation (December 2024)

---

For detailed development notes, see BUILD_NOTES.md
For usage instructions, see README.md
