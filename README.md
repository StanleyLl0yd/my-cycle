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

Source version: **1.1.1** · Latest published APK: **1.1.0** · Min SDK: **26 (Android 8.0)** · Target SDK: **36**

## ✨ Features

- First setup asks which situation fits you now, and you must choose one before continuing:
  - periods started less than 1 year ago
  - periods started 1–3 years ago
  - periods have been coming for more than 3 years and the gaps are usually similar
  - periods have been coming for more than 3 years but the gaps still change a lot
  - periods are changing as you get older
  - periods stopped for a full year as part of getting older, or a doctor has already explained that they stopped for good
- The choice can be changed later in Settings without deleting old records
- Existing 1.0.0/1.1.0 records stay in place. Old “A few spots” entries are treated as spots, not as a real period
- Today shows how many days have passed since the last recorded period and a **range of dates** when the next one may start
- Daily records for bleeding, mood, how your body feels and free notes
- “A few spots” is kept in the diary but does **not** start a new cycle
- If you explicitly record “No blood” or “A few spots” between two bleeding days, the app does not join those days into one period. A completely unrecorded nearby day can still be treated as a missed diary entry
- The one period-start date entered during first setup is not treated as proof that the bleeding lasted one day. The bleeding length stays unknown until enough real days are recorded
- Calendar shows recorded bleeding and pale calendar guesses for possible future dates
- After enough fairly similar adult cycles, the calendar may show a broad range of days when pregnancy could be more likely
- Statistics use recent history: up to 6 finished cycles for summary numbers and up to 12 cycles for the history list
- Simple attention notes for some unusually long gaps, longer-than-usual bleeding and new bleeding after about a year without periods
- Dark mode, better contrast and spoken calendar descriptions for TalkBack
- CSV export through Android's system file picker
- Complete deletion of period records and app settings
- System, Light and Dark themes with Material You colors on Android 12+
- English and Russian language support
- About section with description, author, version, license, privacy information and GitHub link

Future dates can be viewed in the calendar but cannot be saved as real events.

## 📅 How estimates work

My Cycle treats the calendar as a **guess**, not as a measurement of the body.

On first use, the app asks for the situation that fits you, the first day of the most recent real period and the usual gap between periods. The first question must be answered before setup continues. The option about periods stopping for a full year is meant for age-related changes or a situation already explained by a doctor; a year without periods for another unknown reason should be discussed with a doctor.

When an existing installation is updated, the previous diary stays untouched. If the app does not yet know which situation fits, it uses a deliberately wide range and does not estimate egg-release or pregnancy-likelihood days until you choose the closest option in Settings.

The first guess is shown as a **date range**, not one exact day. The range is deliberately wider in the first years after periods begin, when periods have stayed very uneven for years, when they are changing with age, and when recent gaps are fairly steady but their usual length is outside the common range used by the app.

After real history is available, the app uses up to the **6 most recent finished cycles**. Newer cycles count more. If the recent gaps differ a lot, the date range becomes wider.

The app does **not** know when an egg is released. Only after at least **3 fairly similar finished adult cycles** can it show a broad calendar range when pregnancy may be more likely. It does not show this range in the first years, for long-term very uneven cycles, when periods are changing with age, after age-related periods have stopped for a full year, while the situation choice is unset, or when a fairly steady adult cycle is outside the common range used by the app.

The pregnancy-likelihood range starts five days before the broad possible egg-release range and ends with that possible egg-release range. It is still only a calendar guess.

Never use the calendar colors or dates as “safe days” to avoid pregnancy.

The app also keeps a few simple attention rules:

- in the first years, a current or most recent gap of about **3 months or more** is worth discussing with a doctor or another trusted health professional
- later, an unexplained gap of about **3 months or more** is also worth discussing with a doctor if pregnancy is not expected and you do not know why the periods stopped
- from 1 to 3 years after periods begin, gaps shorter than about **21 days** or longer than about **45 days** deserve attention
- after the first 3 years, when gaps are normally similar, gaps shorter than about **21 days** or longer than about **35 days** deserve attention, especially if this keeps happening
- bleeding lasting more than about **7 days in the first years**, or more than about **8 days later**, can trigger a note to seek advice
- after a full year without a period as part of age-related changes, new bleeding should be discussed with a doctor, even if there is only a little blood

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

The latest published signed APK is **1.1.0** and is available from GitHub Releases. The source code in this branch is already **1.1.1**; the published APK line will be updated only after the signed 1.1.1 release exists.

[Download latest release](https://github.com/StanleyLl0yd/my-cycle/releases/latest)

SHA-256 (`My-Cycle-v1.1.0.apk`): `a8991c51f08421c31e0a53a1a2cb1243239e3dd044177ecfa8137e903c461f69`

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

## 🚫 Not included in 1.1.1

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
