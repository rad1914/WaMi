package com.radwrld.wami.network

import android.app.Activity
import android.graphics.Color
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.adapter.ChatAdapter
import com.radwrld.wami.model.Message
import com.radwrld.wami.storage.ServerConfigStorage
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import java.net.URISyntaxException

class SocketManager(
    private val activity: Activity,
    private val config: ServerConfigStorage,
    private val initialJid: String,
    private val messages: MutableList<Message>,
    private val adapter: ChatAdapter,
    private val rvChat: RecyclerView,
    private val tvStatus: TextView
) {
    private lateinit var socket: Socket

    fun connect() {
        tryConnect(0)
    }

    private fun tryConnect(tries: Int) {
        val server = if (tries == 0) config.primaryServer else config.fallbackServer
        try {
            socket = IO.socket("http://$server")
            socket.on(Socket.EVENT_CONNECT) {
                activity.runOnUiThread {
                    config.resetToPrimary()
                    tvStatus.text = "Connected"
                    tvStatus.setTextColor(Color.GREEN)
                }
            }
            socket.on(Socket.EVENT_DISCONNECT) {
                activity.runOnUiThread {
                    tvStatus.text = "Disconnected"
                    tvStatus.setTextColor(Color.RED)
                }
            }
            socket.on("whatsapp-message", onIncoming)
            socket.connect()
        } catch (e: URISyntaxException) {
            if (tries == 0) tryConnect(1)
        }
    }

    private val onIncoming = Emitter.Listener { args ->
        val arr = args.getOrNull(0) as? JSONArray ?: return@Listener
        for (i in 0 until arr.length()) {
            val up = arr.getJSONObject(i)
            val msgs = up.optJSONArray("messages") ?: continue
            for (j in 0 until msgs.length()) {
                val m = msgs.getJSONObject(j)
                val key = m.getJSONObject("key")
                val from = key.getString("remoteJid")
                val body = m.optJSONObject("message")?.optString("conversation") ?: continue
                if (from.substringBefore("@") == initialJid.substringBefore("@")) {
                    val msg = Message(
                        name = from,
                        lastMessage = body,
                        avatarUrl = "",
                        phoneNumber = from.substringBefore("@"),
                        isOnline = false,
                        isOutgoing = key.optBoolean("fromMe", false)
                    )
                    activity.runOnUiThread {
                        messages.add(msg)
                        adapter.notifyItemInserted(messages.size - 1)
                        rvChat.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }
    }

    fun cleanup() {
        socket.disconnect()
        socket.off()
    }
}
