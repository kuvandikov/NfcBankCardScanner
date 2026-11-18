# scan-card-nfc

Android library that reads the **card number and expiry date** of contactless bank cards (EMV over NFC),
so a banking app can attach a card without manual typing. Built for Uzbek bank apps: **Humo** and **Uzcard**
are first-class, alongside Visa, Mastercard, Mir and UnionPay.

The library does not make payments. It reads only the public data that every card exposes over NFC.

> **Status:** `1.0.0-SNAPSHOT`. The EMV engine is covered by JVM unit tests. Device testing with real
> Humo/Uzcard cards is the next milestone — see the [roadmap](docs/PLAN.md).

## Usage

```kotlin
class AddCardActivity : ComponentActivity() {
    private val scanner by lazy { AndroidNfcScanner(this) }

    private fun scanCard() = lifecycleScope.launch {
        when (val result = scanner.scan()) {          // cancel the coroutine to stop reader mode
            is NfcScanResult.Success -> fillForm(result.card.pan, result.card.expiry)
            NfcScanResult.CardMovedTooFast -> toast("Hold the card still")
            NfcScanResult.CardBlocked -> toast("The card is blocked")
            NfcScanResult.NfcDisabled -> openNfcSettings()
            NfcScanResult.NfcNotSupported -> showManualEntry()
            NfcScanResult.Cancelled -> Unit
            is NfcScanResult.Error -> toast("Could not read the card")
        }
    }
}
```

With Compose: `val scanner = rememberNfcCardScanner()` from `scan-card-nfc-compose`. More in [docs/usage.md](docs/usage.md).

## Download

```kotlin
dependencies {
    implementation("io.github.kuvandikov:scan-card-nfc:<version>")
    // optional
    implementation("io.github.kuvandikov:scan-card-nfc-compose:<version>")
}
```

Upgrading from 0.1.x (Java, `CardNfcAsyncTask`)? Read the [migration guide](docs/migration.md).

## Supported cards

| Scheme | Detected by | Status |
|---|---|---|
| Humo | BIN `9860` (wins over co-badged AID) | Engine ✅ · real cards ⏳ |
| Uzcard | BIN `8600` (wins over co-badged AID) | Engine ✅ · real cards ⏳ |
| Visa | AID `A000000003…` | Engine ✅ |
| Mastercard / Maestro | AID `A000000004…` | Engine ✅ |
| Mir | AID `A000000658…` | Engine ✅ |
| UnionPay | AID `A000000333…` | Engine ✅ |
| Amex, JCB, Discover | AID / BIN | Engine ✅ |

## Modules

| Artifact | Type | Purpose |
|---|---|---|
| `scan-card-nfc-emv-core` | Kotlin/JVM | EMV engine: TLV, APDU, PDOL, PPSE → SELECT → GPO → READ RECORD. No Android dependency. |
| `scan-card-nfc` | Android library | `AndroidNfcScanner` (Reader Mode, lifecycle-aware), `AndroidCardProvider` (`IsoDep`). Depends on `emv-core` via `api`. |
| `scan-card-nfc-compose` | Android library | `rememberNfcCardScanner()`, `rememberNfcEnabledState()`. |
| `internal-testutils` | not published | `FakeCardProvider`, synthetic APDU fixtures. |
| `sample` | app | "Add card" demo screen. |

## Why no iOS?

Apple blocks payment-card AIDs (Visa, Mastercard and others) in Core NFC. The newer
`NFCPaymentTagReaderSession` (iOS 26) is limited to the EU, requires an EU account and device, and needs a
separate entitlement from Apple. For users in Uzbekistan this path is closed, so the library is Android-only.
The EMV engine lives in a pure Kotlin module so it can move to Kotlin Multiplatform if that changes.
See [ADR 0001](docs/decisions/0001-android-only.md).

## Security

- Only public EMV data is read: PAN, expiry date, sometimes the cardholder name. CVV2/CVC2 and PIN cannot be
  read over NFC.
- Nothing is logged, cached or sent: the library has no `INTERNET` permission and writes nothing to disk.
  `EmvCard.toString()` masks the PAN.
- Report vulnerabilities privately — see [SECURITY.md](SECURITY.md).

## Building

```bash
./gradlew spotlessApply      # format + license headers
./gradlew apiDump            # after a deliberate public API change
./gradlew spotlessCheck apiCheck test lint assemble
./gradlew :sample:installDebug
```

The build follows [android/nowinandroid](https://github.com/android/nowinandroid) (`build-logic`
convention plugins, version catalog, Spotless) and [google/accompanist](https://github.com/google/accompanist)
(published-library plugin, API dumps, mkdocs + Dokka site).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

```
Copyright 2026 Kuvandikov and the scan-card-nfc contributors

Licensed under the Apache License, Version 2.0
```
