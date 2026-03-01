// @path: app/_p6_disabled/MainActivity.kt
// @path: app/src/main/java/com/radwrld/p6/MainActivity.kt
package com.radwrld.p6

import com.radwrld.p6.R
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

var mBancoPreguntas = listOf(
    FalsoVerdadero(R.string.pregunta_1, false),
    FalsoVerdadero(R.string.pregunta_2, true),
    FalsoVerdadero(R.string.pregunta_3, true)
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            var mIndex by remember { mutableStateOf(0) }

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Pregunta(mBancoPreguntas[mIndex].mPregunta)

                    Opciones(
                        mIndex = mIndex,
                        modifier = Modifier.padding(16.dp)
                    )

                    BotonNext {
                        mIndex = (mIndex + 1) % mBancoPreguntas.size
                    }
                }
            }
        }
    }
}

@Composable
fun Pregunta(idTexto: Int) {
    Text(
        text = stringResource(idTexto),
        fontSize = 24.sp
    )
}

@Composable
fun Opciones(
    mIndex: Int,
    modifier: Modifier
) {

    val localContext = LocalContext.current

    val msgBtnVerdadero: String
    val msgBtnFalso: String

    if (mBancoPreguntas[mIndex].mPreguntaCierta) {
        msgBtnVerdadero = stringResource(R.string.respuesta_correcta)
        msgBtnFalso = stringResource(R.string.respuesta_incorrecta)
    } else {
        msgBtnVerdadero = stringResource(R.string.respuesta_incorrecta)
        msgBtnFalso = stringResource(R.string.respuesta_correcta)
    }

    Row(modifier = modifier) {

        Button(
            onClick = {
                Toast.makeText(localContext, msgBtnVerdadero, Toast.LENGTH_SHORT).show()
            }
        ) {
            Text(text = "Verdadero")
        }

        Button(
            onClick = {
                Toast.makeText(localContext, msgBtnFalso, Toast.LENGTH_SHORT).show()
            }
        ) {
            Text(text = "Falso")
        }
    }
}

@Composable
fun BotonNext(onNext: () -> Unit) {

    Button(onClick = { onNext() }) {
        Text(text = "Siguiente")
    }
}
