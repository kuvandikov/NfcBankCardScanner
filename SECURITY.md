# Security policy

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Use GitHub's private vulnerability reporting: **Security → Report a vulnerability** on
https://github.com/kuvandikov/NfcBankCardScanner. Include the library version, the device and Android version,
and steps to reproduce. Do not include real card numbers — mask them (`4111 11•• •••• 1111`).

You will get a reply within 7 days. Fixes are released as patch versions and announced in
[CHANGELOG.md](CHANGELOG.md).

## Supported versions

| Version | Supported |
|---|---|
| 1.x | ✅ |
| 0.1.x | ❌ — please migrate |

## What the library does with card data

- Reads only public EMV data over NFC: PAN, expiry date, sometimes the cardholder name.
  CVV2/CVC2 and PIN are not readable over NFC.
- Does not log, cache, persist or transmit card data. The library declares no `INTERNET` permission.
- Zeroes raw APDU response buffers after parsing. The PAN is a `String` and cannot be zeroed; drop references
  to `EmvCard` as soon as the card is attached.
- Signing keys and Maven Central credentials are stored only in GitHub Secrets.
