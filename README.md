# scan-card-nfc

Android library that reads the **card number and expiry date** of contactless bank cards (EMV over NFC),
so a banking app can attach a card without manual typing. Built for Uzbek bank apps: **Humo** and **Uzcard**
are first-class, alongside Visa, Mastercard, Mir and UnionPay.

The library does not make payments. It reads only the public data that every card exposes over NFC.

> **Status:** `1.0.0`. The EMV engine is covered by JVM unit tests. Device testing with real
> Humo/Uzcard cards is the next milestone — see the [roadmap](docs/PLAN.md).

## Download

Pick **one** artifact, depending on your UI toolkit:

```kotlin
dependencies {
    // Jetpack Compose app: this alone is enough.
    // It brings scan-card-nfc and scan-card-nfc-emv-core transitively.
    implementation("io.github.kuvandikov:scan-card-nfc-compose:1.0.0")

    // Views / Activity / Fragment app (no Compose):
    implementation("io.github.kuvandikov:scan-card-nfc:1.0.0")
}
```

`minSdk` 23. The library declares the `NFC` permission and an optional `android.hardware.nfc` feature in its
own manifest, so you do not need to add anything to `AndroidManifest.xml`.

### Versions

| Version | API |
|---|---|
| `1.0.0` | The Kotlin API described in this README |
| `0.1.4` | Old Java API (`CardNfcAsyncTask`), only `scan-card-nfc`, no Compose artifact. See the [migration guide](docs/migration.md). |

Between releases `VERSION_NAME` in [`gradle.properties`](gradle.properties) ends with `-SNAPSHOT`
(e.g. `1.0.1-SNAPSHOT`): that is unreleased code on `main`, never uploaded to Maven Central. To try it, run
`./gradlew publishToMavenLocal` (JDK 17 or 21; Dokka fails on JDK 25) and add `mavenLocal()` to your
repositories.

## Usage

### Jetpack Compose

```kotlin
@Composable
fun AddCardScreen(onCardRead: (EmvCard) -> Unit) {
    val scanner = rememberNfcCardScanner()              // bound to the hosting ComponentActivity
    val nfcEnabled by rememberNfcEnabledState(scanner)  // re-checked on every resume
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    Column {
        if (scanner.isSupported && !nfcEnabled) {
            TextButton(onClick = { context.openNfcSettings() }) { Text("Turn on NFC") }
        }
        Button(
            enabled = scanner.isSupported,
            onClick = {
                scope.launch {                          // leaving the screen cancels the scan
                    message = "Hold the card to the back of the phone"
                    message = when (val result = scanner.scan()) {
                        is NfcScanResult.Success -> { onCardRead(result.card); null }
                        NfcScanResult.CardMovedTooFast -> "Hold the card still"
                        NfcScanResult.CardBlocked -> "The card is blocked"
                        NfcScanResult.NfcDisabled -> "Turn on NFC"
                        NfcScanResult.NfcNotSupported -> "NFC is not available, enter the card manually"
                        NfcScanResult.Cancelled -> null
                        is NfcScanResult.Error -> "Could not read the card"
                    }
                }
            },
        ) { Text("Scan card") }
        message?.let { Text(it) }
    }
}
```

- `rememberNfcCardScanner()` must be called inside a `ComponentActivity` (e.g. via `setContent { }`).
  In `@Preview` it returns a stub that reports NFC as unsupported.
- Launch `scan()` from `rememberCoroutineScope()`, not from a `ViewModel`: the scanner holds the activity.
  Pass the result to your `ViewModel` instead — see the [sample](sample/src/main/kotlin/io/github/kuvandikov/scancardnfc/sample/AddCardScreen.kt).
- To stop scanning, cancel the `Job` returned by `scope.launch`.

### Activity / Fragment (Views)

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

More in [docs/usage.md](docs/usage.md).

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

## Releasing

For maintainers. `-SNAPSHOT` must never be released.

1. In `gradle.properties` remove `-SNAPSHOT` (e.g. `VERSION_NAME=1.0.1`) and put the same version in the
   README and `docs/usage.md`.
2. Move the `[Unreleased]` section of [CHANGELOG.md](CHANGELOG.md) under `[<version>] — <date>`.
3. Commit, then tag and push: `git tag v1.0.1 && git push origin v1.0.1`.
   [`release.yml`](.github/workflows/release.yml) checks the build, publishes all artifacts to Maven Central
   and deploys the docs.
4. Start the next cycle: `VERSION_NAME=1.0.2-SNAPSHOT` (or `1.1.0-SNAPSHOT`) and commit.

Versions follow [Semantic Versioning](https://semver.org/): patch for fixes, minor for new API, major for
breaking changes.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

```
Copyright 2026 Kuvandikov and the scan-card-nfc contributors

Licensed under the Apache License, Version 2.0
```
