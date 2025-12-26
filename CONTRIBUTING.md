# Contributing to My Cycle

Thank you for your interest in contributing to My Cycle! 💜

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/yourusername/mycycle/issues)
2. If not, create a new issue with:
   - Clear description of the bug
   - Steps to reproduce
   - Expected vs actual behavior
   - Device info (Android version, device model)
   - Screenshots if applicable

### Suggesting Features

1. Open an issue with the `enhancement` label
2. Describe the feature and why it would be useful
3. Keep in mind our core principles: simplicity and privacy

### Code Contributions

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes following our code style
4. Write/update tests if applicable
5. Commit with clear messages: `git commit -m "Add: feature description"`
6. Push and create a Pull Request

### Adding Translations

1. Create a new folder in `app/src/main/res/` named `values-XX` (where XX is the language code)
2. Copy `values/strings.xml` and `values/plurals.xml` to the new folder
3. Translate all strings
4. Pay special attention to plurals (different languages have different plural rules)
5. Test the app with the new language

#### Translation Guidelines

- Keep translations concise - UI space is limited
- Maintain the same tone: warm, friendly, supportive
- Don't translate brand names
- Test all screens to ensure text fits properly

## Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Keep functions small and focused
- Add KDoc comments for public APIs
- No hardcoded strings - use string resources

## Commit Messages

Format: `Type: Description`

Types:
- `Add:` New feature
- `Fix:` Bug fix
- `Update:` Changes to existing feature
- `Refactor:` Code restructuring
- `Docs:` Documentation changes
- `Style:` Formatting, no code change
- `Test:` Adding or updating tests

## Questions?

Feel free to open an issue with the `question` label.

Thank you for helping make My Cycle better! 🌸
