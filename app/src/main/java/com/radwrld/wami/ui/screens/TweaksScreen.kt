// @path: app/src/main/java/com/radwrld/wami/ui/screens/TweaksScreen.kt
package com.radwrld.wami.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.radwrld.wami.ui.screens.tweaks.ChatTweaks
import com.radwrld.wami.ui.screens.tweaks.CustomizationTweaks
import com.radwrld.wami.ui.screens.tweaks.HomeTweaks
import com.radwrld.wami.ui.screens.tweaks.SecurityTweaks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TweaksScreen(
    onNavigateBack: () -> Unit
) {
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
            item { HomeTweaks() }
            item { ChatTweaks() }
            item { SecurityTweaks() }
            item { CustomizationTweaks() }
        }
    }
}
