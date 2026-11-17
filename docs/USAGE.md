# Usage

## Shared code

Write UI and business logic against the `NfcCardScanner` interface:

```kotlin
class CardViewModel(private val scanner: NfcCardScanner) : ViewModel() {
    fun scan() = viewModelScope.launch {
        when (val result = scanner.scan()) {
            is NfcScanResult.Success -> show(result.card)
            NfcScanResult.CardMovedTooFast -> hint("Hold the card still")
            NfcScanResult.CardBlocked -> hint("This card is blocked")
            NfcScanResult.NfcDisabled -> hint("Turn on NFC")
            NfcScanResult.NfcNotSupported -> hint("NFC is not available")
            NfcScanResult.Cancelled -> Unit
            is NfcScanResult.Error -> error(result.type)
        }
    }
}
```

Cancelling the coroutine stops reader mode (Android) or closes the NFC sheet (iOS).

## Android

```kotlin
class MainActivity : ComponentActivity() {
    private val scanner by lazy { AndroidNfcScanner(this) }
    // pass `scanner` to your shared code
}
```

- The library manifest adds `android.permission.NFC` and `<uses-feature android:name="android.hardware.nfc" android:required="false"/>`.
- Reader mode only works while the activity is in the foreground — call `scan()` from a lifecycle-aware scope.

## iOS

```kotlin
// iosMain
fun MainViewController() = ComposeUIViewController { App(remember { IosNfcScanner() }) }
```

Required in the Xcode target:

1. **Capability** *Near Field Communication Tag Reading* → entitlement
   `com.apple.developer.nfc.readersession.formats = [TAG]`.
2. **Info.plist**
   - `NFCReaderUsageDescription`
   - `com.apple.developer.nfc.readersession.iso7816.select-identifiers` — must list the PPSE
     `325041592E5359532E4444463031` and every AID you want to select. iOS refuses `SELECT` for AIDs not listed here.

See `app/iosApp/iosApp/Info.plist` for a complete list.

## Custom transport

Any ISO 7816 transport can be plugged in, for example a PC/SC reader on desktop JVM:

```kotlin
val provider = CardProvider { command -> channel.transmit(CommandAPDU(command)).bytes }
val result = EmvReader(provider, EmvReaderConfig(terminalCountryCode = 860)).read()
```
