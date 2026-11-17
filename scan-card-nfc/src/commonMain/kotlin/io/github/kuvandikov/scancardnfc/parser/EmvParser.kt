package io.github.kuvandikov.scancardnfc.parser

import io.github.kuvandikov.scancardnfc.EmvReaderConfig
import io.github.kuvandikov.scancardnfc.model.CardExpiry
import io.github.kuvandikov.scancardnfc.model.EmvCard
import io.github.kuvandikov.scancardnfc.model.EmvCardScheme
import io.github.kuvandikov.scancardnfc.model.NfcErrorType
import io.github.kuvandikov.scancardnfc.provider.CardProvider
import io.github.kuvandikov.scancardnfc.util.hexToBytes
import io.github.kuvandikov.scancardnfc.util.toHex
import io.github.kuvandikov.scancardnfc.util.toUnsigned
import io.github.kuvandikov.scancardnfc.util.wipe

/**
 * Drives the EMV contactless dialogue:
 *
 * ```
 * SELECT PPSE → SELECT AID → GET PROCESSING OPTIONS → READ RECORD (per AFL)
 * ```
 *
 * Platform independent: all I/O goes through [CardProvider].
 */
internal class EmvParser(
    private val provider: CardProvider,
    private val config: EmvReaderConfig,
    private val pdolBuilder: PdolBuilder = PdolBuilder(config),
) {

    private class Candidate(val aid: ByteArray, val label: String?, val priority: Int)

    suspend fun readCard(): EmvCard {
        val ppse = send(ApduCommands.selectPpse())
        // EMV Book 1, 12.3.2: 6A81 to SELECT PPSE means the card is blocked (or has no PPSE).
        // Still try the known AIDs and only report a blocked card if none of them can be read.
        var blocked: String? = if (ppse.sw == SW_CARD_BLOCKED) "Card is blocked" else null

        val candidates = parsePpse(ppse).ifEmpty {
            EmvCardScheme.knownAids.map { Candidate(it.hexToBytes(), null, Int.MAX_VALUE) }
        }

        var lastError: EmvException? = null
        for (candidate in candidates) {
            try {
                readApplication(candidate)?.let { return it }
            } catch (e: EmvException) {
                lastError = e
            } catch (e: CardBlockedException) {
                blocked = e.message
            }
        }
        lastError?.let { throw it }
        blocked?.let { throw CardBlockedException(it) }
        throw EmvException(NfcErrorType.NO_PAYMENT_APPLICATION, "No payment application found")
    }

    private fun parsePpse(response: ApduResponse): List<Candidate> {
        if (!response.isSuccess) return emptyList()
        val tlv = TlvParser.parse(response.data)
        return tlv.findAll(EmvTags.APPLICATION_TEMPLATE)
            .mapNotNull { app ->
                val aid = app.find(EmvTags.AID)?.value ?: return@mapNotNull null
                Candidate(
                    aid = aid,
                    label = app.find(EmvTags.APPLICATION_LABEL)?.value?.decodeAscii(),
                    priority = app.find(EmvTags.APPLICATION_PRIORITY)?.value?.firstOrNull()
                        ?.let { it.toUnsigned() and 0x0F }?.takeIf { it != 0 } ?: Int.MAX_VALUE,
                )
            }
            .sortedBy { it.priority }
    }

    /**
     * Returns `null` when the application cannot be selected, so the next candidate is tried.
     * Throws [CardBlockedException] when the application is blocked.
     */
    private suspend fun readApplication(candidate: Candidate): EmvCard? {
        val select = send(ApduCommands.select(candidate.aid))
        if (select.sw == SW_APP_BLOCKED || select.sw == SW_CARD_BLOCKED) {
            throw CardBlockedException("Application ${candidate.aid.toHex()} is blocked")
        }
        if (!select.isSuccess) return null

        val fci = TlvParser.parse(select.data)
        val label = fci.find(EmvTags.APPLICATION_PREFERRED_NAME)?.value?.decodeAscii()
            ?: fci.find(EmvTags.APPLICATION_LABEL)?.value?.decodeAscii()
            ?: candidate.label
        val pdol = fci.find(EmvTags.PDOL)?.value?.let(TlvParser::parseDol).orEmpty()

        val gpo = send(ApduCommands.getProcessingOptions(pdolBuilder.build(pdol)))
        if (!gpo.isSuccess) {
            throw EmvException(NfcErrorType.COMMAND_FAILED, "GPO failed with SW ${gpo.sw.toString(16).uppercase()}")
        }

        val collected = mutableListOf<Tlv>()
        val afl: ByteArray? = parseGpo(gpo.data, collected)

        if (afl != null) {
            for (entry in afl.toList().chunked(4)) {
                if (entry.size < 4) break
                val sfi = entry[0].toUnsigned() shr 3
                val first = entry[1].toUnsigned()
                val last = entry[2].toUnsigned()
                for (record in first..last) {
                    readRecord(sfi, record)?.let { collected += it }
                }
                if (collected.hasPan()) break
            }
        }
        if (!collected.hasPan()) bruteForceRecords(collected)

        val card = buildCard(collected, candidate.aid.toHex(), label)
        collected.forEach { it.wipeAll() }
        return card ?: throw EmvException(NfcErrorType.CARD_DATA_NOT_FOUND, "PAN not found on the card")
    }

    /** Returns the AFL and adds any card data embedded in the GPO response to [out]. */
    private fun parseGpo(data: ByteArray, out: MutableList<Tlv>): ByteArray? {
        val tlv = TlvParser.parse(data)
        tlv.find(EmvTags.RESPONSE_FORMAT_1)?.let { format1 ->
            // Format 1: AIP (2 bytes) followed by AFL.
            val value = format1.value
            return if (value.size > 2) value.copyOfRange(2, value.size) else null
        }
        val format2 = tlv.find(EmvTags.RESPONSE_FORMAT_2) ?: return null
        out += format2
        return format2.find(EmvTags.AFL)?.value
    }

    private suspend fun readRecord(sfi: Int, record: Int): Tlv? {
        if (sfi !in 1..30 || record !in 1..255) return null
        val response = send(ApduCommands.readRecord(sfi, record))
        if (!response.isSuccess) return null
        return TlvParser.parse(response.data).firstOrNull { it.tag == EmvTags.RECORD_TEMPLATE }
    }

    /** Fallback for cards without a usable AFL. */
    private suspend fun bruteForceRecords(out: MutableList<Tlv>) {
        for (sfi in 1..config.maxFallbackSfi) {
            for (record in 1..config.maxFallbackRecords) {
                val response = send(ApduCommands.readRecord(sfi, record))
                if (response.sw == 0x6A82) break        // file not found
                if (!response.isSuccess) break          // 6A83 record not found, etc.
                TlvParser.parse(response.data).firstOrNull { it.tag == EmvTags.RECORD_TEMPLATE }?.let { out += it }
                if (out.hasPan()) return
            }
        }
    }

    private fun List<Tlv>.hasPan(): Boolean =
        find(EmvTags.PAN) != null || find(EmvTags.TRACK2) != null || find(EmvTags.TRACK2_MC) != null

    private fun buildCard(data: List<Tlv>, aid: String, label: String?): EmvCard? {
        val track2 = (data.find(EmvTags.TRACK2) ?: data.find(EmvTags.TRACK2_MC))
            ?.value?.let(Track2Decoder::decode)

        val pan = data.find(EmvTags.PAN)?.value?.toHex()?.trimEnd('F')?.takeIf { p -> p.all { it.isDigit() } }
            ?: track2?.pan
            ?: return null

        val expiry: CardExpiry? = data.find(EmvTags.EXPIRY_DATE)?.value?.toHex()
            ?.let { Track2Decoder.parseYyMm(it.take(4)) }
            ?: track2?.expiry

        val holder = data.find(EmvTags.CARDHOLDER_NAME)?.value?.decodeAscii()
            ?.trim()?.takeUnless { it.isEmpty() || it == "/" }

        return EmvCard(
            pan = pan,
            expiry = expiry,
            scheme = EmvCardScheme.detect(pan, aid),
            holderName = holder,
            aid = aid,
            applicationLabel = label,
        )
    }

    /** Sends a command and handles the transport-level status words 61XX and 6CXX. */
    private suspend fun send(command: ApduCommand): ApduResponse {
        var response = ApduResponse.parse(provider.transceive(command.toByteArray()))

        if (response.sw1 == 0x6C) {
            // Wrong Le: repeat with the exact length the card asked for.
            response = ApduResponse.parse(provider.transceive(command.withLe(response.sw2).toByteArray()))
        }

        var data = response.data
        var guard = 0
        while (response.sw1 == 0x61 && guard++ < 16) {
            response = ApduResponse.parse(provider.transceive(ApduCommands.getResponse(response.sw2).toByteArray()))
            data += response.data
        }
        return ApduResponse(data, response.sw1, response.sw2)
    }

    private fun ByteArray.decodeAscii(): String =
        map { b -> b.toUnsigned().let { if (it in 0x20..0x7E) it.toChar() else ' ' } }
            .joinToString("").trim()

    private fun Tlv.wipeAll() {
        value.wipe()
        children.forEach { it.wipeAll() }
    }

    private companion object {
        /** Function not supported: the card is blocked (to SELECT). */
        const val SW_CARD_BLOCKED = 0x6A81

        /** Selected file invalidated: the application is blocked. */
        const val SW_APP_BLOCKED = 0x6283
    }
}
