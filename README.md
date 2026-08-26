# My Cycle

[![Android CI](https://github.com/StanleyLl0yd/my-cycle/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/StanleyLl0yd/my-cycle/actions/workflows/ci.yml)
[![Latest release](https://img.shields.io/github/v/release/StanleyLl0yd/my-cycle)](https://github.com/StanleyLl0yd/my-cycle/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/StanleyLl0yd/my-cycle/total)](https://github.com/StanleyLl0yd/my-cycle/releases)
[![Android 8+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://github.com/StanleyLl0yd/my-cycle/releases/latest)
[![License](https://img.shields.io/badge/license-PolyForm%20Noncommercial%201.0.0-blue)](LICENSE)

[![en](https://img.shields.io/badge/lang-en-red.svg)](README.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](README.ru.md)

A private, offline menstrual cycle tracker for Android, built with Kotlin, Jetpack Compose and Material 3.

[⬇️ Download the latest APK](https://github.com/StanleyLl0yd/my-cycle/releases/latest)

Current version: **1.0.0** · Min SDK: **26 (Android 8.0)** · Target SDK: **36**

## ✨ Features

- Initial setup with the first day of the most recent period and usual cycle length from 21 to 45 days
- Today screen with cycle day, calculated phase, estimated next period and fertile-window status
- Daily logging for period flow, mood, symptoms and notes
- Calendar with confirmed period days, estimated next-period days, fertile window and ovulation day
- Statistics for average cycle length, average period length, regularity and recent cycle history
- CSV export through Android's system file picker
- Complete deletion of cycle data and app preferences
- System, Light and Dark themes with Material You dynamic colors on Android 12+
- English and Russian localization
- About section with description, author, version, license, privacy information and GitHub link

Future dates can be viewed in the calendar but cannot be saved as actual log entries.

## 📅 How estimates work

On first use, estimates are based on the selected cycle length, the most recent period start date and an initial **5-day period length**.

After completed cycles are available, My Cycle uses up to the **6 most recent completed cycles**. Newer cycles receive greater weight when calculating average cycle and period lengths.

The estimated ovulation date is calculated as **14 days before the estimated next period**. The estimated fertile window runs from **5 days before ovulation through 1 day after it**.

These are calendar-based estimates, not medical measurements.

## 🔒 Privacy

- **100% offline for tracked data** — the app does not request the Android `INTERNET` permission
- **No account, analytics, tracking or ads**
- Cycle data is stored locally in a Room database
- App preferences are stored locally with DataStore
- Android app-data backup is disabled
- App files, databases and preferences are excluded from cloud backup and device-transfer extraction rules
- Data leaves the app only when the user explicitly creates a CSV export

The GitHub and license links in About are opened by Android in an external app such as a web browser.

Security issues should be reported according to [SECURITY.md](SECURITY.md).

## ⚕️ Important medical note

My Cycle is **not a medical device**. Period, cycle phase, fertile-window and ovulation dates are estimates. Do not use these estimates as a contraceptive method or as a substitute for professional medical advice.

## 📦 Installation

The recommended way to install the app is to download the signed APK from the latest GitHub Release:

[Download latest release](https://github.com/StanleyLl0yd/my-cycle/releases/latest)

Android 8.0 or newer is required.

## 🛠️ Build from source

Requirements:

- JDK 21
- Android SDK 36
- Gradle 8.13

The repository currently does not include `gradle-wrapper.jar`, so command-line builds require an installed Gradle 8.13.

```bash
git clone https://github.com/StanleyLl0yd/my-cycle.git
cd my-cycle
gradle assembleDebug test
gradle lint
```

To create an unsigned release APK:

```bash
gradle assembleRelease
```

Release signing is not configured in the repository. The APK attached to the official GitHub Release is signed separately.

## 🧱 Technology

| Category | Technology |
| --- | --- |
| Language | Kotlin 2.3.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | Layered MVVM |
| Dependency injection | Koin 4.1.1 |
| Async/state | Kotlin Coroutines + Flow |
| Database | Room 2.8.4 |
| Preferences | DataStore 1.1.4 |
| Build | Gradle 8.13, AGP 8.13.2, Kotlin DSL |

## ✅ Quality checks

GitHub Actions automatically checks pull requests and pushes to `main` with:

- unit tests
- Android Lint
- debug APK assembly
- release APK assembly with R8/resource shrinking

## 🌍 Languages

- English — default
- Русский

Translations follow the device language automatically.

## 🚫 Not included in 1.0.0

The current version does **not** provide:

- notifications or reminders
- data import
- account registration
- cloud synchronization or cloud storage

## 📊 Changelog

- [English changelog](CHANGELOG.md)
- [Русский changelog](CHANGELOG.ru.md)
- [GitHub Releases](https://github.com/StanleyLl0yd/my-cycle/releases)

## 🤝 Contributing

Contributions and bug reports are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

Please keep changes focused, follow Kotlin coding conventions, preserve the offline/privacy-first design, and include tests for behavior changes where practical.

## 📄 License

Licensed under the **PolyForm Noncommercial License 1.0.0**.

Noncommercial use, copying, modification and distribution are permitted under the license terms. Commercial use requires a separate agreement. See [LICENSE](LICENSE) for the authoritative text.

Copyright © 2026 Stanley Lloyd.

## 👨‍💻 Author

**Stanley Lloyd** · [@StanleyLl0yd](https://github.com/StanleyLl0yd)

---

Made with ❤️ for privacy-conscious users. If the project is useful to you, consider giving it a ⭐.
