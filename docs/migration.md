# Migration from 0.1.x (Java) to 1.0 (Kotlin)

Version 1.0 is a rewrite in Kotlin. The Maven coordinates stay the same
(`io.github.kuvandikov:scan-card-nfc`), but the API is new.

!!! warning
    The 0.1.x names below follow the `CardNfcAsyncTask` / `CardNfcInterface` API. Check them against the
    exact 0.1.x version your app uses before migrating.

## Summary

- `AsyncTask` + callbacks → one `suspend fun scan(): NfcScanResult`.
- Foreground dispatch (`onNewIntent`, intent filters, tech list XML) → Reader Mode, managed by the library.
- Root package `io.github.kuvandikov.scancardnfc`.
- `minSdk` 23.

## Step by step

1. **Update the dependency** to `1.0.0`.
2. **Remove the NFC intent setup** from your app: the `TECH_DISCOVERED` intent filter, the `nfc_tech_filter.xml`
   resource, `enableForegroundDispatch` / `disableForegroundDispatch` calls and the NFC handling in `onNewIntent`.
   The library declares the `NFC` permission itself.
3. **Replace the task with the scanner**:

    ```kotlin
    // 0.1.x
    cardNfcAsyncTask = CardNfcAsyncTask.Builder(this, intent, intentFromCreate).build()

    // 1.0
    private val scanner by lazy { AndroidNfcScanner(this) }
    lifecycleScope.launch { handle(scanner.scan()) }
    ```

4. **Map the callbacks to results**:

    | 0.1.x callback | 1.0 result |
    |---|---|
    | `startNfcReadCard()` | — (show progress before calling `scan()`) |
    | `cardIsReadyToRead()` + `getCardNumber()` / `getCardExpireDate()` | `Success(card)` → `card.pan`, `card.expiry` |
    | `doNotMoveCardSoFast()` | `CardMovedTooFast` |
    | `cardWithLockedNfc()` | `CardBlocked` |
    | `unknownEmvCard()` | `Error(NO_PAYMENT_APPLICATION)` / `Error(UNSUPPORTED_TAG)` |
    | `finishNfcReadCard()` | `scan()` returned |
    | `getCardType()` (string) | `card.scheme` (`EmvCardScheme` enum) |

5. **Expiry format**: `card.expiry` is a `CardExpiry(month, year)`; `toString()` gives `MM/YY`.
6. **Check NFC state** with `scanner.isSupported` / `scanner.isEnabled` and open settings with
   `context.openNfcSettings()`.
