// @path: app/src/main/java/com/radwrld/wami/ChatActivity.kt
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
import com.radwrld.wami.network.SendMessageRequest
import com.radwrld.wami.network.WhatsAppApi
import com.radwrld.wami.storage.MessageStorage
import com.radwrld.wami.storage.ServerConfigStorage
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var jid: String
    private lateinit var contactName: String
    private var isGroup: Boolean = false

    // --- Data & Network Dependencies ---
    // In an MVVM architecture, these would be injected into a ViewModel via a Repository.
    private lateinit var api: WhatsAppApi
    private var socket: Socket? = null
    private lateinit var messageStorage: MessageStorage
    private lateinit var serverConfigStorage: ServerConfigStorage

    // --- State Management ---
    // This state would live in a ViewModel to survive configuration changes (e.g., screen rotation).
    private val messageIds = mutableSetOf<String>()

    companion object {
        // Using constants for status strings prevents typos and improves code readability.
        const val STATUS_SENDING = "sending"
        const val STATUS_SENT = "sent"
        const val STATUS_DELIVERED = "delivered"
        const val STATUS_READ = "read"
        const val STATUS_FAILED = "failed"
        const val STATUS_RECEIVED = "received"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- ViewModel Responsibility: Initialization ---
        // A ViewModel would handle this initialization logic.
        jid = intent.getStringExtra("EXTRA_JID") ?: ""
        contactName = intent.getStringExtra("EXTRA_NAME") ?: "Unknown"
        isGroup = jid.endsWith("@g.us")

        // --- Dependency Injection would provide these instances ---
        messageStorage = MessageStorage(this)
        serverConfigStorage = ServerConfigStorage(this)
        api = ApiClient.getInstance(this)
        socket = ApiClient.getSocket()

        if (!isValidJid(jid)) {
            showToast("Error: Invalid or missing contact JID.")
            finish()
            return
        }

        setupUI()
        setupSocketListeners()

        // The ViewModel would expose a Flow or LiveData<List<Message>> that the Activity observes.
        // It would be responsible for triggering this initial data load.
        loadAndSyncChatHistory()

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                // The Activity calls a method on the ViewModel, e.g., viewModel.sendMessage(text).
                sendMessage(text)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // This could be managed by a lifecycle-aware component.
        ApiClient.connectSocket()
    }

    private fun setupUI() {
        binding.tvContactName.text = contactName
        binding.tvLastSeen.visibility = View.GONE
        binding.btnBack.setOnClickListener { finish() }

        adapter = ChatAdapter() // The adapter would be initialized once.
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

    /**
     * --- ViewModel & Repository Responsibility ---
     * This entire function represents the core business logic for sending a message.
     * In MVVM:
     * 1. The ViewModel would have a `sendMessage(text: String)` method.
     * 2. It would create the temporary message object.
     * 3. It would call a `repository.sendMessage(message)` method.
     * 4. The Repository would handle both saving to local storage and making the API call.
     */
    private fun sendMessage(text: String) {
        val tempId = UUID.randomUUID().toString()
        val message = Message(
            id = tempId,
            jid = jid,
            name = contactName,
            text = text,
            status = STATUS_SENDING,
            isOutgoing = true,
            timestamp = System.currentTimeMillis()
        )

        // Optimistically update the UI. A ViewModel would update its StateFlow/LiveData.
        messageStorage.addMessage(jid, message)
        val currentMessages = adapter.currentList.mapNotNull { (it as? ChatListItem.MessageItem)?.message }
        val updatedMessages = currentMessages + message
        processAndSubmitMessages(updatedMessages)

        binding.etMessage.text?.clear()
        sendMessageToServer(message)
    }

    /**
     * --- UI / Data Processing Responsibility ---
     * This logic processes a list of messages for display, including adding dividers.
     * It's good practice to have this kind of mapping logic inside a ViewModel or a dedicated mapper class.
     */
    private fun processAndSubmitMessages(messageList: List<Message>) {
        // PERFORMANCE: Populate the set of known IDs for quick lookups.
        messageIds.clear()
        messageList.forEach { messageIds.add(it.id) }

        val newChatItems = createListWithDividers(messageList.sortedBy { it.timestamp })
        
        val shouldScroll = if (binding.rvMessages.layoutManager is LinearLayoutManager) {
            val layoutManager = binding.rvMessages.layoutManager as LinearLayoutManager
            val lastVisible = layoutManager.findLastVisibleItemPosition()
            // Scroll if user is at the bottom or the list is new (or a message from self was sent).
            lastVisible == -1 || lastVisible >= adapter.itemCount - 2
        } else {
            true
        }

        // The Activity observes data from the ViewModel and submits it to the adapter.
        adapter.submitList(newChatItems) {
            if (shouldScroll) {
                binding.rvMessages.scrollToPosition(newChatItems.size - 1)
            }
        }
    }

    // This is pure data transformation logic, perfect for a helper or mapper class.
    private fun createListWithDividers(messages: List<Message>): List<ChatListItem> {
        val items = mutableListOf<ChatListItem>()
        items.add(ChatListItem.WarningItem)
        if (messages.isEmpty()) return items
        var lastTimestamp: Long = 0
        messages.forEach { message ->
            if (shouldShowDivider(lastTimestamp, message.timestamp)) {
                items.add(ChatListItem.DividerItem(message.timestamp, isDifferentDay(lastTimestamp, message.timestamp)))
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

    /**
     * --- Repository Responsibility ---
     * This function should live in the Repository. The ViewModel would just call `repository.sendMessage(...)`.
     */
    private fun sendMessageToServer(message: Message) {
        if (!isValidJid(jid)) {
            showToast("Cannot send message: Invalid JID format.")
            updateMessageStatusInUI(message.id, STATUS_FAILED)
            messageStorage.updateMessageStatus(jid, message.id, STATUS_FAILED)
            return
        }

        lifecycleScope.launch {
            try {
                val request = SendMessageRequest(jid = jid, text = message.text ?: "", tempId = message.id)
                val response = api.sendMessage(request)
                val tempId = response.tempId ?: message.id

                if (response.success && response.messageId != null) {
                    messageStorage.updateMessage(jid, tempId, response.messageId, STATUS_SENT)
                    updateMessageStatusInUI(tempId, STATUS_SENT, response.messageId)
                } else {
                    messageStorage.updateMessageStatus(jid, tempId, STATUS_FAILED)
                    updateMessageStatusInUI(tempId, STATUS_FAILED)
                    showToast("Error sending: ${response.error ?: "Unknown"}")
                }
            } catch (e: Exception) {
                Log.e("SendMessage", "API call failed for tempId ${message.id}", e)
                if (e is HttpException && e.code() == 401) {
                    forceLogout()
                } else {
                    showToast("Failed to send message.")
                    updateMessageStatusInUI(message.id, STATUS_FAILED)
                    messageStorage.updateMessageStatus(jid, message.id, STATUS_FAILED)
                }
            }
        }
    }

    /**
     * --- Repository & ViewModel Responsibility ---
     * The Repository would be responsible for fetching from local and remote sources and merging them.
     * The ViewModel would trigger this and expose the final, combined list to the UI.
     */
    private fun loadAndSyncChatHistory() {
        val localMessages = messageStorage.getMessages(jid)
        processAndSubmitMessages(localMessages)

        lifecycleScope.launch {
            try {
                val serverHistory = api.getHistory(jid)
                // APPLIED: Mapping now includes media and reply fields.
                val serverMessages = serverHistory.map {
                    Message(
                        id = it.id,
                        jid = it.jid,
                        text = it.text,
                        name = contactName,
                        status = it.status,
                        isOutgoing = (it.isOutgoing == 1),
                        timestamp = it.timestamp,
                        senderName = null, // This could be enriched later for groups
                        mediaUrl = it.mediaUrl,
                        mimetype = it.mimetype,
                        quotedMessageId = it.quotedMessageId,
                        quotedMessageText = it.quotedMessageText
                    )
                }
                val unsentLocal = messageStorage.getMessages(jid).filter { it.status == STATUS_SENDING }
                val combined = (serverMessages + unsentLocal).distinctBy { it.id }.sortedBy { it.timestamp }

                messageStorage.saveMessages(jid, combined)
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

    // --- Socket Listeners ---
    // In MVVM, the Repository would listen to the socket and expose new messages/updates via a Flow.
    private val onNewMessage = Emitter.Listener { args ->
        lifecycleScope.launch(Dispatchers.Default) { // Move JSON parsing off the main thread
            try {
                val data = args[0] as? JSONArray ?: return@launch
                val newMessages = mutableListOf<Message>()

                for (i in 0 until data.length()) {
                    val msgJson = data.getJSONObject(i)
                    val messageJid = msgJson.optString("jid")

                    if (messageJid == jid && !msgJson.optBoolean("fromMe")) {
                        val messageId = msgJson.optString("id")
                        // PERFORMANCE: Use the Set for an O(1) duplicate check
                        if (messageId.isNullOrEmpty() || !messageIds.add(messageId)) continue

                        // APPLIED: Parsing now includes media and reply fields.
                        val newMessage = Message(
                            id = messageId,
                            jid = messageJid,
                            text = msgJson.optString("text"),
                            name = contactName,
                            status = STATUS_RECEIVED,
                            isOutgoing = false,
                            timestamp = msgJson.optLong("timestamp", System.currentTimeMillis()),
                            senderName = if (isGroup) msgJson.optString("pushName", "Unknown") else null,
                            mediaUrl = msgJson.optString("media_url"),
                            mimetype = msgJson.optString("mimetype"),
                            quotedMessageId = msgJson.optString("quoted_message_id"),
                            quotedMessageText = msgJson.optString("quoted_message_text")
                        )
                        messageStorage.addMessage(jid, newMessage)
                        newMessages.add(newMessage)
                    }
                }
                if(newMessages.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val currentMessages = adapter.currentList.mapNotNull { (it as? ChatListItem.MessageItem)?.message }
                        val updatedMessages = currentMessages + newMessages
                        processAndSubmitMessages(updatedMessages)
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
    
    private fun setupSocketListeners() {
        socket?.on("whatsapp-message", onNewMessage)
        socket?.on("whatsapp-message-status-update", onMessageStatusUpdate)
    }

    private fun updateMessageStatusInUI(messageId: String, newStatus: String, newId: String? = null) {
        val currentList = adapter.currentList.toMutableList()
        val indexToUpdate = currentList.indexOfFirst {
            it is ChatListItem.MessageItem && it.message.id == messageId
        }

        if (indexToUpdate != -1) {
            val itemToUpdate = currentList[indexToUpdate] as ChatListItem.MessageItem
            val updatedMessage = itemToUpdate.message.copy(
                status = newStatus,
                id = newId ?: itemToUpdate.message.id
            )
            currentList[indexToUpdate] = ChatListItem.MessageItem(updatedMessage)
            adapter.submitList(currentList)
        }
    }

    private fun isValidJid(jid: String): Boolean {
        if (jid.isBlank()) return false
        return jid.endsWith("@s.whatsapp.net") || jid.endsWith("@g.us")
    }

    private fun showToast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
    }

    private fun forceLogout() {
        showToast("Session expired. Please log in again.")
        ApiClient.close()
        serverConfigStorage.saveSessionId(null)
        serverConfigStorage.saveLoginState(false)

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }



    override fun onDestroy() {
        super.onDestroy()
        // A Repository listening to a socket would manage its own lifecycle.
        socket?.off("whatsapp-message", onNewMessage)
        socket?.off("whatsapp-message-status-update", onMessageStatusUpdate)
        _binding = null
    }
}
