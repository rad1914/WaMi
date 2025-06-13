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
import com.radwrld.wami.adapter.ChatListItem
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
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class ChatActivity : AppCompatActivity() {

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>() // Source of truth for messages
    private val chatItemsForAdapter = mutableListOf<ChatListItem>() // List for the adapter
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

        adapter = ChatAdapter(chatItemsForAdapter)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
        binding.rvMessages.itemAnimator = null // Avoid flickers on list updates

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
        messages.add(message)
        processAndSubmitMessages(messages)
        binding.etMessage.text?.clear()
        sendMessageToServer(message)
    }

    private fun processAndSubmitMessages(messageList: List<Message>) {
        val newChatItems = createListWithDividers(messageList)
        val shouldScroll = if (binding.rvMessages.layoutManager is LinearLayoutManager) {
            val layoutManager = binding.rvMessages.layoutManager as LinearLayoutManager
            layoutManager.findLastVisibleItemPosition() == adapter.itemCount - 1
        } else {
            true
        }
        
        chatItemsForAdapter.clear()
        chatItemsForAdapter.addAll(newChatItems)
        adapter.notifyDataSetChanged()

        if (shouldScroll) {
            binding.rvMessages.scrollToPosition(chatItemsForAdapter.size - 1)
        }
    }

    private fun createListWithDividers(messages: List<Message>): List<ChatListItem> {
        val items = mutableListOf<ChatListItem>()
        // Add the static warning item at the very top
        items.add(ChatListItem.WarningItem)

        if (messages.isEmpty()) return items // Return list with only the warning

        var lastTimestamp: Long = 0

        messages.forEach { message ->
            if (shouldShowDivider(lastTimestamp, message.timestamp)) {
                val isNewDay = isDifferentDay(lastTimestamp, message.timestamp)
                items.add(ChatListItem.DividerItem(message.timestamp, isNewDay))
            }
            items.add(ChatListItem.MessageItem(message))
            lastTimestamp = message.timestamp
        }
        return items
    }

    private fun shouldShowDivider(prevTs: Long, currentTs: Long): Boolean {
        if (prevTs == 0L) return true // Always show divider for the first message
        if (isDifferentDay(prevTs, currentTs)) return true
        val diffMillis = currentTs - prevTs
        return diffMillis > TimeUnit.MINUTES.toMillis(30) // 30 minutes
    }

    private fun isDifferentDay(ts1: Long, ts2: Long): Boolean {
        if (ts1 == 0L) return true
        val cal1 = Calendar.getInstance().apply { timeInMillis = ts1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = ts2 }
        return cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR) ||
               cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR)
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
                Log.e("SendMessage", "API call failed for tempId ${message.id}", e)
                showToast("Failed to send message.")
                updateMessageStatusInUI(message.id, "failed")
                messageStorage.updateMessageStatus(jid, message.id, "failed")
            }
        }
    }

    private fun loadAndSyncChatHistory() {
        val localMessages = messageStorage.getMessages(jid)
        messages.clear()
        messages.addAll(localMessages)
        processAndSubmitMessages(messages)

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
                processAndSubmitMessages(finalList)
            } catch (e: Exception) {
                Log.e("ChatHistory", "Failed to sync history for JID: $jid. Using local cache.", e)
                showToast("Failed to sync chat history.")
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

            socket = IO.socket(socketUrl.trimEnd('/'), opts)
            socket.on(Socket.EVENT_CONNECT) { runOnUiThread { Log.d("SocketIO", "Connected") } }
            socket.on(Socket.EVENT_CONNECT_ERROR) { err ->
                val error = err.firstOrNull()?.toString() ?: "Unknown error"
                runOnUiThread {
                    Log.e("SocketIO", "Socket Connect Error: $error")
                }
            }
            socket.on(Socket.EVENT_DISCONNECT) { runOnUiThread { Log.d("SocketIO", "Disconnected") } }
            socket.on("whatsapp-message", onNewMessage)
            socket.on("whatsapp-message-status-update", onMessageStatusUpdate)
            socket.connect()
        } catch (e: URISyntaxException) {
            Log.e("SocketIO", "Setup failed", e)
            showToast("Failed to establish a connection.")
        }
    }
    
    private val onNewMessage = Emitter.Listener { args ->
        runOnUiThread {
            try {
                val data = args[0] as? JSONArray ?: return@runOnUiThread
                var messageAdded = false
                for (i in 0 until data.length()) {
                    val msgJson = data.getJSONObject(i)
                    val messageJid = msgJson.optString("jid")

                    if (messageJid == jid && !msgJson.optBoolean("fromMe")) {
                         if (messages.any { it.id == msgJson.optString("id") }) continue

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
                        messages.add(newMessage)
                        messageAdded = true
                    }
                }
                if(messageAdded) {
                    processAndSubmitMessages(messages.sortedBy { it.timestamp })
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
        val sourceMessage = messages.find { it.id == messageId }
        sourceMessage?.let {
            it.status = newStatus
            if (newId != null) {
                it.id = newId
            }
        }
        adapter.updateStatus(newId ?: messageId, newStatus, newId)
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
