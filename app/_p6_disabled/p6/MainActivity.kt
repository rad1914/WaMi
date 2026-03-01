// @path: app/_p6_disabled/p6/MainActivity.kt
// @path: app/src/main/java/com/radwrld/p6/MainActivity.kt
package com.radwrld.p6

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val edtHeight = findViewById<EditText>(R.id.edtHeight)
        val edtWeight = findViewById<EditText>(R.id.edtWeight)
        val txtBMI = findViewById<TextView>(R.id.txtBMI)
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)

        btnCalculate.setOnClickListener {
            val height = edtHeight.text.toString().toFloatOrNull()
            val weight = edtWeight.text.toString().toFloatOrNull()

            if (height != null && weight != null && height > 0f) {
                val bmi = weight / (height * height)
                txtBMI.text = "BMI: %.2f".format(bmi)
            } else {
                txtBMI.text = "Valores inválidos"
            }
        }
    }
}
