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

    vm: SessionViewModel
) {
    val qr by vm.qrCode.collectAsState()
    val auth by vm.isAuth.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var sessionId by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Enter Session ID") },
            text = {
                OutlinedTextField(
                    value = sessionId,
                    onValueChange = { sessionId = it },
                    label = { Text("Session ID") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (sessionId.isNotBlank()) {
                        vm.loginWithId(sessionId)
                        showDialog = false
                    }
                }) { Text("Login") }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    val bitmap = remember(qr) {
        qr?.substringAfter(",")?.let {
            runCatching {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            auth -> Button(onClick = { nav.navigate("chats") }) { Text("Enter Chats") }

            bitmap != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Scan this QR")
                Spacer(Modifier.height(16.dp))
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(250.dp))
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showDialog = true }) { Text("Login with Session ID") }
            }

            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showDialog = true }) { Text("Login with Session ID") }
            }
        }
    }
}
