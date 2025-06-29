// @path: app/src/main/java/com/radwrld/wami/TweaksActivity.kt
package com.radwrld.wami

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.radwrld.wami.ui.screens.TweaksScreen
import com.radwrld.wami.ui.theme.WamiTheme

class TweaksActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            WamiTheme {
                TweaksScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
