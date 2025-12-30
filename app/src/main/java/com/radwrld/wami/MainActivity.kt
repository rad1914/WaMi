package com.radwrld.wami

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) = super.onCreate(b).also { setContent { App() } }
}

@Composable
fun App() {
    val client = remember {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }
    DisposableEffect(Unit) { onDispose { client.close() } }

    val base = "http://192.168.100.53:3000"
    var to by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var msgs by remember { mutableStateOf<List<Message>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            runCatching { client.get("$base/messages").body<List<Message>>() }
                .onSuccess { msgs = it.sortedBy(Message::ts) }
            delay(2000)
        }
    }

    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(to, { to = it }, label = { Text("To") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(text, { text = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val cleanTo = to.trim()
                    val cleanText = text.trim()
                    if (cleanTo.isBlank() || cleanText.isBlank()) return@Button
                    scope.launch {
                        runCatching {
                            client.post("$base/send") {
                                contentType(ContentType.Application.Json)
                                setBody(SendReq(cleanTo, cleanText))
                            }.body<String>()
                        }.onFailure {
                            println("Send failed: ${it.message}")
                        }.onSuccess {
                            println("Send ok: $it")
                        }
                    }
                    text = ""
                }
            ) { Text("Send") }

            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(msgs, key = Message::ts) {
                    Text("${it.from}: ${it.text}", Modifier.padding(4.dp))
                }
            }
        }
    }
}

@Serializable data class Message(val from: String, val text: String, val ts: Long)
@Serializable data class SendReq(val to: String, val text: String)