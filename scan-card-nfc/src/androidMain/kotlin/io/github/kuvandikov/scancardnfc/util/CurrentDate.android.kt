package io.github.kuvandikov.scancardnfc.util

import java.util.Calendar

internal actual fun currentDate(): SimpleDate {
    val c = Calendar.getInstance()
    return SimpleDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}
