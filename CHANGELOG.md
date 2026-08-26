# Changelog

All notable changes to My Cycle are documented in this file.

## 1.0.0 — 2026-08-26

### Added
- Period, mood, symptom, and notes logging.
- Calendar with confirmed period days and estimated fertile/ovulation windows.
- Adaptive cycle and period estimates based on recent completed cycles.
- Statistics for average cycle length, average period length, regularity, and recent cycle history.
- CSV export through the Android system file picker.
- System, light, dark, and dynamic-color appearance options.
- English and Russian localization.
- About section with description, author, version, license, privacy information, and GitHub link.
- Unit tests for cycle detection and prediction logic.

### Fixed
- First-run prediction no longer collapses the expected period length to one day.
- Cycle detection handles mid-cycle spotting without shifting the next cycle incorrectly.
- Today and Calendar use the same history-derived cycle rhythm as the prediction engine.
- Confirmed period days take precedence over calculated phase state.
- Calendar states are calculated for the month actually being viewed.
- Editing an existing day preserves its original creation timestamp.
- Future dates cannot be saved as actual logs.
- Clearing data resets onboarding correctly.
- Android 8.0 theme compatibility and Compose configuration-aware resource access.

### Privacy
- No internet permission, analytics, ads, or accounts.
- Android app-data backup and device-transfer extraction of tracked data are disabled.
- Data remains local unless the user explicitly exports a CSV file.

### Release
- Application ID: `com.silverlightning.mycycle`.
- License: PolyForm Noncommercial License 1.0.0.
