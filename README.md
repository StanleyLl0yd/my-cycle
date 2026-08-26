# My Cycle

[![Android CI](https://github.com/StanleyLl0yd/my-cycle/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/StanleyLl0yd/my-cycle/actions/workflows/ci.yml)
[![Latest release](https://img.shields.io/github/v/release/StanleyLl0yd/my-cycle)](https://github.com/StanleyLl0yd/my-cycle/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/StanleyLl0yd/my-cycle/total)](https://github.com/StanleyLl0yd/my-cycle/releases)
[![Android 8+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://github.com/StanleyLl0yd/my-cycle/releases/latest)
[![License](https://img.shields.io/badge/license-PolyForm%20Noncommercial%201.0.0-blue)](LICENSE)

[![en](https://img.shields.io/badge/lang-en-red.svg)](README.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](README.ru.md)

A simple, private period diary for Android. It keeps your real dates and gives careful calendar guesses without pretending that every body follows a 28-day clock.

[⬇️ Download the latest published APK](https://github.com/StanleyLl0yd/my-cycle/releases/latest)

Source version: **1.1.0** · Latest published APK: **1.0.0** · Min SDK: **26 (Android 8.0)** · Target SDK: **36**

## ✨ Features

- First setup asks which situation fits you now:
  - periods started less than 1 year ago
  - periods started 1–3 years ago
  - periods have been coming for more than 3 years and the gaps are usually similar
  - periods have been coming for more than 3 years but the gaps still change a lot
  - periods are changing as you get older
  - no period for 12 months or more
- The choice can be changed later in Settings without deleting old records
- Today shows how many days have passed since the last recorded period and a **range of dates** when the next one may start
- Daily records for bleeding, mood, how your body feels and free notes
- “A few spots” is kept in the diary but does **not** start a new cycle
- A short gap between real periods is kept as entered instead of being hidden by the app
- Calendar shows recorded bleeding and pale calendar guesses for possible future dates
- After enough fairly similar adult cycles, the calendar may show a broad range of days when pregnancy could be more likely
- Statistics describe your own recent pattern instead of comparing everyone with a 28-day cycle
- Simple attention notes for some unusually long gaps, longer-than-usual bleeding and new bleeding after about a year without periods
- CSV export through Android's system file picker
- Complete deletion of period records and app settings
- System, Light and Dark themes with Material You colors on Android 12+
- English and Russian language support
- About section with description, author, version, license, privacy information and GitHub link

Future dates can be viewed in the calendar but cannot be saved as real events.

## 📅 How estimates work

My Cycle treats the calendar as a **guess**, not as a measurement of the body.

On first use, the app asks for the situation that fits you, the first day of the most recent real period and the usual gap between periods. If you choose “no period for 12 months or more”, it does not ask for or create a made-up recent period date and does not guess another period.

The first guess is shown as a **date range**, not one exact day. The range is deliberately wider in the first years after periods begin, when periods have stayed very uneven for years, and when they are changing with age.

After real history is available, the app uses up to the **6 most recent finished cycles**. Newer cycles count more. If the recent gaps differ a lot, the date range becomes wider.

The app does **not** know when an egg is released. Only after at least **3 fairly similar finished adult cycles** can it show a broad calendar range when pregnancy may be more likely. It does not show this range in the first years, for long-term very uneven cycles, when periods are changing with age, or after periods have stopped for 12 months or more.

Never use the calendar colors or dates as “safe days” to avoid pregnancy.

The app also keeps a few simple attention rules:

- in the first years, a gap of about **3 months or more** is worth discussing with a doctor or another trusted health professional
- from 1 to 3 years after periods begin, gaps shorter than about **21 days** or longer than about **45 days** deserve attention if they happen
- after the first 3 years, gaps shorter than about **21 days** or longer than about **35 days** deserve attention, especially if this keeps happening
- bleeding lasting more than about **7 days in the first years**, or more than about **8 days later**, can trigger a note to seek advice
- after **12 months without a period**, any new bleeding should be discussed with a doctor, even if there is only a little blood

These notes do not tell you what the cause is and do not give a diagnosis.

## 🔒 Privacy

- **100% offline for period records** — the app does not request the Android `INTERNET` permission
- **No account, analytics, tracking or ads**
- Period records are stored only on the device in a Room database
- App settings are stored only on the device with DataStore
- Android app-data backup is disabled
- App files, databases and settings are excluded from cloud backup and device-transfer extraction rules
- Your period records leave the app only when you choose to create a CSV copy

The GitHub and license links in About are opened by Android in an external app such as a web browser.

Security issues should be reported according to [SECURITY.md](SECURITY.md).

## ⚕️ Important medical note

My Cycle is **a diary, not a doctor or a medical test**. It cannot know why periods are early, late, heavy, light or missing. It cannot confirm whether an egg was released, whether pregnancy is possible on a particular day, or whether a change is harmless.

Uneven periods are often common in the first years after periods begin and again when periods are getting closer to stopping for good. But that does not mean every unusual change should be ignored. The simple attention limits above are based on widely used guidance from organizations including [ACOG](https://www.acog.org/clinical/clinical-guidance/committee-opinion/articles/2015/12/menstruation-in-girls-and-adolescents-using-the-menstrual-cycle-as-a-vital-sign) and the [2023 International PCOS Guideline](https://www.asrm.org/practice-guidance/practice-committee-documents/recommendations-from-the-2023-international-evidence-based-guideline-for-the-assessment-and-management-of-polycystic-ovary-syndrome/).

The broad possible egg-release range used only for fairly steady adult histories allows for the fact that the time between egg release and the next period is not always exactly 14 days; [ASRM guidance](https://www.asrm.org/practice-guidance/practice-committee-documents/diagnosis-and-treatment-of-luteal-phase-deciency-a-committee-opinion-2021/) describes a normal range that can vary by several days.

If bleeding is very heavy, you feel faint or very weak, pain is severe, you may be pregnant, or something simply feels wrong, do not wait for the app — seek medical help.

## 📦 Installation

The latest published signed APK is currently **1.0.0** and is available from GitHub Releases:

[Download latest release](https://github.com/StanleyLl0yd/my-cycle/releases/latest)

The source code in `main` is being updated to **1.1.0**. Until a signed 1.1.0 release is published, the APK above does not contain the new 1.1.0 cycle rules described in this README.

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

Release signing is not configured in the repository. APKs attached to official GitHub Releases are signed separately.

## 🧱 Technology

| Category | Technology |
| --- | --- |
| Language | Kotlin 2.3.0 |
| UI | Jetpack Compose + Material 3 |
| App structure | Layered MVVM |
| Dependency injection | Koin 4.1.1 |
| Background/state work | Kotlin Coroutines + Flow |
| Local database | Room 2.8.4 |
| Local settings | DataStore 1.1.4 |
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

The app follows the device language automatically.

## 🚫 Not included in 1.1.0

The current source version does **not** provide:

- notifications or reminders
- data import
- account registration
- cloud synchronization or cloud storage
- diagnosis or medical treatment advice
- a real test for egg release or pregnancy

## 📊 Changelog

- [English changelog](CHANGELOG.md)
- [Русский changelog](CHANGELOG.ru.md)
- [GitHub Releases](https://github.com/StanleyLl0yd/my-cycle/releases)

## 🤝 Contributing

Contributions and bug reports are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

Please keep changes focused, preserve the offline/privacy-first design, keep user-facing health explanations in simple everyday language, and include tests for behavior changes where practical.

## 📄 License

Licensed under the **PolyForm Noncommercial License 1.0.0**.

Noncommercial use, copying, modification and distribution are permitted under the license terms. Commercial use requires a separate agreement. See [LICENSE](LICENSE) for the authoritative text.

Copyright © 2026 Stanley Lloyd.

## 👨‍💻 Author

**Stanley Lloyd** · [@StanleyLl0yd](https://github.com/StanleyLl0yd)

---

Made with ❤️ for privacy-conscious users. If the project is useful to you, consider giving it a ⭐.
