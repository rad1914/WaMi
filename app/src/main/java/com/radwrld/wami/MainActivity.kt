// @path: app/src/main/java/com/radwrld/wami/MainActivity.kt
package com.radwrld.wami
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
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

class MainActivity:ComponentActivity(){
    override fun onCreate(b:Bundle?)=super.onCreate(b).also{
        ChatStore.init(applicationContext)
        setContent{App()}
    }
}

@Composable fun App(){
    val c=remember{HttpClient(OkHttp){install(ContentNegotiation){json(Json{ignoreUnknownKeys=true})}}}
    DisposableEffect(Unit){onDispose{c.close()}}
    val base="http://192.168.100.53:3000"
    var to by remember{mutableStateOf("")}
    var text by remember{mutableStateOf("")}
    var msgs by remember{mutableStateOf(emptyList<Message>())}
    val scope=rememberCoroutineScope()
    LaunchedEffect(Unit){
        msgs=ChatStore.get()
        while(true){
            runCatching{c.get("$base/messages").body<List<Message>>()}
                .onSuccess{
                    if(it.isNotEmpty()){
                        val m=(msgs+it).distinctBy{v->v.ts to v.from}.sortedBy(Message::ts)
                        msgs=m;ChatStore.update(m)
                    }
                }
            delay(2000)
        }
    }
    MaterialTheme{
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(to,{to=it},label={Text("To")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(text,{text=it},label={Text("Message")},modifier=Modifier.fillMaxWidth())
            Button({
                val a=to.trim();val b=text.trim()
                if(a.isBlank()||b.isBlank())return@Button
                scope.launch{
                    runCatching{
                        c.post("$base/send"){contentType(ContentType.Application.Json);setBody(SendReq(a,b))}.body<String>()
                    }
                }
                text=""
            }){Text("Send")}
            SelectionContainer {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()){
                    items(msgs,key={ "${it.ts}-${it.from}" }){
                        Text("${it.from}: ${it.text}",Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}

@Serializable data class Message(val from:String,val text:String,val ts:Long)
@Serializable data class SendReq(val to:String,val text:String)
