@file:OptIn(ExperimentalMaterial3Api::class)

package com.elyedali.debttracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elyedali.debttracker.data.Entry
import com.elyedali.debttracker.ui.theme.GetColor
import com.elyedali.debttracker.ui.theme.GiveColor
import com.elyedali.debttracker.util.Money
import com.elyedali.debttracker.util.Share

private data class EntryEditor(val gave: Boolean, val entry: Entry?)

@Composable
fun PersonScreen(vm: AppViewModel, personId: String) {
    val context = LocalContext.current
    val data by vm.data.collectAsStateWithLifecycle()
    val premium by vm.isPremium.collectAsStateWithLifecycle()
    val person = data.persons.firstOrNull { it.id == personId }
    if (person == null) {
        LaunchedEffect(personId) { vm.back() }
        return
    }
    val balance = data.balanceOf(person.id)
    val entries = remember(data, personId) { data.entriesOf(personId) }

    var editor by remember { mutableStateOf<EntryEditor?>(null) }
    var showEditPerson by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(person.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (person.phone.isNotBlank()) {
                            Text(
                                person.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (premium) vm.sharePdf(context, person.id) else vm.navigate(Screen.Paywall)
                    }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Share PDF statement")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit person") },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                onClick = { menu = false; showEditPerson = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete person") },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { menu = false; confirmDelete = true },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { editor = EntryEditor(gave = true, entry = null) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GiveColor, contentColor = Color.White),
                    ) { Text("YOU GAVE", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { editor = EntryEditor(gave = false, entry = null) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GetColor, contentColor = Color.White),
                    ) { Text("YOU GOT", fontWeight = FontWeight.Bold) }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                BalanceCard(
                    balance = balance,
                    currency = vm.currency,
                    hasPhone = person.phone.count { it.isDigit() } >= 6,
                    onWhatsApp = { Share.whatsApp(context, person, Share.reminderText(person, balance, vm.currency)) },
                    onSms = { Share.sms(context, person, Share.reminderText(person, balance, vm.currency)) },
                    onOther = { Share.text(context, Share.reminderText(person, balance, vm.currency), "Send reminder") },
                )
            }
            item {
                Text(
                    "Entries",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            if (entries.isEmpty()) {
                item {
                    Text(
                        "No entries yet. Tap \"You gave\" or \"You got\" to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            }
            items(entries, key = { it.id }) { e ->
                EntryRow(e, vm.currency) { editor = EntryEditor(e.gave, e) }
            }
        }
    }

    editor?.let { ed ->
        EntryDialog(
            gave = ed.gave,
            existing = ed.entry,
            currency = vm.currency,
            onDismiss = { editor = null },
            onSave = { amount, note, date, due ->
                val old = ed.entry
                if (old == null) {
                    vm.addEntry(person.id, amount, ed.gave, note, date, due)
                } else {
                    vm.updateEntry(old.copy(amountCents = amount, note = note, date = date, dueDate = due))
                }
                if (due != null) askNotificationPermission()
                editor = null
            },
            onDelete = ed.entry?.let { e -> { vm.deleteEntry(e.id); editor = null } },
        )
    }

    if (showEditPerson) {
        PersonDialog(
            title = "Edit person",
            initialName = person.name,
            initialPhone = person.phone,
            onDismiss = { showEditPerson = false },
            onSave = { name, phone ->
                vm.updatePerson(person.id, name, phone)
                showEditPerson = false
            },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete ${person.name}?",
            text = "This person and all their entries will be deleted. This cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                confirmDelete = false
                vm.deletePerson(person.id)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun BalanceCard(
    balance: Long,
    currency: String,
    hasPhone: Boolean,
    onWhatsApp: () -> Unit,
    onSms: () -> Unit,
    onOther: () -> Unit,
) {
    val color = balanceColor(balance)
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(statusLabel(balance), style = MaterialTheme.typography.titleSmall, color = color)
            Text(
                Money.format(balance, currency),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            if (balance > 0) {
                Spacer(Modifier.size(12.dp))
                Box {
                    FilledTonalButton(onClick = { if (hasPhone) menu = true else onOther() }) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send payment reminder")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("WhatsApp") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, null) },
                            onClick = { menu = false; onWhatsApp() },
                        )
                        DropdownMenuItem(
                            text = { Text("SMS") },
                            leadingIcon = { Icon(Icons.Filled.Sms, null) },
                            onClick = { menu = false; onSms() },
                        )
                        DropdownMenuItem(
                            text = { Text("Other apps") },
                            leadingIcon = { Icon(Icons.Filled.Share, null) },
                            onClick = { menu = false; onOther() },
                        )
                    }
                }
            }
        }
    }
}
