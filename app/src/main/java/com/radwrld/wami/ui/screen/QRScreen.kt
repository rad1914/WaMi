// QRScreen.kt
package com.radwrld.wami.ui.screen
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.radwrld.wami.ui.vm.SessionViewModel

@Composable
fun QRScreen(nav: NavController, vm: SessionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val qr  by vm.qrCode.collectAsState()
    val auth by vm.isAuth.collectAsState()

    LaunchedEffect(Unit) { vm.start() }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            auth -> Button(onClick = { nav.navigate("chats") }) {
                Text("Enter Chats")
            }
            qr != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Scan this QR")
                Spacer(Modifier.height(16.dp))
                Image(
                    painter = rememberAsyncImagePainter(qr),
                    contentDescription = null,
                    modifier = Modifier.size(250.dp)
                )
            }
            else -> CircularProgressIndicator()
        }
    }
}
