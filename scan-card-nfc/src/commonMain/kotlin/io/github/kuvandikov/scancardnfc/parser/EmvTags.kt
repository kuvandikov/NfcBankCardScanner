package io.github.kuvandikov.scancardnfc.parser

/** EMV tags used by the reader (EMV Book 3, Annex A). */
internal object EmvTags {
    const val FCI_TEMPLATE = 0x6F
    const val DF_NAME = 0x84
    const val FCI_PROPRIETARY = 0xA5
    const val FCI_ISSUER_DISCRETIONARY = 0xBF0C
    const val APPLICATION_TEMPLATE = 0x61
    const val AID = 0x4F
    const val APPLICATION_LABEL = 0x50
    const val APPLICATION_PREFERRED_NAME = 0x9F12
    const val APPLICATION_PRIORITY = 0x87
    const val PDOL = 0x9F38

    const val RESPONSE_FORMAT_1 = 0x80
    const val RESPONSE_FORMAT_2 = 0x77
    const val AIP = 0x82
    const val AFL = 0x94
    const val RECORD_TEMPLATE = 0x70

    const val PAN = 0x5A
    const val EXPIRY_DATE = 0x5F24
    const val CARDHOLDER_NAME = 0x5F20
    const val TRACK2 = 0x57
    const val TRACK2_MC = 0x9F6B

    // Terminal data requested through the PDOL
    const val TTQ = 0x9F66
    const val AMOUNT_AUTHORISED = 0x9F02
    const val AMOUNT_OTHER = 0x9F03
    const val TERMINAL_COUNTRY_CODE = 0x9F1A
    const val TRANSACTION_CURRENCY_CODE = 0x5F2A
    const val TVR = 0x95
    const val TRANSACTION_DATE = 0x9A
    const val TRANSACTION_TYPE = 0x9C
    const val UNPREDICTABLE_NUMBER = 0x9F37
    const val TERMINAL_TYPE = 0x9F35
    const val TERMINAL_CAPABILITIES = 0x9F33
    const val ADDITIONAL_TERMINAL_CAPABILITIES = 0x9F40
}
