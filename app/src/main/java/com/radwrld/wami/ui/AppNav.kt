// @path: app/src/main/java/com/radwrld/wami/ui/AppNav.kt
package com.radwrld.wami.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.radwrld.wami.ui.screen.ChatScreen
import com.radwrld.wami.ui.screen.MessageScreen
import com.radwrld.wami.ui.screen.QRScreen
import com.radwrld.wami.ui.vm.SessionViewModel

@Composable
fun AppNav() {
    val nav = rememberNavController()

    val sessionViewModel: SessionViewModel = viewModel()

    LaunchedEffect(Unit) {
        sessionViewModel.start()
    }

    NavHost(nav, startDestination = "qr") {
        composable("qr") { QRScreen(nav, sessionViewModel) }
        composable("chats") { ChatScreen(nav, sessionViewModel) }
        composable(
            "chat/{jid}",
            arguments = listOf(navArgument("jid") { type = NavType.StringType })
        ) { backStackEntry ->
            val jid = backStackEntry.arguments!!.getString("jid")!!
            MessageScreen(nav, jid, sessionViewModel)
        }
    }
}
