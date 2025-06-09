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
import com.google.gson.annotations.SerializedName
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import org.json.JSONArray

// DTOs
data class ChatMessageDto(
    val id: Long,
    val jid: String,
    val text: String,
    @SerializedName("isOutgoing") val isOutgoing: Int,
    val status: String,
    val timestamp: Long
)

data class SendRequest(val jid: String, val text: String)
data class SendResponse(val status: String, val id: String?)

interface WhatsAppApi {
    @GET("history/{jid}")
    suspend fun getHistory(
        @Path("jid") jid: String,
        @Query("limit") limit: Int = 200
    ): List<ChatMessageDto>

    @POST("send")
    suspend fun sendMessage(@Body req: SendRequest): SendResponse
}

class SimpleTextWatcher(private val onTextChanged: (CharSequence?) -> Unit) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        onTextChanged(s)
    }
    override fun afterTextChanged(s: Editable?) {}
}

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var api: WhatsAppApi
    private lateinit var socket: Socket
    private lateinit var jid: String
    private lateinit var contactName: String

    companion object {
        private const val BASE_URL = "http://192.168.1.68:3007/"  // cambia según tu servidor
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent data
        jid = intent.getStringExtra("EXTRA_JID") ?: ""
        contactName = intent.getStringExtra("EXTRA_NAME") ?: "Unknown"

        // Header UI
        binding.tvContactName.text = contactName
        binding.tvLastSeen.visibility = View.GONE

        binding.btnBack.setOnClickListener { finish() }

        // RecyclerView
        adapter = ChatAdapter(messages)
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        // Toggle send/mic
        binding.etMessage.addTextChangedListener(SimpleTextWatcher { s ->
            val hasText = !s.isNullOrBlank()
            binding.btnSend.visibility = if (hasText) View.VISIBLE else View.GONE
            binding.btnMic.visibility  = if (hasText) View.GONE    else View.VISIBLE
        })

        // Retrofit setup
        val okClient = OkHttpClient.Builder().build()
        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WhatsAppApi::class.java)

        // Socket.IO & history
        setupSocket()
        loadChatHistory()

        // Send action
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            addMessageToUI(text)
            sendMessageToServer(text)
        }
    }

    private fun addMessageToUI(text: String) {
        val msg = Message(
            name       = contactName,
            text       = text,
            status     = "",                         // sin estado inicial
            id         = "",                         // sin id hasta respuesta del server
            jid        = jid,
            isOutgoing = true,
            timestamp  = System.currentTimeMillis()
        )
        messages.add(msg)
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
        binding.etMessage.text?.clear()
    }

    private fun sendMessageToServer(text: String) {
        lifecycleScope.launch {
            try {
                val resp = api.sendMessage(SendRequest(jid, text))
                if (resp.status != "sent") {
                    Toast.makeText(this@ChatActivity, "Send failed", Toast.LENGTH_SHORT).show()
                } else if (!resp.id.isNullOrBlank()) {
                    // Opcional: actualizar el id y el status en el mensaje reciente
                    adapter.updateStatus("", "sent")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ChatActivity, "Error sending message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadChatHistory() {
        lifecycleScope.launch {
            try {
                api.getHistory(jid).forEach {
                    messages.add(
                        Message(
                            name       = contactName,
                            text       = it.text,
                            status     = it.status,
                            id         = it.id.toString(),
                            jid        = it.jid,
                            isOutgoing = it.isOutgoing == 1,
                            timestamp  = it.timestamp
                        )
                    )
                }
                adapter.notifyDataSetChanged()
                binding.rvMessages.scrollToPosition(messages.size - 1)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ChatActivity, "Failed to load history", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSocket() {
        socket = IO.socket(BASE_URL.removeSuffix("/"))
        socket.on(Socket.EVENT_CONNECT) {
            runOnUiThread { Toast.makeText(this, "Socket connected", Toast.LENGTH_SHORT).show() }
        }
        socket.on(Socket.EVENT_CONNECT_ERROR) {
            runOnUiThread { Toast.makeText(this, "Socket connect error", Toast.LENGTH_SHORT).show() }
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            runOnUiThread { Toast.makeText(this, "Socket disconnected", Toast.LENGTH_SHORT).show() }
        }
        socket.io().on(Manager.EVENT_RECONNECT) {
            runOnUiThread { Toast.makeText(this, "Socket reconnected", Toast.LENGTH_SHORT).show() }
        }
        socket.on("whatsapp-message", onNewMessage)
        socket.connect()
    }

    private val onNewMessage = Emitter.Listener { args ->
        runOnUiThread {
            val arr = args[0] as JSONArray
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val remote = obj.getJSONObject("key").getString("remoteJid")
                if (remote == jid) {
                    val text   = obj.optJSONObject("message")?.optString("conversation") ?: ""
                    val fromMe = obj.getJSONObject("key").optBoolean("fromMe", false)
                    messages.add(
                        Message(
                            name       = contactName,
                            text       = text,
                            status     = "",            // sin estado del socket
                            id         = "",
                            jid        = remote,
                            isOutgoing = fromMe,
                            timestamp  = System.currentTimeMillis()
                        )
                    )
                }
            }
            adapter.notifyDataSetChanged()
            binding.rvMessages.scrollToPosition(messages.size - 1)
        }
    }

    override fun onDestroy() {
        socket.off()
        socket.disconnect()
        super.onDestroy()
    }
}
