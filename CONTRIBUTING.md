# Contributing

Issues and pull requests are welcome.

## Setup

- JDK 17+
- Android Studio (latest stable)
- An NFC phone and real cards for manual testing (`sample` app)

## Before opening a pull request

```bash
./gradlew spotlessApply                          # ktlint + license headers
./gradlew spotlessCheck apiCheck test lint
```

- **Public API.** Published modules use strict explicit API mode. If you change the public API on purpose,
  run `./gradlew apiDump` and commit the updated `api/*.api` file; reviewers look at that diff first.
- **Tests.** Every parser change needs a JVM test in `emv-core/src/test`. Build card responses with the
  `tlv()` helper and `FakeCardProvider` from `internal-testutils`.
- **No Android in `emv-core`.** It is a plain Kotlin/JVM module on purpose.

## Card data rules

**Never commit real card data.** That includes PANs, names, Track 2 data and cryptograms in tests, fixtures,
issues, logs and screenshots.

When you add a fixture from a real card log:

1. Replace the PAN with a synthetic one of the same BIN (keep the first 4–6 digits, change the rest, fix Luhn
   if needed).
2. Replace the cardholder name, Track 2 discretionary data and every cryptogram (`9F26`, `9F4B`, …).
3. Add it to `internal-testutils/.../CardFixtures.kt` with a comment describing the card profile.

## Adding a card scheme

1. Add an entry to `EmvCardScheme` (RID prefixes + BIN regex).
2. Add the full AID to `EmvCardScheme.knownAids`.
3. Cover it in `EmvCardSchemeTest` and, if possible, add a fixture flow test.

## Code style

ktlint via Spotless with the settings in `.editorconfig` (same as nowinandroid).
