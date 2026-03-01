// @path: app/_p6_disabled/p4/MainActivity.kt
// @path: app/src/main/java/com/radwrld/p4/MainActivity.kt
package com.radwrld.p4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent { Calc() }
    }
}

@Composable
fun Calc() {                         
    var a by remember { mutableStateOf("") }  
    var b by remember { mutableStateOf("") }  
    var r by remember { mutableStateOf("0") } 

    fun n(x: String) = x.toDoubleOrNull() ?: 0.0  

    fun op(f: (Double, Double) -> Double) {      
        r = f(n(a), n(b))                         
            .let { if (it.isInfinite()) "∞" else it.toString() }
    }

    Column(Modifier.padding(12.dp)) {             
        TextField(a, { a = it }, label = { Text("A") })
        TextField(b, { b = it }, label = { Text("B") })

        Row {                                     
            listOf(                               
                "+" to { x: Double, y: Double -> x + y },
                "-" to { x, y -> x - y },                 
                "*" to { x, y -> x * y },                 
                "/" to { x, y -> x / y }                  
            ).forEach { (t, f) ->                 
                Button(
                    onClick = {                  
                        if (t == "/" && n(b) == 0.0)
                            r = "∞"              
                        else
                            op(f)                
                    },
                    Modifier.padding(2.dp)       
                ) { Text(t) }                    
            }
        }

        Text(                                    
            "Resultado $r",
            color = Color(0xFFFFA500)             
        )

        Text(                                    
            "Ramses Aracen Delgado\nNC: 22290949 - 8vo Semestre",
            color = Color.Blue
        )
    }
}
