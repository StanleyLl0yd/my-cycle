# Changelog

[![en](https://img.shields.io/badge/lang-en-red.svg)](CHANGELOG.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](CHANGELOG.ru.md)

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.3.0] - 2026-09-02

### 🔐 Privacy and access
- Added optional biometric app lock on Android 9+.
- Added optional screen protection that hides app content from screenshots and the recent-apps preview.
- Full restore deliberately leaves app lock disabled so a backup cannot lock the user out on another device; it can be enabled again manually after restore.

### 💾 Data portability
- Added CSV import with validation, duplicate-date preview and transactional merge; matching dates are replaced only after confirmation.
- Added a versioned `.mycycle` full backup format for diary records and app settings.
- Added full restore with rollback across Room and DataStore if the cross-store operation fails.
- CSV and backup files continue to be created and opened through Android's system file picker.

### 🔔 Reminders and quick access
- Added optional daily local reminders with configurable time and Android 13+ notification permission handling.
- Reminder notifications use neutral text and do not expose period dates or cycle status.
- Added a privacy-safe “Log today” launcher shortcut and home-screen widget that open today's diary entry directly.

### 🎨 Interface, icon and quality
- Replaced the launcher artwork with a warm adaptive cycle/calendar/petal icon without blood or medical imagery.
- Kept the app offline-first with no Android `INTERNET` permission, account, analytics or cloud synchronization.
- Added data-portability codec regression tests and kept debug/release builds, unit tests, Android Lint, Detekt and security scans in the release gate.
- Updated source version to 1.3.0 (`versionCode` 8).

---

## [1.1.5] - 2026-09-01

### 🛡️ Repository and release hardening
- Added the verified Gradle 9.5.0 Wrapper, including the official distribution SHA-256 checksum, so local and CI builds use the same pinned Gradle runtime.
- Switched Android CI, CodeQL, Qodana preparation, release builds and RuStore screenshot builds to `./gradlew`.
- The protected required `build` check now waits for CodeQL, Qodana, Semgrep and Gitleaks, so a security failure blocks the merge through the existing branch ruleset.
- The release workflow still waits for the required `build` check, which now represents the full quality and security gate.
- Updated GitHub Actions dependencies and KSP to 2.3.11 without weakening strict Gradle dependency verification.
- Cleaned stale maintenance branches.
- No user-facing behavior, UI/UX, persistence or application identity changes are intended.
- Updated source version to 1.1.5 (`versionCode` 7).

---

## [1.1.4] - 2026-09-01

### 🧹 Maintenance and code quality
- Completed a repository-wide cleanup and deep refactoring pass while preserving application behavior, UI/UX, data formats and the permanent Android identity.
- Removed unused Compose tooling dependencies, redundant calendar state and calculations, an unused range-observation API and unnecessary Activity lookup code.
- Simplified Room deletion by date and reused the existing atomic batch API for debug-only RuStore screenshot seed data.
- Stabilized Qodana Android project import without disabling strict Gradle dependency verification or lowering the quality gate.
- Kept prediction, history, attention-note, persistence and release-signing behavior unchanged.
- Updated source version to 1.1.4 (`versionCode` 6).

---

## [1.1.3] - 2026-08-31

### 📦 RuStore and application identity
- Fixed the canonical Android application ID and namespace at `com.sl.mycycle` for the first RuStore publication.
- Refactored Kotlin packages, source, test and debug package paths, Room schema paths, workflows and documentation to the canonical application identity.
- Pre-1.1.3 GitHub builds use a different Android application identity and are treated by Android as a separate app; `com.sl.mycycle` is the permanent identity going forward.
- Release publishing now produces a signed Android App Bundle (`.aab`), SHA-256 checksum and public upload certificate for RuStore.
- Added automated real-device-style 1080×1920 RuStore screenshots using a debug-only local demo-data seeder that is excluded from release builds.
- Added English and Russian privacy policies and a maintained RuStore listing with publication metadata and data-safety notes.
- Updated source version to 1.1.3 (`versionCode` 5).

---

## [1.1.2] - 2026-08-31

### 🗓️ Historical data and simpler entry
- Added a quick Calendar flow for entering previous periods by start and end date, with an “Add another” action for entering several earlier cycles.
- Existing mood, symptoms and notes are preserved when a historical period range is added.
- The day after a historical period is recorded as a non-bleeding boundary only when that day is safe to fill automatically, so finished period length can be calculated without overwriting user data.
- Past dates are explicitly discoverable as editable in Calendar.
- Daily entry now keeps bleeding and Save visible first, while mood, symptoms and notes live under an optional “More details” section.
- First-run questions and choices were shortened in English and Russian.

### 🛡️ Reliability, accessibility and release safety
- Historical period writes are atomic from the user’s perspective and roll back if a multi-day save fails.
- Onboarding, daily entry and destructive Settings operations now handle write failures and coroutine cancellation without leaving partial state across Room and DataStore.
- Calendar layout behaves better on smaller screens and with larger text; selection controls and Settings switches expose clearer accessibility semantics.
- Release publishing is immutable: it can only run from protected `main`, requires successful CI for the exact commit, refuses an existing tag/release and verifies the APK signer certificate and v2/v3 signatures.
- Regular CI never receives the release signing key or passwords; signing material is restored only for the signed release build and removed immediately afterward.

### ✅ Quality and supply-chain checks
- Android CI now runs build, unit tests, Android Lint and Detekt with strict Gradle dependency verification.
- Security workflows run Qodana, CodeQL, Semgrep and Gitleaks; Gitleaks checks full Git history.
- Dependabot monitors Gradle, GitHub Actions and the pinned Python security tool.
- Third-party GitHub Actions are pinned to immutable commit SHAs.
- Source-comment policy is enforced automatically: comments must be minimal, necessary and English-only.
- Release builds remain minified with R8 and resource shrinking.
- Updated source version to 1.1.2 (`versionCode` 4).

---

## [1.1.1] - 2026-08-27

### ✅ Safer setup and records
- First setup now requires the user to choose the situation that fits before continuing. It no longer silently assumes a steady adult cycle.
- The “periods stopped for a full year” option now clearly says it is for age-related changes or a situation already explained by a doctor.
- An unexplained gap of about 3 months in an adult cycle can now show a simple note to talk with a doctor.
- The single start date entered during first setup no longer becomes a made-up 1-day period. Its bleeding length stays unknown until real bleeding days are recorded.
- A recorded “No blood” day or “A few spots” now breaks a period episode. A completely unrecorded day between nearby period days can still be treated as a missed diary entry.
- Old 1.0.0 spotting records that were stored as period days are normalized and no longer act like real period bleeding.

### 📅 Clearer estimates
- “The gaps change a lot” is now separate from “the gaps are fairly steady but usually shorter or longer than the common range”.
- Fairly steady adult cycles outside the common range get a wider estimate and no egg-release estimate, without being incorrectly described as highly variable.
- The rough pregnancy-likelihood range now uses the five days before the possible egg-release range through that range itself, without adding an extra day after it.
- Date calculations use an injected clock and screens update when the calendar day changes instead of keeping yesterday until the app is restarted.

### 📊 Statistics and calendar
- Summary numbers now use up to the 6 most recent finished cycles; up to 12 recent cycles remain visible in history.
- History stays visible even when there is not enough information for averages.
- Unknown bleeding length is shown as unknown instead of being invented.
- Calendar week layout now follows the device language/region.
- Calendar days now provide spoken descriptions for TalkBack, including recorded bleeding, spotting and estimated dates.
- Pregnancy/egg-release legend items are hidden when those estimates are not being shown.

### 🎨 Interface and reliability
- Fixed Dark theme backgrounds so dark mode is actually dark.
- Improved text and control contrast, including selected bleeding options.
- Fixed the setup date picker so dates do not move by one day in some time zones.
- CSV file writing now runs off the main UI thread and explicitly uses UTF-8.

### 🧹 Code and tests
- Removed the unused old cycle-phase calculator and its fixed “cycle length minus 14” logic.
- Removed unused phase models, old strings, future-feature strings, Kotlin Serialization dependency and unnecessary R8 keep rules.
- Simplified CI by removing the unused signing-tools artifact.
- Simplified branch cleanup so it only deletes the branch of a successfully merged pull request.
- Added regression tests for spotting migration, explicit no-blood days, unknown first-period length, long gaps, stable-but-outside-range cycles and other review findings.
- Updated source version to 1.1.1 (`versionCode` 3).

---

## [1.1.0] - 2026-08-26

### ✨ Added
- A simple setup choice for six common situations: periods started less than 1 year ago, 1–3 years ago, more than 3 years ago with similar gaps, more than 3 years ago with very uneven gaps, periods changing with age, or no period for 12 months or more.
- The same choice can be changed later in Settings without deleting old records.
- Existing 1.0.0 installations keep all history and start in a conservative “not chosen yet” mode until the user selects the new option once in Settings.
- Clear notes when a very long gap, longer-than-usual bleeding, or bleeding after about a year without periods deserves attention.
- If periods have stopped for 12 months or more, setup no longer asks for or creates a made-up recent period date.

### 📅 Better date estimates
- The app now shows a **range of possible dates** for the next period instead of pretending to know one exact day.
- The first years after periods begin use a wider date range because uneven timing is common.
- Long-term uneven cycles and periods changing with age also use a wider range and do not get an egg-release guess.
- The migration-only “not chosen yet” mode also uses a wide range and never shows an egg-release or pregnancy-likelihood estimate.
- Only a fairly steady adult history with at least 3 finished cycles can show a broad calendar range where pregnancy may be more likely.
- The app never presents those calendar dates as safe birth-control days.
- Up to the 6 most recent finished cycles are still used, with newer dates counting more.

### 🩸 Bleeding records
- “A few spots” is kept separate from a real period and does not start a new cycle.
- A short gap between real periods is no longer silently hidden. The app keeps what the user actually recorded.
- In the first years, a gap of about 3 months or more can trigger a simple note to seek advice.
- Bleeding that continues longer than the usual range can trigger a simple attention note.
- New bleeding after about a year without a period triggers a clear note to contact a doctor.

### 🗣️ Clearer language
- Removed technical cycle-stage words from the main screens.
- Replaced claims such as an exact ovulation day or “peak fertility” with plain explanations of what the calendar can and cannot know.
- Reworked Today, Calendar, Statistics, Settings, setup and daily-log explanations so they can be understood without medical knowledge.
- Statistics describe early-years, long-term uneven and age-changing patterns without turning them into a diagnosis.

### 🧪 Tests & Build
- Added tests for wide early-year estimates, long-term uneven cycles, age-changing cycles, stopped-period mode, migration-safe unset mode, spotting, short cycles and range-based predictions.
- Updated source version to 1.1.0 (`versionCode` 2).

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
- After completed cycles are available, predictions use up to the 6 most recent completed cycles with greater weight given to newer data.
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
- Application ID: `com.sl.mycycle`.
- Min SDK: 26 (Android 8.0).
- Target SDK: 36.

### 📚 Documentation
- Added English and Russian project documentation.
- Added installation, build, privacy, medical disclaimer and contribution information.
- Added PolyForm Noncommercial License 1.0.0 information and author attribution to **Stanley Lloyd**.
