@file:OptIn(ExperimentalMaterial3Api::class)

package com.elyedali.debttracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elyedali.debttracker.ads.AdBanner

@Composable
fun AppRoot(vm: AppViewModel) {
    val screen = vm.backStack.lastOrNull() ?: Screen.Home
    BackHandler(enabled = vm.backStack.size > 1) { vm.back() }
    when (screen) {
        Screen.Home -> HomeScreen(vm)
        is Screen.PersonDetail -> PersonScreen(vm, screen.id)
        Screen.Settings -> SettingsScreen(vm)
        Screen.Paywall -> PaywallScreen(vm)
    }
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val premium by vm.isPremium.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showAdd by rememberSaveable { mutableStateOf(false) }

    val people = remember(data, query) {
        data.persons
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }
            .sortedByDescending { data.lastActivity(it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debt Tracker", fontWeight = FontWeight.Bold) },
                actions = {
                    if (!premium) {
                        TextButton(onClick = { vm.navigate(Screen.Paywall) }) {
                            Icon(Icons.Filled.WorkspacePremium, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Go PRO")
                        }
                    }
                    IconButton(onClick = { vm.navigate(Screen.Settings) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = { AdBanner(premium, Modifier.navigationBarsPadding()) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (vm.canAddPerson()) showAdd = true else vm.navigate(Screen.Paywall) },
                icon = { Icon(Icons.Filled.PersonAdd, null) },
                text = { Text("Add person") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SummaryRow(data.totalToGet, data.totalToGive, vm.currency) }
            if (data.persons.size > 4) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search people") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (data.persons.isEmpty()) item { EmptyHome() }
            items(people, key = { it.id }) { p ->
                PersonRow(p, data.balanceOf(p.id), vm.currency) { vm.navigate(Screen.PersonDetail(p.id)) }
            }
            if (!premium && data.persons.isNotEmpty()) {
                item {
                    Text(
                        "Free plan: ${minOf(data.persons.size, FREE_PERSON_LIMIT)} of $FREE_PERSON_LIMIT people. " +
                            "Go PRO for unlimited people.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
    }

    if (showAdd) {
        PersonDialog(
            title = "Add person",
            onDismiss = { showAdd = false },
            onSave = { name, phone ->
                showAdd = false
                vm.addPerson(name, phone) { id -> vm.navigate(Screen.PersonDetail(id)) }
            },
        )
    }
}
