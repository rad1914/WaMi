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
import io.socket.client.Socket
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import org.json.JSONArray

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
    suspend fun getHistory(@Path("jid") jid: String, @Query("limit") limit: Int = 200): List<ChatMessageDto>

    @POST("send")
    suspend fun sendMessage(@Body req: SendRequest): SendResponse
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
        private const val BASE_URL = "http://192.168.1.68:3007/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jid = intent.getStringExtra("EXTRA_JID") ?: ""
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
                val visible = !s.isNullOrBlank()
                binding.btnSend.visibility = if (visible) View.VISIBLE else View.GONE
                binding.btnMic.visibility = if (visible) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().build())
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
        messages.add(
            Message(contactName, text, "", "", jid, true, System.currentTimeMillis())
        )
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
                } else adapter.updateStatus("", "sent")
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error sending message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadChatHistory() {
        lifecycleScope.launch {
            try {
                messages.addAll(api.getHistory(jid).map {
                    Message(contactName, it.text, it.status, it.id.toString(), it.jid, it.isOutgoing == 1, it.timestamp)
                })
                adapter.notifyDataSetChanged()
                binding.rvMessages.scrollToPosition(messages.size - 1)
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Failed to load history", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSocket() {
        socket = IO.socket(BASE_URL.dropLastWhile { it == '/' })
        socket.on(Socket.EVENT_CONNECT)     { toast("Socket connected") }
        socket.on(Socket.EVENT_CONNECT_ERROR){ toast("Socket connect error") }
        socket.on(Socket.EVENT_DISCONNECT)  { toast("Socket disconnected") }
        socket.on("whatsapp-message", onNewMessage)
        socket.connect()
    }

    private val onNewMessage = io.socket.emitter.Emitter.Listener { args ->
        runOnUiThread {
            val arr = args[0] as JSONArray
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val remote = obj.getJSONObject("key").getString("remoteJid")
                if (remote == jid) {
                    val text = obj.optJSONObject("message")?.optString("conversation") ?: ""
                    val fromMe = obj.getJSONObject("key").optBoolean("fromMe", false)
                    messages.add(Message(contactName, text, "", "", remote, fromMe, System.currentTimeMillis()))
                }
            }
            adapter.notifyDataSetChanged()
            binding.rvMessages.scrollToPosition(messages.size - 1)
        }
    }

    private fun toast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroy() {
        socket.off(); socket.disconnect()
        super.onDestroy()
    }
}
