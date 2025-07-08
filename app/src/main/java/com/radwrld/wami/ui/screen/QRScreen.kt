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
    var showIdInputDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.start()
    }

    if (showIdInputDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showIdInputDialog = false },
            title = { Text("Enter Session ID") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Session ID") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (text.isNotBlank()) {
                        vm.loginWithId(text)
                        showIdInputDialog = false
                    }
                }) {
                    Text("Login")
                }
            },
            dismissButton = {
                Button(onClick = { showIdInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Scan this QR")
                Image(
                    bitmap = bitmap,
                    contentDescription = "QR Code",
                    modifier = Modifier.size(250.dp)
                )
                Button(onClick = { showIdInputDialog = true }) {
                    Text("Login with Session ID")
                }
            }

            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Button(onClick = { showIdInputDialog = true }) {
                    Text("Login with Session ID")
                }
            }
        }
    }
}