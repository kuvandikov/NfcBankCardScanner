# NFC Bank Card Scanner — Android Library Architecture and Plan

This document describes the architecture and work plan of an open-source **Android** library that reads the
**card number and expiry date** of contactless bank cards (EMV/NFC) for Uzbek banking apps.

It replaces the earlier [Kotlin Multiplatform plan](archive/KMP_NFC_Card_Reader_Architecture.md). Two
projects served as templates: [google/accompanist](https://github.com/google/accompanist) (a published
library: API tracking, docs site, migration guide) and [android/nowinandroid](https://github.com/android/nowinandroid)
(`build-logic` convention plugins, version catalog, Spotless, lint, test layout).

---

## 0. Why the plan changed

- **Main goal: attaching a card in a banking app.** The user taps the card on the phone and the app fills in
  the card number and expiry date. The library does not make payments.
- **Reading payment cards over NFC on iOS is restricted.** Apple blocks payment AIDs (Visa, Mastercard and
  others) in Core NFC. `NFCPaymentTagReaderSession` in iOS 26 works only in the EU, requires an EU account and
  device, and needs a separate permission from Apple. This path is closed for users in Uzbekistan.
- **Conclusion.** If an iPhone cannot read Humo and Uzcard cards, the KMP layer is complexity with no benefit:
  an iOS demo, an Xcode project, a Kotlin/Native build. The library becomes Android-only.

### 0.1. Decision point: the iOS test (1–2 days, before the plan starts)

The plan depends on the result of this test. Do not delete code before running it.

1. On Android, read 3–4 real Humo and Uzcard cards and record the AIDs (tag `4F`) from the PPSE response.
2. In the `Info.plist` of the current iOS demo on `main`, put **only** these domestic AIDs into the
   `select-identifiers` list. Temporarily remove PPSE, Visa and Mastercard.
3. Tap the cards on a real iPhone.

| Result | Decision |
|---|---|
| Card number was read | KMP stays. The current `main` is the base; this document does not apply. |
| `Missing required entitlement` or no tag detected | **Move to an Android-only library according to this plan.** |

Record the result as an ADR in `docs/decisions/0001-android-only.md`. That file answers the question
"why is there no iOS?" later on.

---

## 1. Goal and principles

- **Goal:** a secure, lightweight and easy-to-use NFC card reader library for Android. Visa, Mastercard, Mir,
  UnionPay, **Humo and Uzcard** are supported.
- **Principles:**
  - **EMV logic is platform independent.** TLV, APDU and the EMV flow live in a pure Kotlin/JVM module with
    no Android dependency. Tests run on the JVM, without an emulator, and fast. If iOS becomes possible,
    moving this module to KMP is enough.
  - **A small and stable public API.** `explicitApi()` is enabled and the public API file is kept under
    version control (like `api/current.api` in accompanist).
  - **Card data is never kept anywhere.** It is not written to logs, caches or the network.

---

## 2. Module structure

```
NfcBankCardScanner/
├── build-logic/                         # nowinandroid template
│   └── convention/src/main/kotlin/
│       ├── AndroidLibraryConventionPlugin.kt
│       ├── AndroidLibraryPublishedConventionPlugin.kt   # accompanist template
│       ├── JvmLibraryConventionPlugin.kt
│       └── AndroidApplicationComposeConventionPlugin.kt
├── gradle/libs.versions.toml            # single version catalog
├── emv-core/                            # pure Kotlin/JVM, no Android
│   ├── api/emv-core.api
│   └── src/{main,test}/kotlin/io/github/kuvandikov/scancardnfc/
│       ├── model/      EmvCard, EmvCardScheme, NfcScanResult
│       ├── parser/     TlvParser, ApduCommand, PdolBuilder, EmvParser, Track2Decoder, EmvTags
│       ├── provider/   CardProvider
│       └── EmvReader.kt
├── scan-card-nfc/                       # Android library (main artifact published to Maven)
│   ├── api/scan-card-nfc.api
│   └── src/main/kotlin/io/github/kuvandikov/scancardnfc/
│       ├── provider/AndroidCardProvider.kt      # IsoDep
│       └── scanner/AndroidNfcScanner.kt         # Reader Mode, lifecycle-aware
├── scan-card-nfc-compose/               # optional: rememberNfcCardScanner()
├── internal-testutils/                  # recorded APDU fixtures, FakeCardProvider
├── sample/                              # Compose demo app
├── lint/                                # optional: lint rule that detects logging of the PAN
├── docs/                                # for the mkdocs site (accompanist template)
│   ├── index.md
│   ├── usage.md
│   ├── migration.md                     # 0.1.x (Java) → 1.0 (Kotlin)
│   └── decisions/0001-android-only.md
├── spotless/                            # license header templates
└── .github/workflows/
    ├── build.yml                        # PR: spotless, lint, test, apiCheck
    └── release.yml                      # tag → Maven Central
```

### 2.1. Module responsibilities

| Module | Type | Dependencies | Responsibility |
|---|---|---|---|
| `emv-core` | Kotlin/JVM | `kotlinx-coroutines-core` | EMV flow: PPSE → SELECT AID → GPO → READ RECORD. TLV parsing, scheme detection. |
| `scan-card-nfc` | Android library | `emv-core` (`api`) | `CardProvider` over `IsoDep`, Reader Mode, `suspend fun scan()` returning `NfcScanResult`. |
| `scan-card-nfc-compose` | Android library | `scan-card-nfc`, Compose | `rememberNfcCardScanner()`, helpers for the NFC-off state. |
| `internal-testutils` | JVM, not published | `emv-core` | APDU responses recorded from real cards and **masked**. |
| `sample` | Android app | all modules | Demo and manual testing. |

> **Does `emv-core` have to be a separate artifact?** No. It can also be published inside `scan-card-nfc` as
> a single artifact. The main benefit of a separate module: tests run without Android, and platform
> dependencies are forbidden at the compiler level.

---

## 3. Public API

```kotlin
// scan-card-nfc
public class AndroidNfcScanner(activity: ComponentActivity, config: EmvReaderConfig = EmvReaderConfig()) {
    public val isSupported: Boolean
    public val isEnabled: Boolean
    public suspend fun scan(): NfcScanResult   // cancelling turns Reader Mode off
}

public sealed interface NfcScanResult {
    public data class Success(val card: EmvCard) : NfcScanResult
    public data object CardMovedTooFast : NfcScanResult
    public data object CardBlocked : NfcScanResult
    public data object NfcDisabled : NfcScanResult
    public data object NfcNotSupported : NfcScanResult
    public data object Cancelled : NfcScanResult
    public data class Error(val type: NfcErrorType, val message: String?) : NfcScanResult
}

public class EmvCard(
    public val pan: String,
    public val expiry: YearMonth?,
    public val scheme: EmvCardScheme,
    public val holderName: String?,
) {
    override fun toString(): String = "EmvCard(pan=${maskedPan}, scheme=$scheme)" // the full PAN is never printed
}
```

The existing `NfcCardScanner` interface from `commonMain` can be kept too, so that apps can use a fake scanner
in tests.

---

## 4. Git strategy

- **Do not rewrite history** (no `reset` + `push --force`). The repository is public and has forks and
  Maven users.
- **`git revert -m 1 8cdd69e` is not recommended either.** It returns the code to the old Java version
  (`AsyncTask` and 44 Java files) and the new Kotlin code and tests are lost.
- **Recommended: one forward PR, `refactor: convert to Android-only library`:**
  1. Move the code in `commonMain` to `emv-core/src/main` and `commonTest` to `emv-core/src/test`.
  2. Move `androidMain` to `scan-card-nfc/src/main`.
  3. Delete `iosMain`, `jvmMain`, `app/iosApp` and the iOS part of `app/shared`. `expect/actual`
     (`CurrentDate`) becomes a plain function.
  4. Merge `app/androidApp` and `app/shared` into a single `sample` module.
  5. Delete `docs/KMP_NFC_Card_Reader_Architecture.md` or move it to `docs/archive/`. This document
     replaces it.
- The iOS code stays in git history. If needed, restore it with
  `git checkout 8cdd69e -- scan-card-nfc/src/iosMain`.

---

## 5. Roadmap

### Stage 0: iOS test and decision (1–2 days)
- The test from section 0.1. The result is written as an ADR.
- Remove `key.jks` from the repository and add `*.jks` and `*.keystore` to `.gitignore`. If this key belongs
  to an app on the Play Store, also remove it from git history and rotate the key.

### Stage 1: Build infrastructure (week 1), nowinandroid template
1. `build-logic/convention` and convention plugins: `scancardnfc.android.library`,
   `scancardnfc.android.library.published`, `scancardnfc.jvm.library`.
2. All versions in `gradle/libs.versions.toml`: Kotlin 2.x, AGP, Coroutines.
3. Spotless + ktlint, license header (`spotless/copyright.kt`).
4. `explicitApi()` and the Binary Compatibility Validator (`apiDump`, `apiCheck`). Accompanist does this with
   metalava; either works.
5. Publishing: `com.vanniktech.maven.publish` (accompanist uses it too) and Dokka.
6. `build.yml` CI: `spotlessCheck lint test apiCheck` on every PR.

### Stage 2: Move to Android-only (week 2)
1. The PR from section 4.
2. The artifact coordinate does not change: `io.github.kuvandikov:scan-card-nfc`. 20 companies use this
   coordinate.
3. Set `minSdk` (currently 21; raise it to 23 if `androidx` dependencies require more).
4. `docs/migration.md`: moving from the 0.1.x Java API (`CardNfcAsyncTask`) to `AndroidNfcScanner.scan()` in
   1.0 (accompanist `migration.md` template).

### Stage 3: Humo, Uzcard and real cards (week 3)
1. Add the Humo and Uzcard AIDs to `EmvCardScheme` and the `knownAids` list (the current `TODO`).
2. Co-badged cards: even if the card returns a Visa or Mastercard AID, a BIN of `8600` or `9860` means the
   scheme is Uzcard or Humo (the logic exists; strengthen it with tests).
3. `internal-testutils`: a recorded APDU sequence for every scheme. PAN, name and cryptograms are replaced
   with synthetic values.
4. Errors: card removed early (`TagLostException`), blocked card (`6A81`), cards without an AFL, cards that
   require a PDOL.
5. Device matrix: at least Samsung, Xiaomi and Pixel. Check NFC antenna placement and Reader Mode
   differences.

### Stage 4: Sample app and Compose module (week 4)
1. `sample`: an "add card" screen. The result shows the masked PAN and expiry date, `FLAG_SECURE` is on,
   there is no `INTERNET` permission.
2. `scan-card-nfc-compose` (optional): `rememberNfcCardScanner()` and a helper that opens NFC settings.
3. Integration example for a banking app: `ViewModel` + `NfcScanResult` states + user-facing texts.

### Stage 5: Documentation and the 1.0 release (week 5)
1. `README.md`: short description, installation, a 10-line example, a table of supported cards and a
   "Why no iOS?" section.
2. mkdocs site on GitHub Pages and Dokka API docs (accompanist template).
3. `CHANGELOG.md`, the `v1.0.0` release and Maven Central.
4. A link to the migration guide in the release notes for existing users.

---

## 6. Test strategy

| Level | Where | What is checked |
|---|---|---|
| Unit (JVM) | `emv-core/src/test` | TLV parser, APDU builder, Track 2, scheme detection, PDOL |
| Contract (JVM) | `emv-core/src/test` + `internal-testutils` | Flows of recorded real cards with `FakeCardProvider` |
| Android unit | `scan-card-nfc/src/test` (Robolectric) | Reader Mode on/off, cancellation |
| Manual | `sample` + real cards | The device matrix from stage 3 |

---

## 7. Security

1. **Only public EMV data is read:** PAN, expiry date and (if present) the cardholder name. CVV2/CVC2 and the
   PIN are not and cannot be read from the chip.
2. **No logs.** `EmvCard.toString()` masks the PAN. APDU logs are written only in `debug` mode and masked. An
   optional lint rule detects calls like `Log.*(card.pan)`.
3. **No cache.** The library writes nothing to disk and does not access the network (no `INTERNET`
   permission in the manifest).
4. **Repository hygiene:** keystores and secret keys are stored only in GitHub Secrets (the current
   `release.yaml` already does this).
5. `SECURITY.md`: how to report a vulnerability.

---

## 8. Grants and funding

- The **Kotlin Foundation Grants** program focuses mainly on the Kotlin Multiplatform ecosystem. An
  Android-only library may not qualify; check the current terms.
- The answers in the **Claude for Open Source** application do not change: they did not promise iOS.

---

## 9. Conclusion

The library solves one concrete problem for Uzbek banking apps: attaching a card without typing it in. The
Android-only direction simplifies the project, and the pure Kotlin `emv-core` module keeps the way back to KMP
open if iOS access appears.
