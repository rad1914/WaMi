package com.radwrld.wami

import android.content.Intent
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
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.WhatsAppApi
import com.radwrld.wami.storage.MessageStorage
import com.radwrld.wami.storage.ServerConfigStorage // Import statement
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class ChatActivity : AppCompatActivity() {

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private val chatItemsForAdapter = mutableListOf<ChatListItem>()
    private lateinit var api: WhatsAppApi
    private var socket: Socket? = null
    private lateinit var messageStorage: MessageStorage
    private lateinit var serverConfigStorage: ServerConfigStorage // Declare the property
    private lateinit var jid: String
    private lateinit var contactName: String
    private var isGroup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jid = intent.getStringExtra("EXTRA_JID") ?: ""
        contactName = intent.getStringExtra("EXTRA_NAME") ?: "Unknown"
        messageStorage = MessageStorage(this)
        serverConfigStorage = ServerConfigStorage(this) // Initialize the property
        isGroup = jid.endsWith("@g.us")

        if (!isValidJid(jid)) {
            showToast("Error: Invalid or missing contact JID.")
            finish()
            return
        }

        // Use the centralized ApiClient
        api = ApiClient.getInstance(this)
        socket = ApiClient.getSocket()

        setupUI()
        setupSocketListeners()
        loadAndSyncChatHistory()

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ApiClient.connectSocket()
    }

    override fun onPause() {
        super.onPause()
        // To save battery, you might disconnect here and reconnect in onResume.
        // For real-time notifications even when the app is in the background (but activity is paused),
        // you might leave it connected. For this app, we'll keep it simple.
        // ApiClient.disconnectSocket()
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
        binding.rvMessages.itemAnimator = null

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
            val lastVisible = layoutManager.findLastVisibleItemPosition()
            lastVisible == -1 || lastVisible >= adapter.itemCount - 2
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
        items.add(ChatListItem.WarningItem)

        if (messages.isEmpty()) return items

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
        if (prevTs == 0L) return true
        if (isDifferentDay(prevTs, currentTs)) return true
        return (currentTs - prevTs) > TimeUnit.MINUTES.toMillis(30)
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
                val tempId = response.tempId ?: message.id

                if (response.success && response.messageId != null) {
                    messageStorage.updateMessage(jid, tempId, response.messageId, "sent")
                    updateMessageStatusInUI(tempId, "sent", response.messageId)
                } else {
                    messageStorage.updateMessageStatus(jid, tempId, "failed")
                    updateMessageStatusInUI(tempId, "failed")
                    showToast("Error sending: ${response.error ?: "Unknown"}")
                }
            } catch (e: Exception) {
                Log.e("SendMessage", "API call failed for tempId ${message.id}", e)
                if (e is HttpException && e.code() == 401) {
                    forceLogout()
                } else {
                    showToast("Failed to send message.")
                    updateMessageStatusInUI(message.id, "failed")
                    messageStorage.updateMessageStatus(jid, message.id, "failed")
                }
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
                    Message(it.id, it.jid, it.text, contactName, it.status, (it.isOutgoing == 1), it.timestamp, null)
                }

                val unsentLocal = messageStorage.getMessages(jid).filter { it.status == "sending" }
                val combined = (serverMessages + unsentLocal).distinctBy { it.id }.sortedBy { it.timestamp }

                messageStorage.saveMessages(jid, combined)

                messages.clear()
                messages.addAll(combined)
                processAndSubmitMessages(combined)
            } catch (e: Exception) {
                Log.e("ChatHistory", "Failed to sync history for JID: $jid", e)
                 if (e is HttpException && e.code() == 401) {
                    forceLogout()
                } else {
                    showToast("Failed to sync chat history. Using local cache.")
                }
            }
        }
    }

    private fun setupSocketListeners() {
        socket?.on("whatsapp-message", onNewMessage)
        socket?.on("whatsapp-message-status-update", onMessageStatusUpdate)
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

                        val newMessage = Message(
                            id = msgJson.optString("id"),
                            jid = messageJid,
                            text = msgJson.optString("text"),
                            name = contactName,
                            status = "received",
                            isOutgoing = false,
                            timestamp = msgJson.optLong("timestamp", System.currentTimeMillis()),
                            senderName = if (isGroup) msgJson.optString("pushName", "Unknown") else null
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
        return !jid.startsWith("@")
    }

    private fun showToast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
    }

    private fun forceLogout() {
        showToast("Session expired. Please log in again.")
        ApiClient.close()
        // Use the class property to clear session from storage
        serverConfigStorage.saveSessionId(null)
        serverConfigStorage.saveLoginState(false)

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove listeners to prevent memory leaks
        socket?.off("whatsapp-message", onNewMessage)
        socket?.off("whatsapp-message-status-update", onMessageStatusUpdate)
        _binding = null
    }
}
