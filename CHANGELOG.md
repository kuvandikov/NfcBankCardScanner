# Changelog

All notable changes are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0] — 2026-11-19

Rewrite in Kotlin. See the [migration guide](docs/migration.md).

### Added
- `scan-card-nfc-emv-core`: pure Kotlin EMV engine (TLV parser, APDU builder, PDOL builder, Track 2 decoder,
  PPSE → SELECT AID → GPO → READ RECORD with fallbacks for missing PPSE / AFL and `61XX` / `6CXX`).
- Humo and Uzcard detection by BIN, including cards co-badged with Visa / Mastercard.
- `AndroidNfcScanner`: coroutine API over Reader Mode, lifecycle-aware.
- `NfcScanResult.CardBlocked` for blocked applications (`6A81` / `6283`).
- `scan-card-nfc-compose`: `rememberNfcCardScanner()`, `rememberNfcEnabledState()`.
- `openNfcSettings()` helper.

### Changed
- `minSdk` 23.

### Removed
- `CardNfcAsyncTask` and the Java API.
