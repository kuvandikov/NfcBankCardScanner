# NFC Bank Karta O'qish — EMV APDU Ketma-ketligi

Bu hujjat smartfon NFC orqali kontaktsiz bank kartasini o'qish jarayonining
to'liq texnik ketma-ketligini, real APDU buyruq/javoblari bilan tasvirlaydi.

---

## 0-bosqich: Fizik va transport tayyorgarlik

- Telefon 13.56 MHz chastotada elektromagnit maydon chiqaradi (polling).
- Karta shu maydonga kirganda antenna orqali energiya oladi, chip ishga tushadi.
- ISO 14443-3/4 darajasida "tanishuv" (ATS — Answer To Select) sodir bo'ladi.
- Bu qatlamni operatsion tizim o'zi bajaradi; dastur `IsoDep` (Android) yoki
  `NFCISO7816Tag` (iOS) orqali tayyor kanalni oladi.

---

## 1-bosqich: SELECT PPSE

Terminal kontaktsiz to'lov muhitini tanlaydi.

**Yuborilgan (Command APDU):**
```
00 A4 04 00 0E 32 50 41 59 2E 53 59 53 2E 44 44 46 30 31 00
```

| Bayt | Ma'nosi |
|---|---|
| `00` | CLA |
| `A4` | INS — SELECT |
| `04` | P1 — nom bo'yicha tanlash |
| `00` | P2 |
| `0E` | Lc — data uzunligi (14 bayt) |
| `32 50 41 59 2E 53 59 53 2E 44 44 46 30 31` | `"2PAY.SYS.DDF01"` (ASCII) |
| `00` | Le |

**Javob (Response APDU) — misol:**
```
6F 23 84 0E 32 50 41 59 2E 53 59 53 2E 44 44 46 30 31
A5 11 BF0C 0E 61 0C 4F 07 A0000000041010 50 04 4D617374 9000
```

- `9000` — muvaffaqiyatli natija kodi.
- `4F 07 A0000000041010` — kartadagi **AID** (bu misolda Mastercard).

---

## 2-bosqich: SELECT AID

Topilgan AID orqali aniq to'lov ilovasi faollashtiriladi.

```
00 A4 04 00 07 A0 00 00 00 04 10 10 00
```

Javobda ko'pincha **PDOL** (Processing Data Object List) qaytadi — kartaning
qo'shimcha ma'lumot (masalan summa, valyuta) so'rovi.

---

## 3-bosqich: GET PROCESSING OPTIONS (GPO)

Terminal PDOL'da so'ralgan qiymatlarni (skanerlash uchun odatda nol qiymatlar)
to'ldirib yuboradi:

```
80 A8 00 00 <Lc> 83 <uzunlik> <PDOL qiymatlari> 00
```

**Javobda AFL (Application File Locator) keladi**, masalan:

```
94 0C 08 01 01 00 10 01 01 01 18 01 02 00
```

AFL 4 baytli bloklarga bo'linadi:

| Bayt | Ma'nosi |
|---|---|
| 1-bayt | SFI (Short File Identifier) — `>> 3` qilib olinadi |
| 2-bayt | boshlanish yozuv raqami |
| 3-bayt | tugash yozuv raqami |
| 4-bayt | offline autentifikatsiya kerakligi bayrog'i |

> **Eslatma:** GPO javobi ikki formatda kelishi mumkin:
> - `80` (Format 1) — AIP + AFL bitta uzluksiz blokda
> - `77` (Format 2) — TLV ichida alohida teglar
>
> Kod ikkalasini ham qo'llab-quvvatlashi kerak.

---

## 4-bosqich: READ RECORD

Har bir AFL blokiga mos har bir yozuv o'qiladi:

```
00 B2 <yozuv_raqami> <SFI<<3 | 4> 00
```

Masalan `00 B2 01 0C 00` — SFI=1 dagi 1-yozuvni o'qish.

**Javob — misol:**
```
70 81 9F
  57 13 4111111111111111D25121010000000000000F   ← Track2 (Tag 57)
  5F20 1A 4A4F484E2F444F450A2020202020...         ← Ism (Tag 5F20)
  9F1F 18 ...                                      ← Track1 discretionary
  90 00
```

---

## 5-bosqich: TLV dekodlash (Tag-Length-Value)

`57 13 4111111111111111D25121010000000000000F`:

| Qism | Ma'nosi |
|---|---|
| `57` | Tag — Track2 Equivalent Data |
| `13` | Length — 19 bayt (hex) |
| `4111111111111111` | PAN (karta raqami) |
| `D` | Ajratuvchi belgi |
| `2512` | Muddat — `YYMM` (2025-yil, 12-oy) |
| qolgani | Servis kodi + discretionary data |

**Muhim:** ma'lumot **BCD (Binary Coded Decimal)** formatida kodlangan —
har bir yarim bayt (nibble) bitta o'nlik raqamga mos keladi. Parsing kodi
doim "2 hex belgi = 1 raqam" qoidasiga amal qiladi.

---

## 6-bosqich: Ism va qo'shimcha maydonlar

`5F20` tegi mavjud bo'lsa, matn `"/"` bo'yicha bo'linadi:
```
"DOE/JOHN" → Familiya: DOE, Ism: JOHN
```

---

## Amaliyotda uchraydigan muammolar

| Holat | Sabab | Yechim |
|---|---|---|
| `SW = 6C XX` | Le noto'g'ri, kartaga XX bayt kerak | Xuddi shu buyruqni to'g'ri `Le` bilan qayta yuborish |
| PSE ishlamaydi | Ba'zi kartalarda faqat PPSE bor | PPSE (`2PAY...`) va PSE (`1PAY...`) ikkalasini sinash |
| Kernel farqi | Visa/Mastercard/Humo/UzCard'ning o'z EMV Kernel spetsifikatsiyasi bor | Har bir sxema uchun mos GPO format (`80`/`77`) parslash |
| Ulanish uzilishi | Karta juda tez olib qo'yiladi | `IsoDep.setTimeout()` va foydalanuvchiga "kartani ushlab turing" ko'rsatmasi |

---

## Xavfsizlik chegarasi — nima yetishmaydi

Bu ketma-ketlik faqat **ochiq (public) ma'lumot**ni o'qiydi: PAN, muddat,
ba'zan ism. Haqiqiy to'lov uchun terminal qo'shimcha bosqich qo'shadi:

- **DDA/CDA (Dynamic/Combined Data Authentication)** — terminal tasodifiy
  son yuboradi, karta uni o'zining **maxfiy kaliti** bilan imzolab qaytaradi.
- Bu imzoni faqat bank serveri tekshira oladi.
- CVV va PIN hech qachon NFC orqali uzatilmaydi.

Oddiy skaner ilovasi shu DDA/CDA bosqichigacha yetmaydi — u faqat
identifikatsiya darajasida to'xtaydi, to'lov qila olmaydi.

---

## To'liq ketma-ketlik — qisqacha sxema

```
SELECT PPSE ("2PAY.SYS.DDF01")
        ↓ (AID topiladi)
SELECT AID
        ↓ (PDOL qaytadi, agar bo'lsa)
GET PROCESSING OPTIONS (GPO)
        ↓ (AFL qaytadi)
READ RECORD  (AFL bo'yicha har bir yozuv)
        ↓
TLV parsing → Track2 ajratish
        ↓
PAN + Muddat + Ism + Karta turi
```
