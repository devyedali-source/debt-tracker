package com.elyedali.debttracker.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object Money {
    /** Formats the absolute value of [cents] in the chosen currency. */
    fun format(cents: Long, code: String): String {
        val nf = NumberFormat.getCurrencyInstance(Locale.getDefault())
        try {
            val currency = Currency.getInstance(code)
            nf.currency = currency
            val digits = currency.defaultFractionDigits.coerceAtLeast(0)
            nf.minimumFractionDigits = digits
            nf.maximumFractionDigits = digits
        } catch (e: Exception) {
            // Keep the locale default.
        }
        return nf.format(abs(cents) / 100.0)
    }

    /** Amount as plain text for editing, e.g. 1250 -> "12.5". */
    fun plain(cents: Long): String =
        BigDecimal.valueOf(cents).movePointLeft(2).stripTrailingZeros().toPlainString()

    /** Parses user input such as "12.50", "12,50" or "1,200.75". Returns cents or null. */
    fun parse(input: String): Long? {
        var t = input.trim().replace(" ", "").replace(" ", "")
        if (t.isEmpty()) return null
        t = if (t.contains('.') && t.contains(',')) t.replace(",", "") else t.replace(',', '.')
        val value = t.toBigDecimalOrNull() ?: return null
        if (value.signum() <= 0) return null
        if (value > BigDecimal("1000000000000")) return null
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
    }
}

object Dates {
    fun format(ms: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))

    /** Local time -> UTC midnight of the same calendar day (what the date picker expects). */
    fun localToPicker(ms: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = ms }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        return utc.timeInMillis
    }

    /** Date picker value (UTC midnight) -> local time on that day at [hour]. */
    fun pickerToLocal(utcMs: Long, hour: Int): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMs }
        val local = Calendar.getInstance()
        local.clear()
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), hour, 0)
        return local.timeInMillis
    }
}

fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
