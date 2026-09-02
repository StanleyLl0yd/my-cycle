# Contributing to My Cycle

[![en](https://img.shields.io/badge/lang-en-red.svg)](CONTRIBUTING.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](CONTRIBUTING.ru.md)

Contributions and bug reports are welcome. Please keep changes focused and preserve the project's privacy-first, offline design.

## 🐛 Reporting bugs

Before opening a new issue, check whether the problem has already been reported.

Include:

- a clear description of the problem
- steps to reproduce it
- expected and actual behavior
- Android version and device model when relevant
- screenshots when they help explain the issue

Do not include personal cycle data, private notes, exported CSV files or other sensitive information in public reports.

## 💡 Feature requests

Open an issue and explain:

- what problem the feature solves
- how it should behave
- whether it affects privacy, storage, permissions or network access

Features that introduce accounts, cloud synchronization, analytics, advertising or background network access require especially careful privacy review.

## 🧑‍💻 Code contributions

1. Fork the repository.
2. Create a focused branch.
3. Make the smallest practical change.
4. Add or update tests when behavior changes.
5. Run the project checks.
6. Open a Pull Request with a concise explanation of the change.

## ✅ Project checks

Requirements:

- JDK 21
- Android SDK 37

Use the verified Gradle Wrapper included in the repository:

```bash
./gradlew --dependency-verification=strict assembleDebug assembleRelease test
./gradlew --dependency-verification=strict lint
./gradlew --dependency-verification=strict detekt
python3 scripts/check-source-comments.py
```

Every Pull Request and push to `main` is also checked by Qodana, CodeQL, Semgrep and Gitleaks. Dependabot monitors Gradle, GitHub Actions and the pinned Python security tool.

## 🧭 Code style

- Follow Kotlin coding conventions.
- Keep UI strings in Android string resources.
- Keep domain rules out of Composables when practical.
- Prefer small, focused functions and classes.
- Preserve lifecycle-aware Flow collection and existing state-management patterns.
- Keep source comments to the minimum: only genuinely necessary comments, in English only.
- Do not add network permissions, analytics or advertising dependencies without an explicit project decision.

## 🌍 Translations

English is the default application language. Russian is maintained alongside it.

When changing user-facing text:

- update both English and Russian resources
- keep wording concise enough for mobile UI
- preserve placeholders and plurals
- verify that the meaning is equivalent in both languages

## 📄 License

My Cycle is distributed under the **PolyForm Noncommercial License 1.0.0**. Contributions to this repository are expected to be compatible with that license.

See [LICENSE](LICENSE) for the authoritative terms.
