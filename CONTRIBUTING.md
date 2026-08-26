# Contributing to My Cycle

[English](CONTRIBUTING.md) | [Русский](CONTRIBUTING_RU.md)

Contributions are welcome when they fit the current scope of My Cycle: a small, private, offline menstrual cycle tracker.

## Before making a change

For a bug fix, open an issue if the problem is not already documented. For a larger feature or behavior change, discuss it in an issue before implementing it.

Please keep the current product constraints in mind:

- cycle and fertility information must remain clearly presented as estimates;
- privacy and local-first storage are core requirements;
- adding accounts, cloud synchronization, analytics, advertising, or network-dependent features is outside the current 1.0.0 design and should not be introduced without an explicit project decision.

## Development requirements

- JDK 21
- Android SDK 36
- Gradle 8.13

The repository does not currently include `gradle-wrapper.jar`, so command-line checks require an installed Gradle 8.13.

Before submitting a pull request, run:

```bash
gradle assembleDebug test
gradle lint
```

If your change affects release compilation, also run:

```bash
gradle assembleRelease
```

## Pull requests

1. Fork the repository or create a working branch.
2. Keep each change focused on one problem or feature.
3. Add or update tests for cycle-detection or prediction logic when relevant.
4. Keep UI text in Android string resources rather than hardcoding it in Compose code.
5. Update documentation when behavior visible to users changes.
6. Open a pull request with a concise description of what changed and why.

## Localization

English is the default application language. Russian is maintained as an additional complete localization.

When adding or changing UI text:

- update `app/src/main/res/values/strings.xml`;
- update `app/src/main/res/values-ru/strings.xml`;
- update both `plurals.xml` files when plurals are involved;
- keep labels concise enough for small screens.

For project documentation, keep the English default file and its `_RU` companion in sync where applicable.

## Code style

- Follow Kotlin conventions.
- Prefer small, focused functions and clear names.
- Do not hardcode user-visible strings.
- Preserve the existing separation between data, domain logic, and Compose UI.
- Avoid introducing dependencies unless they provide a clear benefit to the app.

## License

My Cycle is distributed under the **PolyForm Noncommercial License 1.0.0**. Contributions must be compatible with the project license. See [LICENSE](LICENSE) for the authoritative terms.

## Author

**Stanley Lloyd**
