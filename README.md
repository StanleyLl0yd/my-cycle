# My Cycle

[English](README.md) | [Русский](README_RU.md)

A private, offline menstrual cycle tracker for Android.

**Current version:** 1.0.0  
**Android:** 8.0 (API 26) or newer  
**Application ID:** `com.silverlightning.mycycle`

## Features

### Initial setup

On first launch, the app asks for:

- the first day of the most recent period;
- the usual cycle length, from 21 to 45 days.

The initial period-length estimate is 5 days. It is later adjusted from completed cycle history.

### Today

The Today screen shows, when enough data is available:

- current cycle day;
- calculated cycle phase;
- estimated time until the next period;
- whether the estimated fertile window is active or how many days remain until it starts.

The current day can be opened directly for logging.

### Daily log

For today and past dates, the app can store:

- menstrual flow: spotting, light, medium, heavy, or none;
- mood: great, good, okay, or not great;
- symptoms;
- free-form notes.

Future dates can be viewed in the calendar but cannot be saved as actual log entries.

### Calendar

The calendar supports month-by-month navigation and shows:

- confirmed period days with flow intensity;
- estimated next-period days;
- estimated fertile window;
- estimated ovulation day.

Tapping a date opens its daily log.

### Statistics

After completed cycles are available, the Statistics screen shows:

- average cycle length;
- average period length;
- cycle regularity when at least two completed cycles are available;
- up to 12 most recent detected cycles, including the current cycle when applicable.

### Settings

Available settings and data actions:

- System, Light, and Dark themes;
- Material You dynamic colors on Android 12+;
- CSV export through the Android system file picker;
- complete deletion of cycle data and app preferences;
- About section with description, author, version, license, privacy information, and links to the license and GitHub repository.

The interface is available in English and Russian.

## How estimates work

On first use, estimates are based on the selected cycle length, the last-period start date, and an initial 5-day period length.

Once completed cycles are available, the app uses up to the 6 most recent completed cycles. Newer cycles have greater weight when calculating average cycle and period lengths.

The estimated ovulation date is calculated as 14 days before the estimated next period. The estimated fertile window runs from 5 days before ovulation through 1 day after it.

These are calendar-based estimates, not medical measurements.

## Not included in 1.0.0

The current version does **not** provide:

- notifications or reminders;
- data import;
- account registration;
- cloud synchronization or cloud storage;
- analytics or advertising.

## Privacy

Cycle data is stored locally using Room; app preferences are stored with DataStore.

The application:

- does not request the Android `INTERNET` permission;
- does not require an account;
- contains no analytics or advertising SDKs;
- disables Android app-data backup;
- excludes app files, databases, and preferences from cloud backup and device-transfer extraction rules;
- sends data outside the app only when the user explicitly creates a CSV export.

The GitHub and license links in About are opened by the system in an external app such as a web browser.

## Important medical note

My Cycle is **not a medical device**. Period, cycle phase, fertile-window, and ovulation dates are estimates. Do not use these estimates as a contraceptive method or as a substitute for professional medical advice.

## Installation

The signed APK for the current public release is available on the [GitHub Releases](https://github.com/StanleyLl0yd/my-cycle/releases) page.

## Building from source

Requirements:

- JDK 21;
- Android SDK 36;
- Gradle 8.13.

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

## Documentation

- [Changelog](CHANGELOG.md) · [Русский](CHANGELOG_RU.md)
- [Contributing](CONTRIBUTING.md) · [Русский](CONTRIBUTING_RU.md)
- [License](LICENSE) · [Справка на русском](LICENSE_RU.md)

## Author

**Stanley Lloyd**

## License

My Cycle is distributed under the **PolyForm Noncommercial License 1.0.0**. Commercial use is not granted by this license. See [LICENSE](LICENSE) for the authoritative license text.
