@file:OptIn(ExperimentalMaterial3Api::class)

package com.elyedali.debttracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.elyedali.debttracker.data.Entry
import com.elyedali.debttracker.ui.theme.GetColor
import com.elyedali.debttracker.ui.theme.GiveColor
import com.elyedali.debttracker.util.Dates
import com.elyedali.debttracker.util.Money
import java.util.Currency

@Composable
fun PersonDialog(
    title: String,
    initialName: String = "",
    initialPhone: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var phone by rememberSaveable { mutableStateOf(initialPhone) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = false },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = error,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    supportingText = { Text("With country code, e.g. +1 555 123 4567") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) error = true else onSave(name.trim(), phone.trim())
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun EntryDialog(
    gave: Boolean,
    existing: Entry?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (amountCents: Long, note: String, date: Long, dueDate: Long?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var amountText by rememberSaveable { mutableStateOf(existing?.let { Money.plain(it.amountCents) } ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var date by rememberSaveable { mutableLongStateOf(existing?.date ?: System.currentTimeMillis()) }
    var dueDate by rememberSaveable { mutableStateOf(existing?.dueDate) }
    var picking by remember { mutableStateOf(0) } // 0 none, 1 date, 2 due date
    var error by remember { mutableStateOf(false) }
    val accent = if (gave) GiveColor else GetColor

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (gave) "You gave" else "You got",
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = false },
                    label = { Text("Amount ($currency)") },
                    singleLine = true,
                    isError = error,
                    supportingText = { if (error) Text("Enter a valid amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField("Date", Dates.format(date), onClick = { picking = 1 })
                DateField(
                    label = "Due date (optional)",
                    value = dueDate?.let { Dates.format(it) } ?: "Not set",
                    onClick = { picking = 2 },
                    onClear = if (dueDate != null) ({ dueDate = null }) else null,
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, null, tint = GiveColor)
                        Spacer(Modifier.width(6.dp))
                        Text("Delete entry", color = GiveColor)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = Money.parse(amountText)
                if (cents == null) error = true else onSave(cents, note.trim(), date, dueDate)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    when (picking) {
        1 -> DatePickerModal(
            initialUtc = Dates.localToPicker(date),
            onPick = { utc -> date = Dates.pickerToLocal(utc, 12) },
            onDismiss = { picking = 0 },
        )
        2 -> DatePickerModal(
            initialUtc = dueDate?.let { Dates.localToPicker(it) },
            onPick = { utc -> dueDate = Dates.pickerToLocal(utc, 9) },
            onDismiss = { picking = 0 },
        )
    }
}

@Composable
private fun DateField(label: String, value: String, onClick: () -> Unit, onClear: (() -> Unit)? = null) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CalendarToday, null, Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            if (onClear != null) {
                IconButton(onClick = onClear) { Icon(Icons.Filled.Close, contentDescription = "Clear") }
            }
        }
    }
}

@Composable
private fun DatePickerModal(initialUtc: Long?, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let(onPick)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = GiveColor) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun CurrencyDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val all = remember { Currency.getAvailableCurrencies().sortedBy { it.currencyCode } }
    var query by remember { mutableStateOf("") }
    val list = remember(query) {
        all.filter {
            query.isBlank() ||
                it.currencyCode.contains(query, ignoreCase = true) ||
                it.displayName.contains(query, ignoreCase = true)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp)) {
                Text("Currency", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search, e.g. USD or Euro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(list, key = { it.currencyCode }) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(c.currencyCode) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                c.currencyCode,
                                modifier = Modifier.width(56.dp),
                                fontWeight = if (c.currencyCode == current) FontWeight.Bold else FontWeight.Normal,
                                color = if (c.currencyCode == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(c.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
