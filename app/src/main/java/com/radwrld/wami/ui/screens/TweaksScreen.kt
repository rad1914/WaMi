// @path: app/src/main/java/com/radwrld/wami/ui/screens/TweaksScreen.kt
package com.radwrld.wami.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radwrld.wami.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TweaksScreen(
    onNavigateBack: () -> Unit
) {

    var pillNavChecked by remember { mutableStateOf(true) }
    var bubbleStyleChecked by remember { mutableStateOf(true) }
    var biometricChecked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tweaks") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { SectionHeader("Home Screen") }
            item {
                SettingsSwitchItem(
                    title = "Pill Navigation Bar",
                    subtitle = "Use the floating pill navigation bar at the bottom.",
                    checked = pillNavChecked,
                    onCheckedChange = { pillNavChecked = it }
                )
            }

            item { SectionHeader("Chat Screen") }
            item {
                SettingsSwitchItem(
                    title = "Bubble Style Messages",
                    subtitle = "Display messages in modern chat bubbles.",
                    checked = bubbleStyleChecked,
                    onCheckedChange = { bubbleStyleChecked = it }
                )
            }

            item { SectionHeader("Security") }
            item {
                SettingsSwitchItem(
                    title = "Biometric Unlock",
                    subtitle = "Require fingerprint or face unlock to open the app.",
                    checked = biometricChecked,
                    onCheckedChange = { biometricChecked = it }
                )
            }

            item { SectionHeader("Customization") }
            item {
                SettingsClickableItem(
                    title = "Chat Wallpaper",
                    subtitle = "Default",
                    onClick = {  }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(top = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
