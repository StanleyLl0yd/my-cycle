# Concept: Modern Android Cycle Tracker (RU/EN ready)

## Product goals
- Private, accurate menstrual-cycle tracking with respectful onboarding and transparent consent.
- Offline-first with optional encrypted sync; explicit opt-in for any analytics or backups.
- Designed for accessibility, adaptive layouts, and Material 3 aesthetics.

## Core feature set
- **Cycle logging:** start/end of menstruation, flow intensity/color, symptoms, mood, temperature, weight, sleep, medications, custom tags.
- **Forecasts:** fertile window, ovulation estimate, next period; show confidence ranges and explain variability.
- **Reminders:** cycle events, ovulation, medication, hydration, sleep; Android 13+ notification permission flow with clear rationale.
- **Calendars & insights:** monthly/weekly Compose calendars with phase markers, filters by symptom; charts for cycle length, variability, correlations.
- **Education:** contextual tips and a small, locally bundled library; links to vetted resources.
- **Privacy & safety:** on-device by default, biometric/app lock, quick data wipe, encrypted exports.

## Architecture & tech stack (aligned with current Google guidance)
- **UI:** Jetpack Compose + Material 3 (Compose BOM), Navigation Compose, SplashScreen API.
- **State:** Unidirectional data flow (ViewModel + immutable state + intents) with `StateFlow`/`MutableStateFlow`.
- **DI:** Hilt preferred; convention plugins for module setup.
- **Data:** Repository pattern; Room (with `@TypeConverter` for enums/lists), DataStore Proto for preferences, sealed errors.
- **Background:** WorkManager for reminders/sync; avoid foreground services unless justified.
- **Networking:** Retrofit + OkHttp + Moshi or Kotlinx Serialization; TLS 1.2+, optional certificate pinning when sync is enabled.
- **Performance:** Baseline Profiles, App Startup optimization, StrictMode + LeakCanary in debug, R8/ProGuard.
- **Testing:** Unit (Turbine for Flow), instrumented (Room migrations, WorkManager, navigation), Compose UI tests, Macrobenchmark.
- **Modularity:** `app` host + feature modules (`calendar`, `log`, `insights`, `reminders`, `onboarding`, `settings`) and core layers (`designsystem`, `ui`, `model`, `data`, `database`, `datastore`, `analytics`, `common`, `testing`).

## Key flows
- **Onboarding:** goals, cycle length setup, privacy choices, permissions (notifications/alarms) with educational copy.
- **Home dashboard:** “today” card, forecast card, quick log chips, shortcuts to calendar and reminders.
- **Calendar:** adaptive layout (NavigationRail on tablets), day details sheet, symptom filters, badges for fertile window/ovulation.
- **Add/edit entry:** structured form with validation, inline tips, autosave draft via `rememberSaveable` + ViewModel state.
- **Insights:** charts with trend explanations, optional on-device ML hints; exportable as encrypted file on demand.
- **Settings:** theme (dynamic color, light/dark/high-contrast), privacy, backups, analytics consent, notification channels, language selector.

## Privacy, security, and compliance
- Minimal permissions; `POST_NOTIFICATIONS` requested with clear rationale, `SCHEDULE_EXACT_ALARM` only when justified.
- Encrypted local storage (EncryptedFile/SQLCipher-safe Room), DataStore with master key; biometric/app lock using `BiometricPrompt`.
- On-device computations; no sharing of health data without explicit, revocable consent. Visible “Delete all data” action.
- Include Privacy Policy, data retention notes, Play Integrity for distribution; adhere to Play health data policies.

## Localization and internationalization (RU/EN initial scope)
- **Resources:** all user-facing text in `res/values/strings.xml` with `%1$s`-style placeholders; `res/values-ru/strings.xml` for Russian. Keep string keys semantically meaningful.
- **Plurals and genders:** use `plurals` resources for counts; avoid gendered language or provide alternatives per locale. Prefer neutral phrasing in English; in Russian, supply context-aware strings to avoid ambiguity.
- **Date/time/units:** rely on `java.time` with locale-aware formatting; support metric/imperial toggles (°C/°F, kg/lb). Use localized week start (from `Locale` or user setting).
- **Typography and layout:** ensure layouts are flexible for longer Russian strings; use `Modifier.semantics` and `contentDescription` for a11y. Test RTL mirroring even if locales are LTR-only initially.
- **Dynamic language switch:** expose in-app language selector backed by DataStore; update `Locale` at runtime via `LocaleConfig`/`AppCompatDelegate.setApplicationLocales` (API 33+ first-party approach, AppCompat for lower).
- **Testing:** add screenshot/UI tests per locale; pseudo-locale testing (`en-XA`, `ar-XB`) in CI to catch truncation/RTL issues. Validate date/number formatting in unit tests.
- **Content strategy:** avoid idioms; provide glossary for symptoms to maintain consistency across translations. Separate educational content to allow future localization or server-driven updates.
- **Notifications:** channel names/descriptions localized; ensure concise text that fits notification limits in both languages.
- **Accessibility:** review TalkBack output for each locale; ensure phonetic readability of medical terms.

## Analytics and consent
- Analytics off by default; explicit opt-in with clear scopes. Respect system “disable personalized ads.”
- Store consent status in DataStore; gate any logging behind checks. Provide an easy opt-out and data deletion flow.

## Release and distribution
- **SDK targets:** minSdk 26+, targetSdk latest stable; App Bundle with Play App Signing.
- **Continuous delivery:** CI builds `:app:bundleRelease`, runs lint + tests; use Gradle cache and version catalogs. Signed builds with Play Integrity API checks.
- **Quality gates:** detekt/ktlint/Android Lint baselines kept fresh; Baseline Profiles generated via Macrobenchmark.

## Next steps
- Define detailed data schemas and Room migrations with tests.
- Draft design system tokens (colors, typography, shapes) with Dynamic Color support and high-contrast variant.
- Prototype Compose screens (calendar, add entry, dashboard) and validate copy in RU/EN.
- Add localization playbook to onboarding and notification copy; run pseudo-locale audit.
