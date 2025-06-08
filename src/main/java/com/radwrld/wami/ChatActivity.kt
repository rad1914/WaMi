// app/src/main/java/com/radwrld/wami/ChatActivity.kt
package com.radwrld.wami

import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.adapter.ChatAdapter
import com.radwrld.wami.api.SendMessageRequest
import com.radwrld.wami.api.StatusResponse
import com.radwrld.wami.api.WaApi
import com.radwrld.wami.model.Message
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import okhttp3.OkHttpClient
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit

class ChatActivity : AppCompatActivity() {

    private lateinit var waApi: WaApi
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()

    private lateinit var tvStatus: TextView
    private lateinit var etPhone: EditText
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var rvChat: RecyclerView

    private var initialContactJid = ""
    private lateinit var socket: Socket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initialContactJid = intent.getStringExtra("EXTRA_JID") ?: ""

        supportActionBar?.apply {
            title = intent.getStringExtra("EXTRA_NAME") ?: "Chat"
            setDisplayHomeAsUpEnabled(true)
        }

        tvStatus  = findViewById(R.id.tvChatStatus)
        etPhone   = findViewById(R.id.etPhone)
        etMessage = findViewById(R.id.etMessage)
        btnSend   = findViewById(R.id.btnSend)
        rvChat    = findViewById(R.id.rvChat)

        // fill phone field, stripping any domain
        etPhone.setText(initialContactJid.substringBefore("@"))

        setConnectionStatus("Connecting…", Color.YELLOW)
        etMessage.isEnabled = false
        btnSend.isEnabled   = false

        setupRecyclerView()
        setupRetrofit()
        setupSocketIO()

        btnSend.setOnClickListener { sendMessage() }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvChat.adapter = adapter
    }

    private fun setupRetrofit() {
        waApi = Retrofit.Builder()
            .baseUrl("http://192.168.1.68:3007/")
            .client(OkHttpClient.Builder()
                .callTimeout(15, TimeUnit.SECONDS)
                .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WaApi::class.java)

        waApi.getStatus().enqueue(object : Callback<StatusResponse> {
            override fun onResponse(call: Call<StatusResponse>, resp: Response<StatusResponse>) {
                if (resp.isSuccessful && resp.body()?.connected == true) {
                    onConnected()
                } else {
                    setConnectionStatus("Disconnected", Color.RED)
                }
            }
            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                setConnectionStatus("Error", Color.RED)
            }
        })
    }

    private fun setupSocketIO() {
        try {
            socket = IO.socket("http://192.168.1.68:3007")
            socket.on(Socket.EVENT_CONNECT)    { runOnUiThread { onConnected() } }
            socket.on(Socket.EVENT_DISCONNECT) { runOnUiThread { setConnectionStatus("Disconnected", Color.RED) } }
            socket.on("whatsapp-message", onIncomingMessage)
            socket.connect()
        } catch (e: URISyntaxException) {
            Toast.makeText(this, "Socket.IO URI error", Toast.LENGTH_SHORT).show()
        }
    }

    private val onIncomingMessage = Emitter.Listener { args ->
        val upserts = args.getOrNull(0) as? JSONArray ?: return@Listener
        for (i in 0 until upserts.length()) {
            val up = upserts.getJSONObject(i)
            val msgsArr = up.optJSONArray("messages") ?: continue
            for (j in 0 until msgsArr.length()) {
                val msgObj = msgsArr.getJSONObject(j)
                val key = msgObj.getJSONObject("key")
                val fromJid = key.getString("remoteJid")
                val isSelf = key.optBoolean("fromMe", false)
                val body = msgObj
                    .optJSONObject("message")
                    ?.optString("conversation")
                    ?: continue

                // Only add messages for current chat
                if (fromJid.startsWith(etPhone.text.toString())) {
                    val senderNumber = fromJid.substringBefore("@")
                    val incoming = Message(
                        name       = senderNumber,
                        lastMessage= body,
                        avatarUrl  = "",
                        phoneNumber= senderNumber,
                        isOnline   = false,
                        isOutgoing = isSelf
                    )
                    runOnUiThread {
                        messages.add(incoming)
                        adapter.notifyItemInserted(messages.size - 1)
                        rvChat.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val rawPhone = etPhone.text.toString().trim()
        val text     = etMessage.text.toString().trim()
        if (rawPhone.isEmpty() || text.isEmpty()) return

        val jid = "${rawPhone.filter { it.isDigit() }}@s.whatsapp.net"

        // show outgoing immediately
        val outgoing = Message(
            name        = rawPhone,
            lastMessage = text,
            avatarUrl   = "",
            phoneNumber = rawPhone,
            isOnline    = false,
            isOutgoing  = true
        )
        messages.add(outgoing)
        adapter.notifyItemInserted(messages.size - 1)
        rvChat.scrollToPosition(messages.size - 1)
        etMessage.text.clear()

        waApi.sendMessage(SendMessageRequest(jid, text))
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, resp: Response<Void>) {
                    if (!resp.isSuccessful) {
                        Toast.makeText(this@ChatActivity, "Send error ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@ChatActivity, "Send failed", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun onConnected() {
        setConnectionStatus("Connected", Color.GREEN)
        etMessage.isEnabled = true
        btnSend.isEnabled   = true
    }

    private fun setConnectionStatus(text: String, color: Int) {
        tvStatus.text = text
        tvStatus.setTextColor(color)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
        socket.off(Socket.EVENT_CONNECT)
        socket.off(Socket.EVENT_DISCONNECT)
        socket.off("whatsapp-message", onIncomingMessage)
    }
}
