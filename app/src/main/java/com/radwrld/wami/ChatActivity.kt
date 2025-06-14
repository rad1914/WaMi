// @path: app/src/main/java/com/radwrld/wami/ChatActivity.kt
package com.radwrld.wami

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    private lateinit var api: WhatsAppApi
    private var socket: Socket? = null
    private lateinit var messageStorage: MessageStorage
    private lateinit var serverConfigStorage: ServerConfigStorage

    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    private val messageIds = mutableSetOf<String>()

    companion object {
        const val STATUS_SENDING = "sending"
        const val STATUS_SENT = "sent"
        const val STATUS_DELIVERED = "delivered"
        const val STATUS_READ = "read"
        const val STATUS_FAILED = "failed"
        const val STATUS_RECEIVED = "received"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Corrected view binding property names to match the XML
        _binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jid = intent.getStringExtra("EXTRA_JID") ?: ""
        contactName = intent.getStringExtra("EXTRA_NAME") ?: "Unknown"
        isGroup = jid.endsWith("@g.us")

        messageStorage = MessageStorage(this)
        serverConfigStorage = ServerConfigStorage(this)
        api = ApiClient.getInstance(this)
        socket = ApiClient.getSocket()

        if (!isValidJid(jid)) {
            showToast("Error: Invalid or missing contact JID.")
            finish()
            return
        }

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    sendMediaMessage(uri)
                }
            }
        }

        setupUI()
        setupSocketListeners()
        loadAndSyncChatHistory()

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        // The reference binding.btnAttach will now resolve correctly.
        binding.btnAttach.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            filePickerLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        ApiClient.connectSocket()
    }

    private fun setupUI() {
        // Use the correct view binding property names (camelCase)
        binding.tvContactName.text = contactName
        binding.tvLastSeen.visibility = View.GONE
        binding.btnBack.setOnClickListener { finish() }

        adapter = ChatAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
        binding.rvMessages.itemAnimator = null

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                // FIXED: Correctly manage visibility of all action buttons
                binding.btnSend.visibility = if (hasText) View.VISIBLE else View.GONE
                binding.btnMic.visibility = if (hasText) View.GONE else View.VISIBLE
                
                // The attachment button is inside the input container, so its visibility isn't
                // tied to the mic/send button container, but we can hide it when there is text.
                binding.btnAttach.visibility = if (hasText) View.GONE else View.VISIBLE
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
            status = STATUS_SENDING,
            isOutgoing = true,
            timestamp = System.currentTimeMillis()
        )
        
        messageStorage.addMessage(jid, message)
        val currentMessages = adapter.currentList.mapNotNull { (it as? ChatListItem.MessageItem)?.message }
        val updatedMessages = currentMessages + message
        processAndSubmitMessages(updatedMessages)

        binding.etMessage.text?.clear()
        sendMessageToServer(message)
    }

    private fun processAndSubmitMessages(messageList: List<Message>) {
        messageIds.clear()
        messageList.forEach { messageIds.add(it.id) }
        val newChatItems = createListWithDividers(messageList.sortedBy { it.timestamp })
        
        val shouldScroll = if (binding.rvMessages.layoutManager is LinearLayoutManager) {
            val layoutManager = binding.rvMessages.layoutManager as LinearLayoutManager
            val lastVisible = layoutManager.findLastVisibleItemPosition()
            lastVisible == -1 || lastVisible >= adapter.itemCount - 2
        } else { true }

        adapter.submitList(newChatItems) {
            if (shouldScroll) {
                binding.rvMessages.scrollToPosition(newChatItems.size - 1)
            }
        }
    }

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
    
    private fun sendMediaMessage(fileUri: Uri) {
        lifecycleScope.launch {
            try {
                val jidRequestBody = jid.toRequestBody("text/plain".toMediaTypeOrNull())
                // Captions can be added here from a dialog in the future
                val captionRequestBody = "".toRequestBody("text/plain".toMediaTypeOrNull())

                val fileStream = contentResolver.openInputStream(fileUri)
                val fileBytes = fileStream?.readBytes()
                fileStream?.close()

                if (fileBytes == null) {
                    showToast("Error reading file.")
                    return@launch
                }

                val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"
                val fileName = getFileName(fileUri)

                val requestFile = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", fileName, requestFile)
                
                showToast("Uploading media...")
                val response = api.sendMedia(jidRequestBody, captionRequestBody, body)

                if (response.success) {
                    showToast("Media sent successfully.")
                    // Refresh history to see the new media message
                    loadAndSyncChatHistory()
                } else {
                    showToast("Failed to send media: ${response.error}")
                }
            } catch (e: Exception) {
                Log.e("SendMedia", "Failed to send media file", e)
                if (e is HttpException && e.code() == 401) {
                    forceLogout()
                } else {
                    showToast("An error occurred while sending the file.")
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if(columnIndex >= 0) result = cursor.getString(columnIndex)
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                // FIXED: Use non-null asserted call (!!) because the `if` condition
                // guarantees `result` is not null at this point.
                result = result!!.substring(cut + 1)
            }
        }
        return result ?: "unknown_file"
    }

    private fun loadAndSyncChatHistory() {
        val localMessages = messageStorage.getMessages(jid)
        processAndSubmitMessages(localMessages)

        lifecycleScope.launch {
            try {
                val serverHistory = api.getHistory(jid)
                // FIXED: Construct absolute media URLs
                val baseUrl = serverConfigStorage.getCurrentServer().removeSuffix("/")
                val serverMessages = serverHistory.map {
                    Message(
                        id = it.id,
                        jid = it.jid,
                        text = it.text,
                        name = it.name ?: contactName,
                        status = it.status,
                        isOutgoing = (it.isOutgoing == 1),
                        timestamp = it.timestamp,
                        senderName = it.name,
                        mediaUrl = it.mediaUrl?.let { url -> "$baseUrl$url" },
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
    
    private val onNewMessage = Emitter.Listener { args ->
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val data = args[0] as? JSONArray ?: return@launch
                val newMessages = mutableListOf<Message>()
                // FIXED: Construct absolute media URLs for incoming messages
                val baseUrl = serverConfigStorage.getCurrentServer().removeSuffix("/")

                for (i in 0 until data.length()) {
                    val msgJson = data.getJSONObject(i)
                    val messageJid = msgJson.optString("jid")

                    if (messageJid == jid && !msgJson.optBoolean("fromMe")) {
                        val messageId = msgJson.optString("id")
                        if (messageId.isNullOrEmpty() || !messageIds.add(messageId)) continue
                        
                        val relativeMediaUrl = msgJson.optString("media_url", null)

                        val newMessage = Message(
                            id = messageId,
                            jid = messageJid,
                            text = msgJson.optString("text", null),
                            name = contactName,
                            status = STATUS_RECEIVED,
                            isOutgoing = false,
                            timestamp = msgJson.optLong("timestamp", System.currentTimeMillis()),
                            senderName = if (isGroup) msgJson.optString("name", null) else null,
                            mediaUrl = relativeMediaUrl?.let { url -> "$baseUrl$url" },
                            mimetype = msgJson.optString("mimetype", null),
                            quotedMessageId = msgJson.optString("quoted_message_id", null),
                            quotedMessageText = msgJson.optString("quoted_message_text", null)
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
        socket?.off("whatsapp-message", onNewMessage)
        socket?.off("whatsapp-message-status-update", onMessageStatusUpdate)
        _binding = null
    }
}
