// ChatActivity.kt

package com.radwrld.wami

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ChatAdapter
import com.radwrld.wami.databinding.ActivityChatBinding
import com.radwrld.wami.model.Message
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.client.IO.Options
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var api: WhatsAppApi
    private lateinit var socket: Socket
    private lateinit var jid: String
    private lateinit var contactName: String

    companion object {
        private const val BASE_URL = "http://22.ip.gl.ply.gg:18880/"
    }

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
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
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

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WhatsAppApi::class.java)

        setupSocket()
        loadChatHistory()

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessageToUI(text)
                sendMessageToServer(text)
            }
        }
    }

    private fun addMessageToUI(text: String) {
        val message = Message(contactName, text, "", "", jid, true, System.currentTimeMillis())
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
        binding.etMessage.text?.clear()
    }

    private fun sendMessageToServer(text: String) {
        if (!isValidJid(jid)) {
            showToast("Cannot send message: Invalid JID format.")
            return
        }

        showToast("Requesting to send message: JID=$jid, Text=$text")

        lifecycleScope.launch {
            try {
                showToast("Sending message to JID: $jid - Text: $text")
                val result = api.sendMessage(SendRequest(jid, text))
                val trimmed = result.trim().trim('"')
                if (trimmed.equals("OK", ignoreCase = true)) {
                    showToast("Message sent")
                } else {
                    showToast("Send failed: $trimmed")
                }
            } catch (e: Exception) {
                showToast("Error sending message: ${e.message}")
            }
        }
    }

    private fun loadChatHistory() {
        lifecycleScope.launch {
            try {
                val history = api.getHistory(jid)
                messages.addAll(history.map {
                    Message(
                        contactName,
                        it.text,
                        it.status,
                        it.id.toString(),
                        it.jid,
                        it.isOutgoing == 1,
                        it.timestamp
                    )
                })
                adapter.notifyDataSetChanged()
                binding.rvMessages.scrollToPosition(messages.size - 1)
            } catch (e: Exception) {
                showToast("Failed to load history: ${e.message}")
            }
        }
    }

    private fun setupSocket() {
        try {
            val opts = Options().apply {
                forceNew = true
                reconnection = true
                transports = arrayOf(WebSocket.NAME)
            }
            socket = IO.socket(BASE_URL.trimEnd('/'), opts)
            socket.apply {
                on(Socket.EVENT_CONNECT) { showToast("Socket connected") }
                on(Socket.EVENT_CONNECT_ERROR) { showToast("Socket connect error") }
                on(Socket.EVENT_DISCONNECT) { showToast("Socket disconnected") }
                on("whatsapp-message", onNewMessage)
                connect()
            }
        } catch (e: Exception) {
            showToast("Socket connection failed: ${e.message}")
        }
    }

    private val onNewMessage = io.socket.emitter.Emitter.Listener { args ->
        // Prevent crash if the activity is already destroyed
        if (isFinishing || isDestroyed) {
            return@Listener
        }

        runOnUiThread {
            val arr = args[0] as? JSONArray ?: return@runOnUiThread
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue

                // --- APPLIED FIX: Use opt...() methods to prevent JSONException ---
                val keyObject = obj.optJSONObject("key")
                val remoteJid = keyObject?.optString("remoteJid")
                
                if (remoteJid == jid) {
                    val fromMe = keyObject?.optBoolean("fromMe", false) ?: false
                    val text = obj.optJSONObject("message")?.optString("conversation") ?: ""
                    
                    // Only add the message if it has content and is not from us
                    // (since we already add our own messages to the UI instantly)
                    if (text.isNotEmpty() && !fromMe) {
                         val newMessage = Message(contactName, text, "", "", remoteJid, fromMe, System.currentTimeMillis())
                         messages.add(newMessage)
                    }
                }
            }
            // Use notifyDataSetChanged to be safe with multiple additions
            adapter.notifyDataSetChanged()
            binding.rvMessages.scrollToPosition(messages.size - 1)
        }
    }

    private fun isValidJid(jid: String): Boolean {
        return jid.isNotEmpty() && jid.contains("@s.whatsapp.net") && jid.length > "@s.whatsapp.net".length
    }

    private fun showToast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::socket.isInitialized) {
            socket.off()
            socket.disconnect()
        }
    }
}
