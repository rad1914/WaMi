// @path: app/src/main/java/com/radwrld/wami/ui/screens/tweaks/chat.kt
package com.radwrld.wami.ui.screens.tweaks

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import com.radwrld.wami.ui.screens.tweaks.components.SectionHeader
import com.radwrld.wami.ui.screens.tweaks.components.SettingsSwitchItem

@Composable
fun ChatTweaks() {
    var bubbleStyleChecked by remember { mutableStateOf(true) }

    Column {
        SectionHeader("Chat Screen")
        SettingsSwitchItem(
            title = "Bubble Style Messages",
            subtitle = "Display messages in modern chat bubbles.",
            checked = bubbleStyleChecked,
            onCheckedChange = { bubbleStyleChecked = it }
        )
    }
}
