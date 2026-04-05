# NFC Bank Card Scanner Library

<img src="https://img.shields.io/badge/Platform-Android-brightgreen" alt="Platform"> <img src="https://img.shields.io/badge/API-21%2B-blue" alt="API"> [![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

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
    implementation 'io.github.kuvandikov:scan-card-nfc:0.1.4'
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
