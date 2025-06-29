// @path: app/src/main/java/com/radwrld/wami/ui/screens/MediaViewScreen.kt
package com.radwrld.wami.ui.screens

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.github.chrisbanes.photoview.PhotoView

@Composable
fun MediaViewScreen(
    uri: Uri,
    mimeType: String,
    onClose: () -> Unit
) {
    HideSystemUi()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        var isLoading by remember { mutableStateOf(true) }

        when {

            mimeType.startsWith("image/") -> {
                var drawable by remember { mutableStateOf<Drawable?>(null) }
                var error by remember { mutableStateOf(false) }
                val context = LocalContext.current

                LaunchedEffect(uri) {
                    val request = ImageRequest.Builder(context)
                        .data(uri)
                        .target(
                            onSuccess = { result ->
                                drawable = result
                                isLoading = false
                            },
                            onError = {
                                error = true
                                isLoading = false
                            }
                        )
                        .build()
                    ImageLoader(context).enqueue(request)
                }

                if (drawable != null) {

                    AndroidView(
                        factory = { ctx -> PhotoView(ctx) },
                        modifier = Modifier.fillMaxSize()
                    ) { photoView ->
                        photoView.setImageDrawable(drawable)
                    }
                } else if (error) {

                    Text("No se pudo cargar la imagen", color = Color.White)
                }
            }

            mimeType.startsWith("video/") -> {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setMediaController(MediaController(context).apply { setAnchorView(this@apply) })
                            setVideoURI(uri)
                            setOnPreparedListener {
                                isLoading = false
                                it.start()
                            }
                            setOnErrorListener { _, _, _ ->
                                isLoading = false
                                true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator()
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun HideSystemUi() {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as android.app.Activity).window
        DisposableEffect(Unit) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            onDispose {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
