# Kotlin Multiplatform NFC Card Reader — Kutubxona Arxitekturasi va Rejalashtirish Hujjati

Ushbu hujjat **Android** va **iOS** platformalari uchun mo'ljallangan, kontaktatsiz bank kartalaridan (EMV/NFC) ma'lumotlarni o'quvchi open-source **Kotlin Multiplatform (KMP)** kutubxonasini ishlab chiqish arxitekturasi va rejalashtirish bosqichlarini tasvirlaydi.

---

## 1. Loyiha Maqsadi va Strategiyasi

- **Maqsad:** KMP ekotizimida Android (`IsoDep`) va iOS (`CoreNFC`) platformalarini to'liq qo'llab-quvvatlaydigan, xavfsiz, zamonaviy va ishlatish oson bo'lgan open-source NFC Card Reader kutubxonasini yaratish.
- **Strategiya:**
  - Barcha biznes mantiq, APDU generatorlari, TLV parser va EMV standartlarini `commonMain` modulida jamlash.
  - Platformaga xos I/O operatsiyalarini abstraksiya (`expect/actual` va Clean Architecture) orqali ajratish.
  - Loyihani **Kotlin Foundation Grant** talablariga mos holda yuqori sifatli hujjatlashtirish va testlar bilan ta'minlash.

---

## 2. Kutubxona Arxitekturasi (Library Architecture)

Arxitektura **Clean Architecture** hamda **SOLID** tamoyillariga tayangan holda quyidagi uchta asosiy qatlamga bo'linadi:

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

### 2.1. Qatlamlar Tavsifi

1. **Domain Layer (`commonMain`):**
   - **`EmvCard`**: Karta raqami (PAN), amal qilish muddati, karta turi (Visa, Mastercard, Uzcard, Humo va h.k.) va karta egasining ismini saqlovchi ma'lumotlar modeli.
   - **`NfcScanResult`**: Skanerlash holatlarini (`Success`, `Error`, `CardMovedTooFast`, `CardBlocked`, `NfcNotSupported`) ifodalovchi Sealed Interface.
   - **`CardProvider`**: Platforma darajasida baytlarni uzatuvchi va qabul qiluvchi abstrakt interfeys:
     ```kotlin
     interface CardProvider {
         suspend fun transceive(command: ByteArray): ByteArray
     }
     ```

2. **Core Engine Layer (`commonMain`):**
   - **`ApduBuilder`**: `SELECT PPSE`, `SELECT AID`, `GPO`, va `READ RECORD` buyruqlarini yaratuvchi sinflar.
   - **`TlvParser`**: ASN.1/TLV (Tag-Length-Value) formatidagi baytlar oqimini parse qiluvchi unversal modul.
   - **`EmvParser`**: Muloqot zanjirini va mantiqiy ketma-ketlikni boshqaruvchi asosiy vosita.

3. **Platform Layer (`androidMain` & `iosMain`):**
   - **Android**: `IsoDep` va `NfcAdapter` yordamida `CardProvider` realizatsiyasini amalga oshirish.
   - **iOS**: `CoreNFC` va `NFCIso7816Tag` yordamida `CardProvider` realizatsiyasini amalga oshirish.

---

## 3. Loyiha Modul va Fayllar Strukturasi

```
NfcBankCardScanner/
├── build.gradle.kts
├── settings.gradle.kts
├── docs/
│   ├── ARCHITECTURE.md
│   └── USAGE.md
├── scan-card-nfc/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/io/github/kuvandikov/scancardnfc/
│       │   ├── model/
│       │   │   ├── EmvCard.kt
│       │   │   ├── EmvCardScheme.kt
│       │   │   └── NfcScanResult.kt
│       │   ├── parser/
│       │   │   ├── ApduCommand.kt
│       │   │   ├── EmvParser.kt
│       │   │   └── TlvParser.kt
│       │   ├── provider/
│       │   │   └── CardProvider.kt
│       │   └── util/
│       │       └── ByteArrayExtensions.kt
│       ├── androidMain/kotlin/io/github/kuvandikov/scancardnfc/
│       │   ├── provider/
│       │   │   └── AndroidCardProvider.kt
│       │   └── scanner/
│       │       └── AndroidNfcScanner.kt
│       └── iosMain/kotlin/io/github/kuvandikov/scancardnfc/
│           ├── provider/
│           │   └── IosCardProvider.kt
│           └── scanner/
│               └── IosNfcScanner.kt
└── app/ (Demo ilova)
    ├── androidApp/
    └── iosApp/
```

