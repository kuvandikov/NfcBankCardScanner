package io.github.kuvandikov.scancardnfc.util

internal data class SimpleDate(val year: Int, val month: Int, val day: Int)

/** Local calendar date of the device, used for the Transaction Date (9A) in the PDOL. */
internal expect fun currentDate(): SimpleDate
