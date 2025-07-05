// @path: app/src/main/java/com/radwrld/wami/ui/screens/tweaks/customization.kt
package com.radwrld.wami.ui.screens.tweaks

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.radwrld.wami.ui.screens.tweaks.components.SectionHeader
import com.radwrld.wami.ui.screens.tweaks.components.SettingsClickableItem

@Composable
fun CustomizationTweaks() {
    Column {
        SectionHeader("Customization")
        SettingsClickableItem(
            title = "Chat Wallpaper",
            subtitle = "Default",
            onClick = {  }
        )
    }
}
