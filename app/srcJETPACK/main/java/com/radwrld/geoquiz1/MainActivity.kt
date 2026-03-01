// @path: app/srcJETPACK/main/java/com/radwrld/geoquiz1/MainActivity.kt
package com.radwrld.geoquiz1

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoQuiz()
        }
    }
}

@Composable
fun GeoQuiz() {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Guadalajara es la capital de Jalisco")

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                Toast.makeText(context, "Correcto", Toast.LENGTH_SHORT).show()
            }) {
                Text("Correcto")
            }

            Button(onClick = {
                Toast.makeText(context, "Incorrecto", Toast.LENGTH_SHORT).show()
            }) {
                Text("Incorrecto")
            }
        }
    }
}
