# NFC Bank Card Scanner Library

<img src="https://img.shields.io/badge/Platform-Android-brightgreen" alt="Platform"> <img src="https://img.shields.io/badge/API-21%2B-blue" alt="API"> <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">

An Android library for reading bank card information via NFC technology.

## ✨ Features
- ✅ Read bank card data via NFC
- ✅ Retrieve card number (PAN)
- ✅ Get expiration date
- ✅ Access cardholder name (if available)
- ✅ Simple integration
- ✅ Supports major card networks (Visa, Mastercard, Humo, Uzcard, etc.)

## 📦 Installation

Add this dependency to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'io.github.kuvandikov:scan-card-nfc:0.1.1'
}
```

## 🚀 Usage
Basic Setup
1. Add NFC permissions to your AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="true" />
```
2. Add NFC intent filter:
```xml
<intent-filter>
    <action android:name="android.nfc.action.TECH_DISCOVERED" />
</intent-filter>
<meta-data
    android:name="android.nfc.action.TECH_DISCOVERED"
    android:resource="@xml/nfc_tech_filter" />
```
Implementation Example

```kotlin
class MainActivity : AppCompatActivity(), NfcCardReader.CardListener {

    private lateinit var nfcCardReader: NfcCardReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcCardReader = NfcCardReader(this)
    }

    override fun onResume() {
        super.onResume()
        nfcCardReader.enableReaderMode(this)
    }

    override fun onPause() {
        super.onPause()
        nfcCardReader.disableReaderMode()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcCardReader.readCard(intent)
    }

    override fun onCardRead(card: BankCard) {
        runOnUiThread {
            cardNumberTextView.text = card.cardNumber
            expiryDateTextView.text = "${card.expiryMonth}/${card.expiryYear}"
            cardholderNameTextView.text = card.cardholderName ?: "Unknown"
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }
    }
}
```

## 💳 BankCard Model
The library returns a BankCard object with these properties:
```kotlin
data class BankCard(
    val cardNumber: String,       // Primary Account Number (PAN)
    val expiryMonth: Int,         // Month (1-12)
    val expiryYear: Int,          // Year (last 2 digits)
    val cardholderName: String?,  // Cardholder name (nullable)
    val cardType: String?         // Card network (Visa, Mastercard, etc.)
)
```
## ⚠️ Important Notes
Tested with Android API 21+
Requires NFC-enabled device
Works with contactless payment cards
Some cards may not provide all information
Read-only functionality (no writing support)

## 🤝 Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
