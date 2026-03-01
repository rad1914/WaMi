// @path: app/_p6_disabled/p2/MainActivity.kt
// @path: app/src/main/java/com/radwrld/p2/MainActivity.kt
// @path: app/src/main/java/com/radwrld/pr2/MainActivity.kt
package com.radwrld.pr2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Cover() }
    }
}

@Composable
fun Cover() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                "Practica #2",
                fontSize = 36.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )

            Text(
                "Num. Control: 22290949",
                fontSize = 20.sp,
                color = Color(0xFF03A9F4),
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "Ramses Aracen Delgado | 8vo Semestre",
                fontSize = 24.sp,
                color = Color(0xFFFFC107),
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif
            )
        }
    }
}
