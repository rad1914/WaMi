// @path: app/_p6_disabled/p3/MainActivity.kt
// @path: app/src/main/java/com/radwrld/p3/MainActivity.kt
package com.radwrld.pr3

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Boton() }
    }
}

@Composable
fun Boton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var texto by remember { mutableStateOf("") }
    var saluda by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                saluda = "Hola"
                Toast.makeText(context, "Hola $texto", Toast.LENGTH_SHORT).show()
            }
        ) {
            Text("filled")
        }

        FilledTonalButton(
            onClick = { }
        ) {
            Text("tonal")
        }

        OutlinedButton(
            onClick = { }
        ) {
            Text("outline")
        }

        Row {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = "Carita"
            )
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email"
            )
        }

        Row {
            TextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Nombre") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words
                )
            )
            Text(
                text = saluda,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
