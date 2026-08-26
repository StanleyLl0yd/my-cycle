# 🌸 My Cycle

A simple, private period tracker for Android.

![Android](https://img.shields.io/badge/Android-26%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-purple?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## ✨ Features

- **Simple logging** — Track period flow, mood, symptoms, and notes
- **Cycle estimates** — Predictions adapt to recent completed cycles
- **Calendar** — See confirmed period days and estimated fertile/ovulation windows
- **Statistics** — Average cycle length, period length, regularity, and recent history
- **CSV export** — Save your tracked data through Android's system file picker
- **Private by design** — No account, no analytics, no ads, no internet permission
- **English & Russian** — Full EN/RU interface localization

## 🔒 Privacy First

My Cycle is designed so that tracked data remains local to the app:

- ✅ No internet permission
- ✅ No user account
- ✅ No analytics or tracking SDKs
- ✅ No ads
- ✅ Android app-data backup disabled
- ✅ Local Room database and DataStore preferences
- ✅ User-controlled CSV export

## ⚕️ Important note

Cycle, fertile-window, and ovulation dates are estimates based on calendar history. My Cycle is not a medical device and should not be used as a contraceptive method or as a substitute for medical advice.

## 🛠 Tech Stack

- **Language:** Kotlin 2.3.0
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Layered MVVM with separate data/domain/UI concerns
- **Database:** Room
- **Preferences:** DataStore
- **DI:** Koin
- **Async/state:** Coroutines + Flow
- **CI:** GitHub Actions, JDK 21, Gradle 8.13

## 📦 Building

### Requirements

- Android Studio with Android SDK 36
- JDK 21
- Gradle 8.13 if building from the command line

### Steps

1. Clone the repository:

```bash
git clone https://github.com/StanleyLl0yd/my-cycle.git
cd my-cycle
```

2. Open the project in Android Studio and sync Gradle.

3. Run the `app` configuration on an Android 8.0 (API 26) or newer device/emulator.

For CI-equivalent command-line checks:

```bash
gradle build
gradle lint
```

## 🌍 Localization

Currently supported languages:

- 🇺🇸 English (default)
- 🇷🇺 Russian

Want to help translate? See [CONTRIBUTING.md](CONTRIBUTING.md).

## 📄 License

This project is licensed under the MIT License. See [LICENSE](LICENSE).

## 🤝 Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting changes.
