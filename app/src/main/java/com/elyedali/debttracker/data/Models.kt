package com.elyedali.debttracker.data

import org.json.JSONArray
import org.json.JSONObject

data class Person(
    val id: String,
    val name: String,
    val phone: String,
    val createdAt: Long,
)

/**
 * One money movement with a person.
 * gave = true  -> "You gave" (they owe you more)
 * gave = false -> "You got"  (you owe them more, or they paid you back)
 */
data class Entry(
    val id: String,
    val personId: String,
    val amountCents: Long,
    val gave: Boolean,
    val note: String,
    val date: Long,
    val dueDate: Long?,
    val createdAt: Long,
)

data class LedgerData(
    val persons: List<Person> = emptyList(),
    val entries: List<Entry> = emptyList(),
) {
    private val balanceMap: Map<String, Long> by lazy {
        val map = HashMap<String, Long>()
        entries.forEach { e ->
            map[e.personId] = (map[e.personId] ?: 0L) + if (e.gave) e.amountCents else -e.amountCents
        }
        map
    }

    /** Positive: they owe you. Negative: you owe them. */
    fun balanceOf(personId: String): Long = balanceMap[personId] ?: 0L

    fun entriesOf(personId: String): List<Entry> =
        entries.filter { it.personId == personId }
            .sortedWith(compareByDescending<Entry> { it.date }.thenByDescending { it.createdAt })

    fun lastActivity(personId: String): Long {
        val last = entries.filter { it.personId == personId }.maxOfOrNull { it.createdAt }
        val created = persons.firstOrNull { it.id == personId }?.createdAt ?: 0L
        return maxOf(last ?: 0L, created)
    }

    val totalToGet: Long get() = persons.sumOf { balanceOf(it.id).coerceAtLeast(0L) }
    val totalToGive: Long get() = persons.sumOf { (-balanceOf(it.id)).coerceAtLeast(0L) }
}

object LedgerJson {
    fun encode(data: LedgerData): String {
        val root = JSONObject()
        root.put("app", "debt-tracker")
        root.put("version", 1)
        val persons = JSONArray()
        data.persons.forEach { p ->
            persons.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("phone", p.phone)
                    .put("createdAt", p.createdAt)
            )
        }
        val entries = JSONArray()
        data.entries.forEach { e ->
            val o = JSONObject()
                .put("id", e.id)
                .put("personId", e.personId)
                .put("amountCents", e.amountCents)
                .put("gave", e.gave)
                .put("note", e.note)
                .put("date", e.date)
                .put("createdAt", e.createdAt)
            if (e.dueDate != null) o.put("dueDate", e.dueDate)
            entries.put(o)
        }
        root.put("persons", persons)
        root.put("entries", entries)
        return root.toString()
    }

    /** Throws if the text is not a valid backup. */
    fun decode(text: String): LedgerData {
        val root = JSONObject(text)
        require(root.has("persons") && root.has("entries")) { "Not a Debt Tracker file" }
        val pa = root.getJSONArray("persons")
        val persons = (0 until pa.length()).map { i ->
            val o = pa.getJSONObject(i)
            Person(
                id = o.getString("id"),
                name = o.getString("name"),
                phone = o.optString("phone", ""),
                createdAt = o.optLong("createdAt", 0L),
            )
        }
        val ids = persons.map { it.id }.toSet()
        val ea = root.getJSONArray("entries")
        val entries = (0 until ea.length()).map { i ->
            val o = ea.getJSONObject(i)
            val due = o.optLong("dueDate", -1L)
            Entry(
                id = o.getString("id"),
                personId = o.getString("personId"),
                amountCents = o.getLong("amountCents"),
                gave = o.getBoolean("gave"),
                note = o.optString("note", ""),
                date = o.getLong("date"),
                dueDate = if (due > 0) due else null,
                createdAt = o.optLong("createdAt", o.getLong("date")),
            )
        }.filter { it.personId in ids }
        return LedgerData(persons, entries)
    }
}
