# Security Policy

[![en](https://img.shields.io/badge/lang-en-red.svg)](SECURITY.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](SECURITY.ru.md)

## Supported versions

Security fixes are provided for the latest public release of My Cycle. Older releases are not supported once a newer public release is available.

## Scope

Security reports may cover the Android application, local data protection and import/export behavior, repository dependencies, CI/CD, signing, and official GitHub release artifacts.

My Cycle is offline-first and has no Android `INTERNET` permission. Reports about a backend, cloud account, analytics service, or remote API are therefore normally outside the current product scope unless such a component is introduced later.

## Reporting a vulnerability

Please do not disclose security or privacy vulnerabilities in a public GitHub Issue.

If GitHub shows a **Report a vulnerability** option in the Security tab, use that private reporting channel.

If private vulnerability reporting is not available, open a minimal Issue titled `Security contact request` without technical details, proof-of-concept code, exported cycle data or other sensitive information. A private contact channel can then be arranged before details are shared.

When reporting a vulnerability, please include privately:

- affected app version
- Android version and device model, when relevant
- clear reproduction steps
- expected and actual behavior
- impact assessment
- proof of concept, if needed and safe to share privately

We aim to acknowledge a complete private report within seven days when practical. Triage, remediation and disclosure timing depend on severity and reproducibility. Please allow time for investigation and a fix before public disclosure.

## Sensitive data

Never include real cycle history, personal notes, exported CSV files, private keys, API keys, access tokens or other personal data in public vulnerability reports, screenshots, logs or test cases.
