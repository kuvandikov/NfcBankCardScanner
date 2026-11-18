# ADR 0001: Android-only library

- **Status:** Proposed — waiting for the iOS device test below
- **Date:** 2026-11-18
- **Deciders:** maintainers

## Context

The previous plan ([archive/KMP_NFC_Card_Reader_Architecture.md](../archive/KMP_NFC_Card_Reader_Architecture.md))
targeted Kotlin Multiplatform for Android and iOS.

The main use case is attaching a card in a bank app: the user taps the card, the app fills in the card number
and expiry date. Most users are in Uzbekistan and carry Humo or Uzcard cards.

On iOS, Core NFC blocks payment-card AIDs (Visa, Mastercard and others). `NFCPaymentTagReaderSession`
(iOS 26) works only in the EU, for EU accounts and devices, with a separate entitlement from Apple. It is not
available to users in Uzbekistan. It is still unknown whether the **domestic** Humo/Uzcard AIDs are blocked too.

## Test (before the decision becomes final)

1. On Android, read 3–4 real Humo and Uzcard cards and record the AIDs (tag `4F`) from the PPSE response.
2. In the KMP iOS demo's `Info.plist`, put **only** those domestic AIDs into
   `com.apple.developer.nfc.readersession.iso7816.select-identifiers` (remove PPSE, Visa and Mastercard).
3. Tap the cards on a real iPhone.

| Result | Decision |
|---|---|
| Card number was read | Keep Kotlin Multiplatform. This ADR is rejected. |
| `Missing required entitlement`, or the tag is not detected | Accept this ADR: Android-only. |

**Result:** _not run yet — fill in date, devices, iOS version, cards and outcome._

## Decision (if accepted)

Ship an Android-only library. Keep the EMV engine in the pure Kotlin/JVM module `emv-core` so that moving it
to `commonMain` is a mechanical change if iOS access opens up.

## Consequences

- Simpler build: no Kotlin/Native, no Xcode project, no iOS CI runner.
- iOS apps cannot use the library; the README answers "Why no iOS?" with a link to this ADR.
- The Kotlin Foundation grant (focused on Multiplatform) may no longer fit; check current terms.
- The iOS code stays available in git history of the KMP branch.
