// @path: app/src/main/java/com/radwrld/wami/ui/screen/QRScreen.kt
package com.radwrld.wami.ui.screen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.radwrld.wami.ui.vm.SessionViewModel

@Composable
fun QRScreen(
    nav: NavController,
    vm: SessionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val qr by vm.qrCode.collectAsState()
    val auth by vm.isAuth.collectAsState()

    LaunchedEffect(Unit) {
        vm.start()
    }

    val bitmap = remember(qr) {
        qr?.takeIf { it.startsWith("data:image/") }
            ?.substringAfter(",")
            ?.let { base64 ->
                try {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            auth -> Button(onClick = { nav.navigate("chats") }) {
                Text("Enter Chats")
            }

            bitmap != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Scan this QR") 
                Spacer(Modifier.height(16.dp))
                Image(
                    bitmap = bitmap,
                    contentDescription = "QR Code",
                    modifier = Modifier.size(250.dp) 
                )
            }

            else -> CircularProgressIndicator()
        }
    }
}
