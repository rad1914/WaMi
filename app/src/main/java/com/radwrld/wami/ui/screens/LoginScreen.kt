// @path: app/src/main/java/com/radwrld/wami/ui/screens/LoginScreen.kt
package com.radwrld.wami.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.radwrld.wami.R
import com.radwrld.wami.ui.viewmodel.LoginUiState
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    toastEvents: SharedFlow<String>,
    onStart: () -> Unit,
    onSetSession: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var showSessionInputDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        toastEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        onStart()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(painterResource(id = R.drawable.ic_settings), contentDescription = "Settings")
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (uiState) {
                    is LoginUiState.Loading -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(uiState.msg, style = MaterialTheme.typography.titleMedium)
                    }
                    is LoginUiState.ShowQr -> {
                        QrCodeContent(bitmap = uiState.bitmap, message = uiState.msg)
                    }
                    is LoginUiState.Error -> {
                        Text(
                            text = uiState.msg,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                    is LoginUiState.Idle -> {
                        Text("WaMi", style = MaterialTheme.typography.headlineLarge)
                    }
                    is LoginUiState.LoggedIn -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("¡Vamos!", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            if (uiState !is LoginUiState.LoggedIn) {
                Button(
                    onClick = { showSessionInputDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Icon(painterResource(id = R.drawable.ic_group_add), contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Login as Multiuser")
                }
            }
        }
    }

    if (showSessionInputDialog) {
        SessionInputDialog(
            onDismiss = { showSessionInputDialog = false },
            onConfirm = { sessionId ->
                onSetSession(sessionId)
                showSessionInputDialog = false
            }
        )
    }
}

@Composable
fun QrCodeContent(bitmap: Bitmap, message: String) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR code for login",
        modifier = Modifier.size(240.dp)
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(message, style = MaterialTheme.typography.titleMedium)
}

@OptIn(ExperimentalMaterial3Api::class) // <-- ERROR CORREGIDO AQUÍ
@Composable
fun SessionInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inicio multi-usuario") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Ingresa ID de sesión") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Conectar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
