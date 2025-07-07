package com.radwrld.wami

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.radwrld.wami.ui.AppNav
import com.radwrld.wami.ui.theme.Material3Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        setContent {
            Material3Theme {
                AppNav()
            }
        }
    }
}
