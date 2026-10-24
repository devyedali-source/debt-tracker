package com.elyedali.debttracker.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elyedali.debttracker.App
import com.elyedali.debttracker.billing.BillingManager
import com.elyedali.debttracker.data.Entry
import com.elyedali.debttracker.data.LedgerData
import com.elyedali.debttracker.data.LedgerJson
import com.elyedali.debttracker.data.Prefs
import com.elyedali.debttracker.data.Repository
import com.elyedali.debttracker.util.PdfExporter
import com.elyedali.debttracker.util.Share
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val FREE_PERSON_LIMIT = 3

sealed interface Screen {
    data object Home : Screen
    data class PersonDetail(val id: String) : Screen
    data object Settings : Screen
    data object Paywall : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository.get(app)
    private val prefs = Prefs(app)
    val billing: BillingManager = (app as App).billing

    val data: StateFlow<LedgerData> = repo.data
    val isPremium: StateFlow<Boolean> = billing.isPremium
    val price: StateFlow<String?> = billing.price

    var currency by mutableStateOf(prefs.currencyCode)
        private set
    var lockEnabled by mutableStateOf(prefs.lockEnabled)
        private set

    var locked by mutableStateOf(prefs.lockEnabled && billing.isPremium.value)
    var leftAt = 0L

    val backStack = mutableStateListOf<Screen>(Screen.Home)

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun lockActive(): Boolean = lockEnabled && isPremium.value

    fun canAddPerson(): Boolean = isPremium.value || data.value.persons.size < FREE_PERSON_LIMIT

    fun addPerson(name: String, phone: String, onAdded: (String) -> Unit) {
        viewModelScope.launch { onAdded(repo.addPerson(name, phone)) }
    }

    fun updatePerson(id: String, name: String, phone: String) {
        viewModelScope.launch { repo.updatePerson(id, name, phone) }
    }

    fun deletePerson(id: String) {
        viewModelScope.launch { repo.deletePerson(id) }
    }

    fun addEntry(personId: String, amountCents: Long, gave: Boolean, note: String, date: Long, dueDate: Long?) {
        viewModelScope.launch { repo.addEntry(personId, amountCents, gave, note, date, dueDate) }
    }

    fun updateEntry(entry: Entry) {
        val old = data.value.entries.firstOrNull { it.id == entry.id }
        if (old?.dueDate != entry.dueDate) prefs.clearNotified(entry.id)
        viewModelScope.launch { repo.updateEntry(entry) }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch { repo.deleteEntry(id) }
    }

    fun changeCurrency(code: String) {
        prefs.currencyCode = code
        currency = code
    }

    fun changeLock(context: Context, on: Boolean) {
        if (on) {
            val auth = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            if (BiometricManager.from(context).canAuthenticate(auth) != BiometricManager.BIOMETRIC_SUCCESS) {
                toast(context, "Please set up a screen lock or fingerprint on your phone first")
                return
            }
        }
        prefs.lockEnabled = on
        lockEnabled = on
    }

    fun setDebugPremium(on: Boolean) = billing.setDebugPremium(on)

    fun sharePdf(context: Context, personId: String?) {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    PdfExporter.export(context.applicationContext, data.value, currency, personId)
                }
                Share.file(context, file, "application/pdf", "Share PDF")
            } catch (e: Exception) {
                toast(context, "Could not create the PDF")
            }
        }
    }

    fun backupTo(context: Context, uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(LedgerJson.encode(data.value).toByteArray())
                    } != null
                } catch (e: Exception) {
                    false
                }
            }
            toast(context, if (ok) "Backup saved" else "Could not save the backup")
        }
    }

    fun readBackup(context: Context, uri: Uri, onResult: (LedgerData) -> Unit) {
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use {
                        LedgerJson.decode(it.readBytes().decodeToString())
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (parsed == null) toast(context, "This file is not a Debt Tracker backup") else onResult(parsed)
        }
    }

    fun restore(context: Context, newData: LedgerData) {
        viewModelScope.launch {
            repo.replaceAll(newData)
            backStack.clear()
            backStack.add(Screen.Home)
            toast(context, "Data restored")
        }
    }

    private fun toast(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}
