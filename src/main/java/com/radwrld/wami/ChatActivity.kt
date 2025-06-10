// ChatActivity.kt


package com.radwrld.wami

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ChatAdapter
import com.radwrld.wami.databinding.ActivityChatBinding
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.SendRequest
import com.radwrld.wami.network.WhatsAppApi
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URISyntaxException
import java.util.UUID

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var api: WhatsAppApi
    private lateinit var socket: Socket
    private lateinit var jid: String
    private lateinit var contactName: String

    private val serverUrl = "http://22.ip.gl.ply.gg:18880/" // IMPORTANT: Use your server's public URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jid = intent.getStringExtra("EXTRA_JID") ?: ""
        if (!isValidJid(jid)) {
            showToast("Error: Invalid or missing contact JID.")
            finish()
            return
        }

        contactName = intent.getStringExtra("EXTRA_NAME") ?: "Unknown"
        binding.tvContactName.text = contactName
        binding.tvLastSeen.visibility = View.GONE
        binding.btnBack.setOnClickListener { finish() }

        adapter = ChatAdapter(messages)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                binding.btnSend.visibility = if (hasText) View.VISIBLE else View.GONE
                binding.btnMic.visibility = if (hasText) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        api = Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WhatsAppApi::class.java)

        setupSocket()
        loadChatHistory()

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                val tempId = UUID.randomUUID().toString()
                addMessageToUI(text, tempId)
                sendMessageToServer(text, tempId)
            }
        }
    }

    private fun addMessageToUI(text: String, tempId: String) {
        val message = Message(
            id = tempId,
            jid = jid,
            name = contactName,
            text = text,
            status = "sending",
            isOutgoing = true,
            timestamp = System.currentTimeMillis()
        )
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
        binding.etMessage.text?.clear()
    }

    private fun sendMessageToServer(text: String, tempId: String) {
        if (!isValidJid(jid)) {
            showToast("Cannot send message: Invalid JID format.")
            return
        }
        lifecycleScope.launch {
            try {
                val response = api.sendMessage(SendRequest(jid, text, tempId))
                val messageIndex = messages.indexOfFirst { it.id == response.tempId }
                if (messageIndex == -1) return@launch

                if (response.success && response.messageId != null) {
                    messages[messageIndex].id = response.messageId
                    messages[messageIndex].status = "sent"
                } else {
                    messages[messageIndex].status = "failed"
                    showToast("Error sending: ${response.error ?: "Unknown"}")
                }
                adapter.notifyItemChanged(messageIndex)

            } catch (e: Exception) {
                Log.e("SendMessage", "API call failed for tempId $tempId", e)
                val messageIndex = messages.indexOfFirst { it.id == tempId }
                if (messageIndex != -1) {
                    messages[messageIndex].status = "failed"
                    adapter.notifyItemChanged(messageIndex)
                }
                showToast("Error sending message: ${e.message}")
            }
        }
    }

    private fun loadChatHistory() {
        lifecycleScope.launch {
            try {
                val history = api.getHistory(jid)
                messages.clear()
                messages.addAll(history.map {
                    Message(
                        id = it.id,
                        jid = it.jid,
                        text = it.text,
                        name = contactName,
                        status = it.status,
                        isOutgoing = (it.isOutgoing == 1),
                        timestamp = it.timestamp
                    )
                })
                adapter.notifyDataSetChanged()
                binding.rvMessages.scrollToPosition(messages.size - 1)
            } catch (e: Exception) {
                Log.e("ChatHistory", "Failed to load history for JID: $jid", e)
            }
        }
    }

    private fun setupSocket() {
        try {
            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
                transports = arrayOf(WebSocket.NAME)
            }
            socket = IO.socket(serverUrl.trimEnd('/'), opts)
            socket.on(Socket.EVENT_CONNECT) { runOnUiThread { Log.d("SocketIO", "Connected") } }
            socket.on(Socket.EVENT_CONNECT_ERROR) { err -> runOnUiThread { Log.e("SocketIO", "Connection Error: ${err.firstOrNull()}") } }
            socket.on(Socket.EVENT_DISCONNECT) { runOnUiThread { Log.d("SocketIO", "Disconnected") } }
            socket.on("whatsapp-message", onNewMessage)
            socket.on("whatsapp-message-status-update", onMessageStatusUpdate)
            socket.connect()
        } catch (e: URISyntaxException) {
            Log.e("SocketIO", "Setup failed", e)
        }
    }

    private val onNewMessage = Emitter.Listener { args ->
        runOnUiThread {
            try {
                val data = args[0] as? JSONArray ?: return@runOnUiThread
                for (i in 0 until data.length()) {
                    val msgJson = data.getJSONObject(i)
                    val messageJid = msgJson.optString("jid")
                    if (messageJid == jid && !msgJson.optBoolean("fromMe")) {
                        messages.add(
                            Message(
                                id = msgJson.optString("id"),
                                jid = messageJid,
                                text = msgJson.optString("text"),
                                name = contactName,
                                status = "received",
                                isOutgoing = false,
                                timestamp = msgJson.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                        adapter.notifyItemInserted(messages.size - 1)
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }
                }
            } catch (e: Exception) {
                Log.e("OnNewMessage", "Error processing new message", e)
            }
        }
    }

    private val onMessageStatusUpdate = Emitter.Listener { args ->
        runOnUiThread {
            try {
                val data = args[0] as? JSONObject ?: return@runOnUiThread
                val messageId = data.optString("id")
                val newStatus = data.optString("status")

                val messageIndex = messages.indexOfFirst { it.id == messageId }
                if (messageIndex != -1) {
                    messages[messageIndex].status = newStatus
                    adapter.notifyItemChanged(messageIndex)
                }
            } catch (e: Exception) {
                Log.e("StatusUpdate", "Failed to parse status update", e)
            }
        }
    }

    private fun isValidJid(jid: String): Boolean {
        return jid.isNotEmpty() && jid.contains("@s.whatsapp.net")
    }

    private fun showToast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::socket.isInitialized && socket.connected()) {
            socket.off()
            socket.disconnect()
        }
    }
}
