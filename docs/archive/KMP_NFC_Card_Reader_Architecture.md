# Kotlin Multiplatform NFC Card Reader — Library Architecture and Planning Document

> **Archived.** Superseded by [PLAN.md](../PLAN.md) (Android-only). Kept for the decision history in
> [ADR 0001](../decisions/0001-android-only.md).

This document describes the architecture and planning stages of an open-source **Kotlin Multiplatform (KMP)**
library for **Android** and **iOS** that reads data from contactless bank cards (EMV/NFC).

---

## 1. Project goal and strategy

- **Goal:** create a secure, modern and easy-to-use open-source NFC card reader library in the KMP ecosystem
  that fully supports Android (`IsoDep`) and iOS (`CoreNFC`).
- **Strategy:**
  - Keep all business logic, APDU generators, the TLV parser and EMV standards in the `commonMain` module.
  - Separate platform-specific I/O through abstraction (`expect/actual` and Clean Architecture).
  - Provide high-quality documentation and tests that meet the **Kotlin Foundation Grant** requirements.

---

## 2. Library architecture

The architecture follows **Clean Architecture** and **SOLID** principles and is split into three layers:

```
+-------------------------------------------------------------------+
|                        Client Application                         |
|                   (Android App / SwiftUI App)                     |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                     KMP Library Public Interface                  |
|                      (NfcCardScanner, NfcFlow)                    |
+-------------------------------------------------------------------+
                                  |
  +-------------------------------+-------------------------------+
  | (commonMain)                                                  |
  v                                                               v
+----------------------------------+    +----------------------------------+
|           Domain Layer           |    |           Core Engine            |
| - EmvCard, CardScheme, Result    |    | - ApduBuilder, TlvParser         |
| - CardProvider (Interface)       |    | - EmvParser, Track2Decoder       |
+----------------------------------+    +----------------------------------+
                  ^                                       ^
                  |                                       |
  +---------------+---------------------------------------+
  |
  +-----------------------------------+-----------------------------------+
  | (Platform Layer)                  |                                   |
  v                                   v                                   v
+-----------------------------------+   +-----------------------------------+
|           androidMain             |   |              iosMain              |
| - AndroidCardProvider (IsoDep)    |   | - IosCardProvider (NFCIso7816Tag) |
| - NfcAdapter Manager              |   | - CoreNFC Session Wrapper         |
+-----------------------------------+   +-----------------------------------+
```

### 2.1. Layers

1. **Domain layer (`commonMain`):**
   - **`EmvCard`**: data model holding the card number (PAN), expiry date, card type (Visa, Mastercard,
     Uzcard, Humo, etc.) and the cardholder name.
   - **`NfcScanResult`**: sealed interface for scan states (`Success`, `Error`, `CardMovedTooFast`,
     `NfcNotSupported`).
   - **`CardProvider`**: abstract interface that sends and receives bytes at the platform level:
     ```kotlin
     interface CardProvider {
         suspend fun transceive(command: ByteArray): ByteArray
     }
     ```

2. **Core engine layer (`commonMain`):**
   - **`ApduBuilder`**: builds the `SELECT PPSE`, `SELECT AID`, `GPO` and `READ RECORD` commands.
   - **`TlvParser`**: universal module that parses ASN.1/TLV (Tag-Length-Value) byte streams.
   - **`EmvParser`**: main component that drives the command chain and its logical order.

3. **Platform layer (`androidMain` & `iosMain`):**
   - **Android**: `CardProvider` implementation with `IsoDep` and `NfcAdapter`.
   - **iOS**: `CardProvider` implementation with `CoreNFC` and `NFCIso7816Tag`.

---

## 3. Modules and file structure

```
scan-card-nfc-kmp/
├── build.gradle.kts
├── settings.gradle.kts
├── docs/
│   ├── ARCHITECTURE.md
│   └── USAGE.md
├── library/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/io/github/kuvandikov/nfcreader/
│       │   ├── model/       EmvCard.kt, EmvCardScheme.kt, NfcScanResult.kt
│       │   ├── parser/      ApduCommand.kt, EmvParser.kt, TlvParser.kt
│       │   ├── provider/    CardProvider.kt
│       │   └── util/        ByteArrayExtensions.kt
│       ├── androidMain/kotlin/io/github/kuvandikov/nfcreader/
│       │   ├── provider/    AndroidCardProvider.kt
│       │   └── scanner/     AndroidNfcScanner.kt
│       └── iosMain/kotlin/io/github/kuvandikov/nfcreader/
│           ├── provider/    IosCardProvider.kt
│           └── scanner/     IosNfcScanner.kt
└── sample/ (demo apps)
    ├── androidApp/
    └── iosApp/
```

---

## 4. Roadmap

### Stage 1: Infrastructure and KMP setup (week 1)
1. Set up the KMP project and Gradle kts files on the latest versions (`Kotlin 2.x`,
   `Coroutines Multiplatform`).
2. Configure Gradle publishing scripts for Maven Central / GitHub Packages.
3. Enable strict API mode and declare the license (Apache 2.0).

### Stage 2: Migrate and rebuild the core business logic in `commonMain` (week 2)
1. Move all logic from the Java files to idiomatic Kotlin.
2. Write pure Kotlin TLV and APDU parsers (no Java/Android libraries).
3. Define AIDs and regex rules for the Visa, Mastercard, Humo, Uzcard and Mir schemes.
4. Write unit tests (test the APDU/TLV parsers with mock bytes).

### Stage 3: Build the Android and iOS platform layers (week 3)
1. **Android:** create `AndroidCardProvider` and `AndroidNfcScanner` over `IsoDep` (foreground Reader Mode).
2. **iOS:** create `IosCardProvider` with CoreNFC (`NFCTagReaderSession`, `NFCIso7816Tag`) and C interop.
3. Standardize Coroutine Flow / suspend wrappers on both platforms.

### Stage 4: Sample apps (week 4)
1. Build sample **Compose Multiplatform** and **SwiftUI** apps that use the library.
2. Test with real Humo, Uzcard, Visa and Mastercard cards on physical devices.
3. Polish error handling (card removed early, encrypted/blocked chip, etc.).

### Stage 5: Documentation and grant application (week 5)
1. Write `README.md`, `CONTRIBUTING.md` and the API reference.
2. Announce the project on GitHub and publish the first version (`v1.0.0`).
3. Prepare the application on the **Kotlin Foundation Grants Program** page and submit the proposal.

---

## 5. Security and certification notes

1. **Confidentiality:** data read over NFC is only public EMV data (PAN and expiry date). CVV2/CVC2 and the
   PIN are not transmitted outside the chip, per security standards.
2. **No caching:** sensitive data is not kept in memory for long and is cleared right after the operation.

---

## 6. Conclusion

This architecture and plan make the project secure and efficient, turn it into an in-demand open-source
solution in the Kotlin Multiplatform ecosystem, and maximize the chances of receiving a Kotlin Foundation
Grant.
