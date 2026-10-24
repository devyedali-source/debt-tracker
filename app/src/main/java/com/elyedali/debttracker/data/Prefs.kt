package com.elyedali.debttracker.data

import android.content.Context
import java.util.Currency
import java.util.Locale

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var currencyCode: String
        get() = sp.getString("currency", null) ?: defaultCurrency()
        set(value) = sp.edit().putString("currency", value).apply()

    var lockEnabled: Boolean
        get() = sp.getBoolean("lock", false)
        set(value) = sp.edit().putBoolean("lock", value).apply()

    var premiumCached: Boolean
        get() = sp.getBoolean("premium", false)
        set(value) = sp.edit().putBoolean("premium", value).apply()

    /** Only used in debug builds to test PRO screens without a real purchase. */
    var debugPremium: Boolean
        get() = sp.getBoolean("debug_premium", false)
        set(value) = sp.edit().putBoolean("debug_premium", value).apply()

    fun isNotified(key: String): Boolean =
        sp.getStringSet("notified", emptySet())?.contains(key) == true

    fun markNotified(key: String) {
        val set = HashSet(sp.getStringSet("notified", emptySet()) ?: emptySet())
        set.add(key)
        sp.edit().putStringSet("notified", set).apply()
    }

    fun clearNotified(key: String) {
        val set = HashSet(sp.getStringSet("notified", emptySet()) ?: emptySet())
        if (set.remove(key)) sp.edit().putStringSet("notified", set).apply()
    }

    companion object {
        fun defaultCurrency(): String = try {
            Currency.getInstance(Locale.getDefault()).currencyCode
        } catch (e: Exception) {
            "USD"
        }
    }
}
