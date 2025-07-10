// @path: app/src/main/java/com/radwrld/wami/MainActivity.kt
package com.radwrld.wami

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.radwrld.wami.ui.AppNav
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            androidx.compose.material3.MaterialTheme {
                AppNav()
            }
        }
    }
}
