// @path: app/src/main/java/com/radwrld/wami/ui/screens/tweaks/security.kt
package com.radwrld.wami.ui.screens.tweaks

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import com.radwrld.wami.ui.screens.tweaks.components.SectionHeader
import com.radwrld.wami.ui.screens.tweaks.components.SettingsSwitchItem

@Composable
fun SecurityTweaks() {
    var alwaysOnChecked by remember { mutableStateOf(true) }
    var biometricChecked by remember { mutableStateOf(false) }

    Column {
        SectionHeader("Security & Privacy")
        SettingsSwitchItem(
            title = "Always On",
            subtitle = "Keeps your contact always shown as online.",
            checked = alwaysOnChecked,
            onCheckedChange = { alwaysOnChecked = it }
        )
        SettingsSwitchItem(
            title = "Biometric Unlock",
            subtitle = "Require fingerprint or face unlock to open the app.",
            checked = biometricChecked,
            onCheckedChange = { biometricChecked = it }
        )
    }
}
