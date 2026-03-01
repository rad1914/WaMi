// @path: app/_p6_disabled/p5/MainActivity.kt
// @path: app/src/main/java/com/radwrld/p5/MainActivity.kt
package com.radwrld.p5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorApp()
        }
    }
}

@Composable
fun CalculatorApp() {
    var display by remember { mutableStateOf("0") }
    var firstNumber by remember { mutableStateOf<Double?>(null) }
    var operator by remember { mutableStateOf<String?>(null) }

    fun onNumberClick(num: String) {
        display = if (display == "0") num else display + num
    }

    fun onOperatorClick(op: String) {
        firstNumber = display.toDoubleOrNull()
        operator = op
        display = "0"
    }

    fun onEqualsClick() {
        val second = display.toDoubleOrNull()
        val first = firstNumber

        if (first != null && second != null && operator != null) {
            val result = when (operator) {
                "+" -> first + second
                "-" -> first - second
                "*" -> first * second
                "/" -> if (second != 0.0) first / second else 0.0
                else -> 0.0
            }
            display = result.toString().removeSuffix(".0")
        }

        firstNumber = null
        operator = null
    }

    fun onClear() {
        display = "0"
        firstNumber = null
        operator = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = display,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )

        val buttons = listOf(
            listOf("7", "8", "9", "/"),
            listOf("4", "5", "6", "*"),
            listOf("1", "2", "3", "-"),
            listOf("0", "C", "=", "+")
        )

        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { label ->
                    CalcButton(label) {
                        when (label) {
                            "C" -> onClear()
                            "=" -> onEqualsClick()
                            "+", "-", "*", "/" -> onOperatorClick(label)
                            else -> onNumberClick(label)
                        }
                    }
                }
            }
        }

        Text(
            text = "Ramses Aracen Delgado\nNC: 22290949 - 8vo Semestre",
            color = Color(0xFF2196F3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}

@Composable
fun RowScope.CalcButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(70.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
