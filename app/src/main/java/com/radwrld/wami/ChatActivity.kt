// @path: app/src/main/java/com/radwrld/wami/ChatActivity.kt

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
import com.radwrld.wami.network.WhatsAppApi
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.storage.MessageStorage
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

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var api: WhatsAppApi
    private lateinit var socket: Socket
    private lateinit var messageStorage: MessageStorage
    private lateinit var jid: String
    private lateinit var contactName: String
    private var isGroup: Boolean = false

    private lateinit var serverConfigStorage: ServerConfigStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jid = intent.getStringExtra("EXTRA_JID") ?: ""
        contactName = intent.getStringExtra("EXTRA_NAME") ?: "Unknown"
        messageStorage = MessageStorage(this)
        isGroup = jid.endsWith("@g.us")
        
        serverConfigStorage = ServerConfigStorage(this)

        if (!isValidJid(jid)) {
            showToast("Error: Invalid or missing contact JID.")
            finish()
            return
        }

        setupUI()
        setupApi()
        setupSocket()
        loadAndSyncChatHistory()

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }
    }

    private fun setupUI() {
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
    }

    private fun setupApi() {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        
        val baseUrl = "http://${serverConfigStorage.getCurrentServer()}/"
        // ADDED: Debugging toast for API URL
        showToast("API Base URL: $baseUrl")

        api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WhatsAppApi::class.java)
    }

    private fun sendMessage(text: String) {
        val tempId = UUID.randomUUID().toString()
        val message = Message(
            id = tempId,
            jid = jid,
            name = contactName,
            text = text,
            status = "sending",
            isOutgoing = true,
            timestamp = System.currentTimeMillis()
        )

        messageStorage.addMessage(jid, message)
        addMessageToAdapter(message)
        binding.etMessage.text?.clear()
        sendMessageToServer(message)
    }

    private fun addMessageToAdapter(message: Message) {
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    private fun sendMessageToServer(message: Message) {
        if (!isValidJid(jid)) {
            showToast("Cannot send message: Invalid JID format.")
            updateMessageStatusInUI(message.id, "failed")
            messageStorage.updateMessageStatus(jid, message.id, "failed")
            return
        }

        lifecycleScope.launch {
            try {
                val response = api.sendMessage(
                    jid = jid,
                    text = message.text,
                    tempId = message.id
                )

                val tempIdFromResponse = response.tempId
                if (tempIdFromResponse == null) {
                    val error = "Server response did not include a tempId."
                    Log.e("SendMessage", error)
                    showToast(error)
                    messageStorage.updateMessageStatus(jid, message.id, "failed")
                    updateMessageStatusInUI(message.id, "failed")
                    return@launch
                }

                if (response.success && response.messageId != null) {
                    messageStorage.updateMessage(jid, tempIdFromResponse, response.messageId, "sent")
                    updateMessageStatusInUI(tempIdFromResponse, "sent", response.messageId)
                } else {
                    messageStorage.updateMessageStatus(jid, tempIdFromResponse, "failed")
                    updateMessageStatusInUI(tempIdFromResponse, "failed")
                    showToast("Error sending: ${response.error ?: "Unknown"}")
                }
            } catch (e: Exception) {
                // MODIFIED: Show detailed exception in toast and update UI status
                val errorMsg = "API Error: ${e.javaClass.simpleName}\n${e.message}"
                Log.e("SendMessage", "API call failed for tempId ${message.id}", e)
                showToast(errorMsg)
                
                // FIX: Update message status to 'failed' on exception
                updateMessageStatusInUI(message.id, "failed")
                messageStorage.updateMessageStatus(jid, message.id, "failed")
            }
        }
    }

    private fun loadAndSyncChatHistory() {
        val localMessages = messageStorage.getMessages(jid)
        messages.clear()
        messages.addAll(localMessages)
        adapter.notifyDataSetChanged()
        if (messages.isNotEmpty()) {
            binding.rvMessages.scrollToPosition(messages.size - 1)
        }

        lifecycleScope.launch {
            try {
                val serverHistory = api.getHistory(jid)
                
                val serverMessages = serverHistory.map {
                    Message(
                        id = it.id,
                        jid = it.jid,
                        text = it.text,
                        name = contactName,
                        status = it.status,
                        isOutgoing = (it.isOutgoing == 1),
                        timestamp = it.timestamp,
                        senderName = null
                    )
                }

                val currentLocalMessages = messageStorage.getMessages(jid)
                val unsentLocalMessages = currentLocalMessages.filter { it.status == "sending" }
                val combinedMessages = mutableListOf<Message>()
                combinedMessages.addAll(serverMessages)
                combinedMessages.addAll(unsentLocalMessages)
                val finalList = combinedMessages.distinctBy { it.id }.sortedBy { it.timestamp }
                
                messageStorage.saveMessages(jid, finalList)
                
                messages.clear()
                messages.addAll(finalList)
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            } catch (e: Exception) {
                Log.e("ChatHistory", "Failed to sync history for JID: $jid. Using local cache.", e)
                showToast("History sync failed: ${e.javaClass.simpleName}")
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
            
            val socketUrl = "http://${serverConfigStorage.getCurrentServer()}"
            // ADDED: Debugging toast for Socket URL
            showToast("Socket URL: $socketUrl")

            socket = IO.socket(socketUrl.trimEnd('/'), opts)
            socket.on(Socket.EVENT_CONNECT) { runOnUiThread { Log.d("SocketIO", "Connected") } }
            // MODIFIED: Add verbose toast for connection errors
            socket.on(Socket.EVENT_CONNECT_ERROR) { err ->
                val error = err.firstOrNull()?.toString() ?: "Unknown error"
                runOnUiThread {
                    val errorMsg = "Socket Connect Error: $error"
                    Log.e("SocketIO", errorMsg)
                    showToast(errorMsg)
                }
            }
            socket.on(Socket.EVENT_DISCONNECT) { runOnUiThread { Log.d("SocketIO", "Disconnected") } }
            socket.on("whatsapp-message", onNewMessage)
            socket.on("whatsapp-message-status-update", onMessageStatusUpdate)
            socket.connect()
        } catch (e: URISyntaxException) {
            Log.e("SocketIO", "Setup failed", e)
            showToast("Socket setup failed: ${e.message}")
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
                        val senderName = if (isGroup) msgJson.optString("pushName", "Unknown") else null

                        val newMessage = Message(
                            id = msgJson.optString("id"),
                            jid = messageJid,
                            text = msgJson.optString("text"),
                            name = contactName,
                            status = "received",
                            isOutgoing = false,
                            timestamp = msgJson.optLong("timestamp", System.currentTimeMillis()),
                            senderName = senderName
                        )
                        messageStorage.addMessage(jid, newMessage)
                        addMessageToAdapter(newMessage)
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
                
                messageStorage.updateMessageStatus(jid, messageId, newStatus)
                updateMessageStatusInUI(messageId, newStatus)
            } catch (e: Exception) {
                Log.e("StatusUpdate", "Failed to parse status update", e)
            }
        }
    }

    private fun updateMessageStatusInUI(messageId: String, newStatus: String, newId: String? = null) {
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex != -1) {
            messages[messageIndex].status = newStatus
            if (newId != null) {
                messages[messageIndex].id = newId
            }
            adapter.notifyItemChanged(messageIndex)
        }
    }

    private fun isValidJid(jid: String): Boolean {
        if (jid.isBlank()) return false
        if (!jid.endsWith("@s.whatsapp.net") && !jid.endsWith("@g.us")) return false
        if (jid.startsWith("@")) return false
        return true
    }

    private fun showToast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::socket.isInitialized && socket.connected()) {
            socket.off()
            socket.disconnect()
        }
        _binding = null
    }
}
