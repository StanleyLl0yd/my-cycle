# Changelog

[![en](https://img.shields.io/badge/lang-en-red.svg)](CHANGELOG.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](CHANGELOG.ru.md)

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-08-26

### ✨ Added
- Initial setup with the first day of the most recent period and usual cycle length from 21 to 45 days.
- Today screen with cycle day, calculated phase, estimated next period and fertile-window status.
- Daily logging for menstrual flow, mood, symptoms and notes.
- Calendar with confirmed period days, estimated next-period days, fertile window and ovulation day.
- Statistics for average cycle length, average period length, cycle regularity and recent cycle history.
- CSV export through the Android system file picker.
- Complete deletion of cycle data and app preferences.
- System, Light and Dark themes with Material You dynamic colors on Android 12+.
- English and Russian localization.
- About section with description, author, version, PolyForm license, privacy information and GitHub link.

### 📅 Cycle estimates
- Initial estimates use the most recent period start date, the selected cycle length and a 5-day initial period length.
- After completed cycles are available, predictions use up to the 6 most recent completed cycles with greater weight given to newer cycles.
- Estimated ovulation is calculated 14 days before the estimated next period.
- The estimated fertile window runs from 5 days before ovulation through 1 day after it.

### 🔒 Privacy
- No Android `INTERNET` permission.
- No account, analytics, tracking SDKs or ads.
- Cycle data and preferences remain local unless the user explicitly creates a CSV export.
- Android app-data backup is disabled.
- App files, databases and preferences are excluded from cloud backup and device-transfer extraction rules.

### 🧪 Tests & Build
- Added unit tests for cycle detection and prediction logic.
- Added GitHub Actions validation for unit tests, Android Lint, debug APK assembly and release APK assembly.
- Release builds use R8 minification and resource shrinking.
- Application ID: `com.silverlightning.mycycle`.
- Min SDK: 26 (Android 8.0).
- Target SDK: 36.

### 📚 Documentation
- Added English and Russian project documentation.
- Added installation, build, privacy, medical disclaimer and contribution information.
- Added PolyForm Noncommercial License 1.0.0 information and author attribution to **Stanley Lloyd**.
