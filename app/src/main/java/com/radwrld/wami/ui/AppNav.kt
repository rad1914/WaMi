// @path: app/src/main/java/com/radwrld/wami/ui/AppNav.kt
package com.radwrld.wami.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.radwrld.wami.ui.screen.ChatScreen
import com.radwrld.wami.ui.screen.MessageScreen
import com.radwrld.wami.ui.screen.QRScreen
import com.radwrld.wami.ui.vm.SessionViewModel

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val sessionVM: SessionViewModel = viewModel()

    LaunchedEffect(Unit) { sessionVM.start() }

    NavHost(nav, startDestination = "qr") {
        composable("qr") { QRScreen(nav, sessionVM) }

        composable("chats") { ChatScreen(nav) }
        composable("chat/{jid}",
            arguments = listOf(navArgument("jid") { type = NavType.StringType })
        ) { back ->
            MessageScreen(back.arguments!!.getString("jid")!!, sessionVM)
        }
    }
}