---

## 4. Rejalashtirish Bosqichlari (Roadmap)

Kutubxonani yaratish 5 ta asosiy bosqichga bo'linadi:

### Bosqich 1: Infratuzilma va KMP Muhitini Sozlash
- **Muddat:** 1-hafta
- **Vazifalar:**
  1. KMP loyihasi va Gradle kts fayllarini eng so'nggi versiyalarda sozlash (`Kotlin 2.x`, `Coroutines Multiplatform`).
  2. Maven Central / GitHub Packages nashr qilish uchun Gradle publishing skriptlarini sozlash.
  3. Strict API mode hamda litsenziyani (Apache 2.0) ko'rsatish.

### Bosqich 2: Core Business Logic (`commonMain`) Migratsiyasi va Qayta Qurish
- **Muddat:** 2-hafta
- **Vazifalar:**
  1. Java fayllaridagi bor-yo'q mantiqni to'liq idiomatik Kotlin'ga o'tkazish.
  2. Pure Kotlin TLV va APDU parserlarini yozish (hech qanday Java/Android kutubxonalarisiz).
  3. Visa, Mastercard, Humo, Uzcard, Mir karta sxemalarining AID va Regex qoidalarini shakllantirish.
  4. Unit testlarni yozish (Mock baytlar bilan APDU/TLV parserlarini test qilish).

### Bosqich 3: Android va iOS Platforma Qatlamlarini Qurish
- **Muddat:** 3-hafta
- **Vazifalar:**
  1. **Android:** `IsoDep` orqali `AndroidCardProvider` va `AndroidNfcScanner`ni yaratish (Foreground Reader Mode).
  2. **iOS:** CoreNFC (`NFCTagReaderSession`, `NFCIso7816Tag`) va C-Interop orqali `IosCardProvider`ni yaratish.
  3. Ikkala platformada ham Coroutine Flow / Suspend wrapper'larini standartlashtirish.

### Bosqich 4: Snov va Sample App (Demo) Ishlab Chiqish
- **Muddat:** 4-hafta
- **Vazifalar:**
  1. Kutubxonadan foydalanuvchi namuna **Compose Multiplatform** hamda **SwiftUI** ilovalarini yaratish.
  2. Real Humo, Uzcard, Visa va Mastercard kartalari bilan fizik qurilmalarda test o'tkazish.
  3. Xatoliklarni (kartani barvaqt olib tashlash, shifrlangan/bloklangan chip va b.) boshqarishni mukammallashtirish.

### Bosqich 5: Hujjatlashtirish va Grantga Ariza Topshirish
- **Muddat:** 5-hafta
- **Vazifalar:**
  1. `README.md`, `CONTRIBUTING.md` va API ma'lumotnomalarini tayyorlash.
  2. Loyihani GitHub'ga e'lon qilish va dastlabki versiyani (`v1.0.0`) nashr etish.
  3. **Kotlin Foundation Grants Program** sahifasida arizani shakllantirish va loyiha taklifini yuborish.

---

## 5. Xavfsizlik va Sertifikatsiya Eslatmalari

1. **Konfidentsiallik:** NFC orqali o'qiladigan ma'lumotlar faqat ochiq EMV ma'lumotlaridir (PAN va Amal qilish muddati). CVV2/CVC2 va PIN-kod xavfsizlik standartlariga muvofiq chipdan tashqariga uzatilmadi.
2. **Keshlanishning Oldini Olish:** Maxfiy ma'lumotlar xotirada uzoq ushlanmasligi va operatsiyadan so'ng darhol tozalanilishi ta'minlanadi.

---

## 6. Xulosa

Ushbu arxitektura va reja loyihani nafaqat xavfsiz va samarali qiladi, balki Kotlin Multiplatform ekotizimida yuqori talabga ega bo'lgan ochiq kodli yechimga aylantiradi hamda Kotlin Foundation Grantini qo'lga kiritish imkoniyatini maksimal darajada oshiradi.