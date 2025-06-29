// @path: app/src/main/java/com/radwrld/wami/MediaViewActivity.kt

package com.radwrld.wami

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.radwrld.wami.ui.screens.MediaViewScreen
import com.radwrld.wami.ui.theme.WamiTheme

class MediaViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        val type = intent.type

        if (uri == null || type == null) {
            Toast.makeText(this, "Media not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            WamiTheme {

                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()

                MediaViewScreen(
                    uri = uri,
                    mimeType = type,
                    onClose = { finish() }
                )
            }
        }
    }
}
