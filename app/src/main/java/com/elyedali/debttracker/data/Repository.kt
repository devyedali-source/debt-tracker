package com.elyedali.debttracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** All data lives in one JSON file on the phone. No server. */
class Repository private constructor(context: Context) {

    private val file = File(context.filesDir, "ledger.json")
    private val mutex = Mutex()
    private val _data = MutableStateFlow(load())
    val data: StateFlow<LedgerData> = _data

    private fun load(): LedgerData {
        if (!file.exists()) return LedgerData()
        return try {
            LedgerJson.decode(file.readText())
        } catch (e: Exception) {
            // Keep the unreadable file aside instead of overwriting it.
            file.copyTo(File(file.parentFile, "ledger-unreadable-${System.currentTimeMillis()}.json"), overwrite = true)
            LedgerData()
        }
    }

    private suspend fun update(transform: (LedgerData) -> LedgerData) {
        mutex.withLock {
            val updated = transform(_data.value)
            _data.value = updated
            withContext(Dispatchers.IO) {
                val tmp = File(file.parentFile, "ledger.json.tmp")
                tmp.writeText(LedgerJson.encode(updated))
                if (!tmp.renameTo(file)) {
                    file.writeText(tmp.readText())
                    tmp.delete()
                }
            }
        }
    }

    suspend fun addPerson(name: String, phone: String): String {
        val id = UUID.randomUUID().toString()
        update { it.copy(persons = it.persons + Person(id, name, phone, System.currentTimeMillis())) }
        return id
    }

    suspend fun updatePerson(id: String, name: String, phone: String) = update { d ->
        d.copy(persons = d.persons.map { if (it.id == id) it.copy(name = name, phone = phone) else it })
    }

    suspend fun deletePerson(id: String) = update { d ->
        d.copy(persons = d.persons.filterNot { it.id == id }, entries = d.entries.filterNot { it.personId == id })
    }

    suspend fun addEntry(personId: String, amountCents: Long, gave: Boolean, note: String, date: Long, dueDate: Long?) {
        val entry = Entry(
            id = UUID.randomUUID().toString(),
            personId = personId,
            amountCents = amountCents,
            gave = gave,
            note = note,
            date = date,
            dueDate = dueDate,
            createdAt = System.currentTimeMillis(),
        )
        update { it.copy(entries = it.entries + entry) }
    }

    suspend fun updateEntry(entry: Entry) = update { d ->
        d.copy(entries = d.entries.map { if (it.id == entry.id) entry else it })
    }

    suspend fun deleteEntry(id: String) = update { d ->
        d.copy(entries = d.entries.filterNot { it.id == id })
    }

    suspend fun replaceAll(newData: LedgerData) = update { newData }

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context.applicationContext).also { instance = it }
            }
    }
}
