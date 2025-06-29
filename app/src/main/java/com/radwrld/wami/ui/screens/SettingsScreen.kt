// @path: app/src/main/java/com/radwrld/wami/ui/screens/SettingsScreen.kt
package com.radwrld.wami.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(

    sessionId: String?,
    theme: String,
    isCustomIpEnabled: Boolean,
    onCustomIpEnabledChange: (Boolean) -> Unit,

    onNavigateBack: () -> Unit,
    onSessionClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onThemeClick: () -> Unit,
    onSetCustomIpClick: () -> Unit,
    onResetHiddenConversationsClick: () -> Unit,
    onKillAppClick: () -> Unit,
    onResetPrefsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            item { SettingsCategoryHeader("Account") }
            if (!sessionId.isNullOrBlank()) {
                item {
                    SettingsClickableRow(
                        title = "Session ID",
                        subtitle = "••••••••••••••••",
                        onClick = onSessionClick
                    )
                }
            }
            item {
                SettingsClickableRow(
                    title = "Logout and delete session",
                    onClick = onLogoutClick
                )
            }
            item { SettingDivider() }

            item { SettingsCategoryHeader("Appearance") }
            item {
                SettingsClickableRow(
                    title = "App Theme",
                    subtitle = theme,
                    onClick = onThemeClick
                )
            }
            item { SettingDivider() }

            item { SettingsCategoryHeader("Network") }
            item {
                SettingsSwitchRow(
                    title = "Enable custom IP",
                    summary = "Once enabled, you can set a custom API backend...",
                    isChecked = isCustomIpEnabled,
                    onCheckedChange = onCustomIpEnabledChange
                )
            }
            if (isCustomIpEnabled) {
                item {
                    SettingsClickableRow(
                        title = "Set custom Backend",
                        onClick = onSetCustomIpClick
                    )
                }
            }
            item { SettingDivider() }

            item { SettingsCategoryHeader("Troubleshooting") }
            item {
                SettingsClickableRow(
                    title = "Reset hidden conversations",
                    onClick = onResetHiddenConversationsClick
                )
            }
            item {
                SettingsDestructiveRow(
                    title = "Kill app",
                    onClick = onKillAppClick
                )
            }
            item {
                SettingsDestructiveRow(
                    title = "Reset app preferences",
                    onClick = onResetPrefsClick
                )
            }
        }
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsClickableRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(title: String, summary: String?, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isChecked,
                onClick = { onCheckedChange(!isChecked) },
                role = Role.Switch
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsDestructiveRow(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
fun SettingDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun ThemeDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit
) {
    val themeOptions = mapOf("Light" to "light", "Dark" to "dark", "System Default" to "system")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column(Modifier.selectableGroup()) {
                themeOptions.forEach { (text, value) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (value == currentTheme),
                                onClick = { onThemeSelected(value) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (value == currentTheme),
                            onClick = null
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CustomIpDialog(
    currentIp: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentIp) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Custom IP") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("e.g., 127.0.0.1:3007") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
