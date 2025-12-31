# Privacy Policy for MCTB Auto-Reply

**Last Updated:** January 15, 2025

## Overview

MCTB Auto-Reply ("the App") is designed with privacy as a core principle. This Privacy Policy explains how the App handles your information.

## Information Collection and Use

### What We DO Collect

The App stores the following information **locally on your device only**:

1. **App Settings:**
   - Enable/disable toggle state
   - Custom auto-reply message
   - Active hours configuration
   - Auto-text usage counter

2. **Debounce Tracking:**
   - Timestamps of when auto-replies were sent to specific phone numbers
   - Used solely to prevent sending duplicate messages within 30 minutes

### What We DO NOT Collect

The App does **NOT** collect, transmit, or share:

- ❌ Phone numbers or contact information
- ❌ Call logs or call history
- ❌ SMS message contents
- ❌ Personal identification information
- ❌ Device identifiers or advertising IDs
- ❌ Location data
- ❌ Analytics or usage statistics
- ❌ Crash reports or diagnostics

## Data Storage

- **All data is stored locally** on your device using Android DataStore
- **No cloud storage** or remote servers are used
- **No network connections** are made by the App
- Data persists until you uninstall the App or clear app data

## Permissions Required

The App requires the following permissions to function:

### Essential Permissions

1. **READ_PHONE_STATE**
   - Purpose: Detect when phone calls are ringing, answered, or missed
   - Usage: Monitor call state transitions to identify missed calls

2. **READ_CALL_LOG**
   - Purpose: Access call information (phone number)
   - Usage: Retrieve the phone number of missed calls

3. **SEND_SMS**
   - Purpose: Send automatic text message replies
   - Usage: Send your configured auto-reply message to missed callers

4. **RECEIVE_BOOT_COMPLETED**
   - Purpose: Auto-start the service after device reboot
   - Usage: Restore auto-reply functionality if enabled before reboot

5. **FOREGROUND_SERVICE + FOREGROUND_SERVICE_PHONE_CALL**
   - Purpose: Run reliably in the background
   - Usage: Maintain call monitoring service

6. **POST_NOTIFICATIONS**
   - Purpose: Display persistent service notification
   - Usage: Show that the auto-reply service is active

7. **REQUEST_IGNORE_BATTERY_OPTIMIZATIONS**
   - Purpose: Request exemption from aggressive battery optimization
   - Usage: Ensure the service isn't killed by system battery management

## Data Sharing

**The App does not share any data with third parties.**

- No advertising networks
- No analytics providers
- No cloud services
- No social media integrations
- No data brokers

## Data Security

- All data is protected by Android's app sandboxing
- Data is only accessible by the App itself
- No encryption is needed as no sensitive data is stored permanently
- Phone numbers in debounce tracking are stored as hashed keys (digits-only)

## Children's Privacy

The App does not knowingly collect information from children under 13. The App is intended for general audiences and does not target children.

## Data Retention

- Settings and preferences: Retained until you change them or uninstall the App
- Debounce timestamps: Retained for 30 minutes after sending an auto-reply, then become irrelevant (not actively deleted but overwritten on next use)

## Data Deletion

To delete all data stored by the App:

**Option 1: Clear App Data**
1. Go to Settings → Apps → MCTB Auto-Reply
2. Tap "Storage"
3. Tap "Clear Data"

**Option 2: Uninstall**
1. Uninstalling the App automatically deletes all stored data
2. No data remains on device or any servers after uninstallation

## Third-Party Services

The App does **NOT** use any third-party services, including:

- No Google Analytics
- No Firebase Analytics or Crashlytics
- No advertising SDKs
- No social media SDKs
- No cloud storage providers

## Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Changes will be posted in this document with an updated "Last Updated" date. Continued use of the App after changes constitutes acceptance of the updated policy.

## Open Source

The App is open source under the MIT License. You can review the source code to verify these privacy claims at:

[GitHub Repository URL - Add your repository URL here]

## Contact

If you have questions about this Privacy Policy or the App's privacy practices, please contact:

**Email:** [Your contact email]
**GitHub Issues:** [Your repository issues URL]

## Legal Compliance

This App complies with:

- Google Play Store policies
- Android app privacy requirements
- General Data Protection Regulation (GDPR) principles
- California Consumer Privacy Act (CCPA) principles

## Your Rights

You have the right to:

- Access your data (stored locally on your device)
- Delete your data (via app settings or uninstall)
- Disable the App at any time
- Uninstall the App without any data retention

## Consent

By using the App, you consent to this Privacy Policy and the processing of data as described herein.

---

**MCTB Auto-Reply is committed to protecting your privacy.**
**No data leaves your device. Ever.**
