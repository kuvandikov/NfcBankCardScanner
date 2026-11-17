# NFC Card Reader for Kotlin Multiplatform

Read the public data of contactless bank cards (EMV over NFC) from shared Kotlin code on **Android** and **iOS**.

```kotlin
when (val result = scanner.scan()) {
    is NfcScanResult.Success -> println(result.card)        // EmvCard(pan=4111 11•• •••• 1111, expiry=12/29, scheme=VISA, …)
    NfcScanResult.CardMovedTooFast -> showHint("Hold the card still")
    NfcScanResult.NfcDisabled -> openNfcSettings()
    else -> showError(result)
}
```

> **Status:** `0.2.0-SNAPSHOT` — Kotlin Multiplatform rewrite. The EMV engine is covered by unit tests;
> platform layers still need verification on physical devices (roadmap stage 4).

## Features

- Pure-Kotlin EMV engine in `commonMain`: BER-TLV parser, APDU builder, PDOL builder, Track 2 decoder
- `SELECT PPSE → SELECT AID → GPO → READ RECORD` flow with fallbacks (no PPSE, no AFL, `61XX`, `6CXX`)
- Schemes: Visa, Mastercard, Maestro, Mir, UnionPay, Amex, JCB, Discover, **Uzcard**, **Humo**
- Android: `IsoDep` + foreground reader mode
- iOS: CoreNFC `NFCTagReaderSession` + `NFCISO7816Tag`
- Coroutine-first API (`suspend fun scan()`), cancellable
- Strict explicit API mode, Apache 2.0

## Supported targets

| Target | Scanner | Notes |
|---|---|---|
| Android (minSdk 23) | `AndroidNfcScanner` | requires NFC hardware |
| iOS arm64 / simulator arm64 | `IosNfcScanner` | iPhone 7+; NFC does not work in the simulator |
| JVM | — | EMV engine only; bring your own `CardProvider` (e.g. PC/SC) |

## Installation

```kotlin
// build.gradle.kts of your shared module
commonMain.dependencies {
    implementation("io.github.kuvandikov:scan-card-nfc:0.2.0-SNAPSHOT")
}
```

Platform setup (manifest, entitlements, Info.plist) is described in [docs/USAGE.md](docs/USAGE.md).

## Migrating from 0.1.x

`0.2.0` is a Kotlin Multiplatform rewrite with a new API. The Maven coordinates stay the same,
but the package and every public type have changed, so existing code needs updating.

**1. Bump the dependency**

```diff
- implementation("io.github.kuvandikov:scan-card-nfc:0.1.4")
+ implementation("io.github.kuvandikov:scan-card-nfc:0.2.0")
```

minSdk drops from 26 to 23. If you use Compose Multiplatform, your app still needs minSdk 24.

**2. Update imports**

The package changes from `io.github.kuvandikov.scan_card_nfc` to `io.github.kuvandikov.scancardnfc`.

| 0.1.x | 0.2.0 |
|---|---|
| `CardNfcAsyncTask` + `CardNfcAsyncTask.Builder` | `AndroidNfcScanner(activity)` (implements `NfcCardScanner`) |
| `CardNfcUtils.enableDispatch()` / `disableDispatch()` | not needed: reader mode runs only while `scan()` is suspended |
| `onNewIntent(intent)` → build a new task | remove it; call `scanner.scan()` instead |
| `CardNfcInterface` callbacks | `NfcScanResult` returned by `scan()` |
| `task.cardNumber` | `card.pan` (`card.maskedPan` for display) |
| `task.cardExpireDate` (`String`) | `card.expiry` (`CardExpiry(month, year)`, `toString()` = `MM/YY`) |
| `task.cardType` (`String`) | `card.scheme` (`EmvCardScheme`, `.displayName` for display) |
| `EmvCard.holderFirstname` / `holderLastname` | `card.holderName` |

**3. Replace callbacks with a single `when`**

| `CardNfcInterface` callback | `NfcScanResult` |
|---|---|
| `cardIsReadyToRead()` | `Success(card)` |
| `doNotMoveCardSoFast()` | `CardMovedTooFast` |
| `unknownEmvCard()` | `Error(NO_PAYMENT_APPLICATION)` or `Error(CARD_DATA_NOT_FOUND)` |
| `cardWithLockedNfc()` | `CardBlocked` |
| `startNfcReadCard()` / `finishNfcReadCard()` | the `scan()` call starts and returns |
| — | `NfcDisabled`, `NfcNotSupported`, `Cancelled` |

Before:

```kotlin
class MainActivity : ComponentActivity(), CardNfcAsyncTask.CardNfcInterface {
    private lateinit var task: CardNfcAsyncTask

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        task = CardNfcAsyncTask.Builder(this, intent, false).build()
    }

    override fun cardIsReadyToRead() = show(task.cardNumber, task.cardExpireDate, task.cardType)
    override fun doNotMoveCardSoFast() = hint("Hold the card still")
    // ...
}
```

After:

```kotlin
class MainActivity : ComponentActivity() {
    private val scanner by lazy { AndroidNfcScanner(this) }

    fun startScan() = lifecycleScope.launch {
        when (val result = scanner.scan()) {
            is NfcScanResult.Success -> with(result.card) { show(maskedPan, expiry, scheme.displayName) }
            NfcScanResult.CardMovedTooFast -> hint("Hold the card still")
            NfcScanResult.NfcDisabled -> hint("Turn on NFC")
            else -> showError(result)
        }
    }
}
```

**Removed in 0.2.0:** transaction history (`listTransactions`), remaining PIN tries (`leftPinTry`),
ATR descriptions, and the `CARD_*` string constants. Several regional schemes from 0.1.x (CB, Dankort,
RuPay, Interac, …) now come back as `EmvCardScheme.UNKNOWN`; the PAN and expiry are still read.

## Project structure

Layout and Gradle conventions follow [JetBrains/kotlinconf-app](https://github.com/JetBrains/kotlinconf-app).

```
NfcBankCardScanner/
├── gradle/libs.versions.toml     # version catalog
├── scan-card-nfc/                # published artifact
│   └── src/
│       ├── commonMain/           # model, parser (EMV engine), provider, util
│       ├── commonTest/           # unit tests with scripted card responses
│       ├── androidMain/          # AndroidCardProvider, AndroidNfcScanner
│       ├── iosMain/              # IosCardProvider, IosNfcScanner
│       └── jvmMain/
├── app/
│   ├── shared/                   # Compose Multiplatform demo UI
│   ├── androidApp/
│   └── iosApp/                   # SwiftUI host (XcodeGen project.yml)
└── docs/
```

## Building

```bash
./gradlew :scan-card-nfc:jvmTest                      # fast EMV engine tests
./gradlew :scan-card-nfc:allTests                     # all targets (iOS tests need macOS)
./gradlew :app:androidApp:installDebug          # Android demo
cd app/iosApp && xcodegen generate && open iosApp.xcodeproj   # iOS demo
```

## Security

Only data that the card exposes without cardholder verification is read: PAN, expiry date, and sometimes
the cardholder name. CVV2/CVC2 and PIN never leave the chip, and the dynamic data in Track 2 discretionary
fields is dropped. Raw response buffers are zeroed after parsing. `EmvCard.toString()` masks the PAN.
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#security).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[Apache License 2.0](LICENSE)
