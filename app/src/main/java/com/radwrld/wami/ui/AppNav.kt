// NavGraph.kt
package com.radwrld.wami.ui
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.radwrld.wami.ui.screen.*

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "qr") {
        composable("qr")        { QRScreen(nav) }
        composable("chats")     { ChatScreen(nav) }
        composable("chat/{jid}") {
            val jid = it.arguments!!.getString("jid")!!
            MessageScreen(nav, jid)
        }
    }
}
