@file:OptIn(ExperimentalMaterial3Api::class)

package com.elyedali.debttracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elyedali.debttracker.BuildConfig
import com.elyedali.debttracker.ads.AdsManager
import com.elyedali.debttracker.util.findActivity
import com.elyedali.debttracker.billing.BillingManager
import com.elyedali.debttracker.data.LedgerData
import com.elyedali.debttracker.util.Share

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val premium by vm.isPremium.collectAsStateWithLifecycle()
    val price by vm.price.collectAsStateWithLifecycle()
    var showCurrency by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<LedgerData?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.backupTo(context, uri) }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.readBackup(context, uri) { pendingRestore = it } }

    fun pro(action: () -> Unit) {
        if (premium) action() else vm.navigate(Screen.Paywall)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlanCard(premium, price) { vm.navigate(Screen.Paywall) }

            Section("General") {
                SettingRow(Icons.Filled.CurrencyExchange, "Currency", vm.currency) { showCurrency = true }
                SettingRow(
                    icon = Icons.Filled.Fingerprint,
                    title = "App lock",
                    subtitle = "Fingerprint or screen lock",
                    pro = !premium,
                    trailing = {
                        Switch(
                            checked = vm.lockEnabled && premium,
                            onCheckedChange = { on -> pro { vm.changeLock(context, on) } },
                        )
                    },
                ) { pro { vm.changeLock(context, !vm.lockEnabled) } }
            }

            Section("Data") {
                SettingRow(Icons.Filled.PictureAsPdf, "Export PDF report", "All people and balances", pro = !premium) {
                    pro { vm.sharePdf(context, null) }
                }
                SettingRow(Icons.Filled.Backup, "Back up to a file", "Save a copy to Google Drive or your phone", pro = !premium) {
                    pro { backupLauncher.launch("debt-tracker-backup.json") }
                }
                SettingRow(Icons.Filled.Restore, "Restore from a file", "Replace current data with a backup", pro = !premium) {
                    pro { restoreLauncher.launch(arrayOf("*/*")) }
                }
            }

            Section("About") {
                if (premium) {
                    SettingRow(Icons.Filled.CardMembership, "Manage subscription", "Change or cancel in Google Play") {
                        Share.openUrl(
                            context,
                            "https://play.google.com/store/account/subscriptions?sku=" +
                                "${BillingManager.PRODUCT_ID}&package=${context.packageName}",
                        )
                    }
                }
                if (!premium && AdsManager.privacyOptionsRequired(context)) {
                    SettingRow(Icons.Filled.PrivacyTip, "Privacy options", "Manage your ad choices") {
                        context.findActivity()?.let { AdsManager.showPrivacyOptions(it) }
                    }
                }
                SettingRow(Icons.Filled.Star, "Rate Debt Tracker", "It helps a lot!") {
                    Share.openUrl(context, Share.STORE_URL)
                }
                SettingRow(Icons.Filled.Share, "Share with friends", null) {
                    Share.text(
                        context,
                        "I use Debt Tracker to keep track of money I lend and borrow. Try it: ${Share.STORE_URL}",
                        "Share Debt Tracker",
                    )
                }
                if (BuildConfig.DEBUG) {
                    SettingRow(
                        icon = Icons.Filled.BugReport,
                        title = "Test PRO features",
                        subtitle = "Only in test builds, not in the store version",
                        trailing = { Switch(checked = premium, onCheckedChange = { vm.setDebugPremium(it) }) },
                    ) { vm.setDebugPremium(!premium) }
                }
            }

            Text(
                "Your data stays on this phone. No account, no servers.\nVersion ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }

    if (showCurrency) {
        CurrencyDialog(
            current = vm.currency,
            onPick = { vm.changeCurrency(it); showCurrency = false },
            onDismiss = { showCurrency = false },
        )
    }

    pendingRestore?.let { d ->
        ConfirmDialog(
            title = "Restore backup?",
            text = "Your current data will be replaced by this backup (${d.persons.size} people, " +
                "${d.entries.size} entries).",
            confirmText = "Restore",
            onConfirm = {
                pendingRestore = null
                vm.restore(context, d)
            },
            onDismiss = { pendingRestore = null },
        )
    }
}

@Composable
private fun PlanCard(premium: Boolean, price: String?, onUpgrade: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.WorkspacePremium,
                null,
                Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (premium) "PRO is active" else "Debt Tracker PRO",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    if (premium) "Thank you for your support!"
                    else if (price != null) "No ads, unlimited people, PDF, lock and backup for $price/month"
                    else "No ads, unlimited people, PDF, lock and backup",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (!premium) {
                Spacer(Modifier.width(8.dp))
                Button(onClick = onUpgrade) { Text("Upgrade") }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    pro: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val supporting: (@Composable () -> Unit)? = subtitle?.let { s -> @Composable { Text(s) } }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title)
                if (pro) {
                    Spacer(Modifier.width(8.dp))
                    ProBadge()
                }
            }
        },
        supportingContent = supporting,
        trailingContent = trailing,
    )
}
