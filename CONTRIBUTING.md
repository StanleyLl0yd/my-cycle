# Contributing to My Cycle

Thank you for your interest in contributing to My Cycle! 💜

## License

My Cycle is distributed under the **PolyForm Noncommercial License 1.0.0**. Contributions to this repository are expected to be compatible with that license. See [LICENSE](LICENSE) for the complete terms.

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/StanleyLl0yd/my-cycle/issues)
2. If not, create a new issue with:
   - Clear description of the bug
   - Steps to reproduce
   - Expected vs actual behavior
   - Device info (Android version, device model)
   - Screenshots if applicable

### Suggesting Features

1. Open an issue with the `enhancement` label
2. Describe the feature and why it would be useful
3. Keep in mind the core principles: simplicity and privacy

### Code Contributions

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes following the code style
4. Write or update tests where applicable
5. Commit with a clear message
6. Push and create a Pull Request

### Adding Translations

1. Create a new folder in `app/src/main/res/` named `values-XX` (where XX is the language code)
2. Copy `values/strings.xml` and `values/plurals.xml` to the new folder
3. Translate all strings
4. Pay special attention to plurals
5. Test the app with the new language

#### Translation Guidelines

- Keep translations concise — UI space is limited
- Maintain the same warm, clear tone
- Don't translate brand names
- Test all screens to ensure text fits properly

## Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Keep functions small and focused
- Add KDoc comments for public APIs where useful
- No hardcoded UI strings — use string resources

## Commit Messages

Use clear, concise imperative messages, for example:

- `Add cycle statistics`
- `Fix period prediction fallback`
- `Update Russian translations`
- `Refactor settings state`
- `Add prediction tests`

## Questions?

Feel free to open an issue with the `question` label.

Thank you for helping make My Cycle better! 🌸
