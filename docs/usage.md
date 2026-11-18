# Usage

## Activity / Fragment

```kotlin
class AddCardActivity : ComponentActivity() {
    private val scanner by lazy { AndroidNfcScanner(this) }

    fun onScanClicked() {
        lifecycleScope.launch { handle(scanner.scan()) }
    }
}
```

- `scan()` waits until the activity is resumed, enables Reader Mode, reads the first card presented and
  disables Reader Mode again.
- Cancel the coroutine to stop waiting. If the activity is destroyed, `scan()` returns `Cancelled`.
- Create one scanner per activity. Do not keep it in a `ViewModel` (it references the activity).

## Compose

Add only `io.github.kuvandikov:scan-card-nfc-compose:1.0.0`; it exposes `scan-card-nfc` transitively.
`rememberNfcCardScanner()` must run inside a `ComponentActivity`.

```kotlin
@Composable
fun AddCard(viewModel: AddCardViewModel = viewModel()) {
    val scanner = rememberNfcCardScanner()
    val nfcEnabled by rememberNfcEnabledState(scanner)   // refreshed on every resume
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (!nfcEnabled) {
        TextButton(onClick = { context.openNfcSettings() }) { Text("Turn on NFC") }
    }
    Button(onClick = { scope.launch { viewModel.onScanResult(scanner.scan()) } }) {
        Text("Scan card")
    }
}
```

The `sample` module contains a complete "add card" screen with a `ViewModel` and user-facing texts for every
result.

## Results

| Result | Typical message |
|---|---|
| `Success(card)` | Fill the form with `card.pan` and `card.expiry` |
| `CardMovedTooFast` | "Hold the card still until reading finishes" |
| `CardBlocked` | "The card is blocked, contact your bank" |
| `NfcDisabled` | "Turn on NFC" + `openNfcSettings()` |
| `NfcNotSupported` | Hide the NFC option, offer manual entry |
| `Cancelled` | Nothing |
| `Error(type)` | `UNSUPPORTED_TAG` → "This is not a bank card"; otherwise "Could not read the card" |

## Testing your app

Depend on the `NfcCardScanner` interface and use a fake in unit tests:

```kotlin
class FakeNfcCardScanner(var next: NfcScanResult) : NfcCardScanner {
    override val isSupported = true
    override val isEnabled = true
    override suspend fun scan() = next
}
```

## Configuration

```kotlin
AndroidNfcScanner(
    activity = this,
    config = EmvReaderConfig(
        terminalCountryCode = 860,     // ISO 3166 numeric, default Uzbekistan
        transactionCurrencyCode = 860, // ISO 4217 numeric, default UZS
        transceiveTimeoutMillis = 5_000,
    ),
)
```

## Handling card data

- Never log `card.pan`. Use `card.maskedPan` for display and logs; `toString()` already masks.
- Send the PAN to your backend over your existing secure channel and drop the reference.
- Consider `FLAG_SECURE` on screens that show card data (the sample does).
