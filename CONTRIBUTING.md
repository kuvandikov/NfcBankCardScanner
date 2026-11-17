# Contributing

Thanks for helping! Issues and pull requests are welcome.

## Development setup

- JDK 21
- Android Studio (latest) with the Kotlin Multiplatform plugin
- macOS + Xcode for iOS targets, and [XcodeGen](https://github.com/yonaskolb/XcodeGen) for the sample app

## Before opening a PR

```bash
./gradlew :scan-card-nfc:jvmTest :scan-card-nfc:assemble
```

- Public API changes must keep explicit visibility and return types (strict API mode is on).
- Add a `commonTest` case for every parser change. Use `FakeCardProvider` and the `tlv()` helper.
- **Never commit real card data.** Test PANs must be synthetic (e.g. `4111111111111111`) or fully masked.
  When you capture APDU logs from a real card, replace PAN, name, Track 2 and cryptograms before sharing.

## Adding a card scheme

1. Add an entry to `EmvCardScheme` with RID prefixes and a BIN regex.
2. Add its full AID to `EmvCardScheme.knownAids` and to the iOS `select-identifiers` in the sample `Info.plist`.
3. Cover it in `EmvCardSchemeTest`.

## Code style

Kotlin official code style (`kotlin.code.style=official`), settings in `.editorconfig`.
