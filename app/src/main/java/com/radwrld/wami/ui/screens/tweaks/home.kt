// @path: app/src/main/java/com/radwrld/wami/ui/screens/tweaks/home.kt
package com.radwrld.wami.ui.screens.tweaks

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import com.radwrld.wami.ui.screens.tweaks.components.SectionHeader
import com.radwrld.wami.ui.screens.tweaks.components.SettingsSwitchItem

@Composable
fun HomeTweaks() {
    var pillNavChecked by remember { mutableStateOf(true) }

    Column {
        SectionHeader("Home Screen")
        SettingsSwitchItem(
            title = "Pill Navigation Bar",
            subtitle = "Use the floating pill navigation bar at the bottom.",
            checked = pillNavChecked,
            onCheckedChange = { pillNavChecked = it }
        )
    }
}
