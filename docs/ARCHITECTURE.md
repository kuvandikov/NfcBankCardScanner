# Architecture

The original planning document (in Uzbek) is kept in [PLAN.uz.md](PLAN.uz.md).

## Layers

```
┌──────────────────────────────────────────────────────────┐
│ Client app (Compose Multiplatform / SwiftUI)             │
└───────────────────────────┬──────────────────────────────┘
                            ▼
┌──────────────────────────────────────────────────────────┐
│ Public API:  NfcCardScanner · EmvReader · NfcScanResult  │
└──────────────┬───────────────────────────┬───────────────┘
               ▼ commonMain                ▼ commonMain
┌──────────────────────────┐  ┌───────────────────────────────┐
│ Domain                   │  │ Core engine (internal)        │
│ EmvCard, EmvCardScheme,  │  │ ApduCommand, TlvParser,       │
│ NfcScanResult,           │  │ PdolBuilder, Track2Decoder,   │
│ CardProvider (interface) │  │ EmvParser                     │
└──────────────▲───────────┘  └───────────────────────────────┘
               │ implements
┌──────────────┴───────────────┐  ┌───────────────────────────────┐
│ androidMain                  │  │ iosMain                       │
│ AndroidCardProvider (IsoDep) │  │ IosCardProvider (NFCISO7816Tag)│
│ AndroidNfcScanner            │  │ IosNfcScanner (NFCTagReader-  │
│ (NfcAdapter reader mode)     │  │ Session)                      │
└──────────────────────────────┘  └───────────────────────────────┘
```

- **`CardProvider`** is the only seam between the engine and the platform: `suspend fun transceive(ByteArray): ByteArray`.
  Response bytes always include SW1 SW2. Losing the card throws `CardConnectionLostException`.
- **`EmvReader`** runs the engine on any `CardProvider` and maps every failure to `NfcScanResult` — it never throws
  except for coroutine cancellation.
- **`NfcCardScanner`** is the high-level API: it owns the platform session, waits for a card, connects it and
  delegates to `EmvReader`.
- **`expect/actual`** is used only where the platform must supply a value (`currentDate()` for the PDOL tag 9A).
  Platform types are plain classes so that their constructors can take platform arguments (e.g. `Activity`).

## EMV flow (`EmvParser`)

1. `SELECT 2PAY.SYS.DDF01` (PPSE) → list of applications (tag 61), ordered by priority (87).
   If PPSE is missing, a list of well-known AIDs is tried.
2. `SELECT AID` → FCI; read label (50/9F12) and PDOL (9F38).
3. `GET PROCESSING OPTIONS` with terminal data built by `PdolBuilder` (country/currency default to 860 — Uzbekistan).
4. Response format 1 (80: AIP + AFL) or format 2 (77: may already contain Track 2).
5. `READ RECORD` for every AFL entry; if there is no AFL, a bounded SFI/record scan.
6. PAN from 5A, otherwise from Track 2 (57 / 9F6B). Expiry from 5F24, otherwise Track 2.
7. Scheme: domestic BIN (Uzcard `8600`, Humo `9860`) → AID (longest RID match) → BIN regex.

Transport status words are handled in one place: `6CXX` (retry with correct Le) and `61XX` (GET RESPONSE).

## Security

- Only public EMV data is read. CVV2/CVC2 and PIN are not available over NFC.
- Track 2 discretionary data (may contain dCVV) is discarded by `Track2Decoder`.
- Raw TLV buffers are zeroed (`wipe()`) as soon as `EmvCard` is built.
- `EmvCard.pan` is a `String` and cannot be zeroed; consumers should drop references quickly and never log
  `card.pan` (use `maskedPan`; `toString()` already masks).

## Testing

`commonTest` drives `EmvReader` with `FakeCardProvider`, which replays scripted APDU responses. Test fixtures
are built with a small `tlv()` helper so lengths are always correct. Run on JVM for speed:
`./gradlew :scan-card-nfc:jvmTest`.
