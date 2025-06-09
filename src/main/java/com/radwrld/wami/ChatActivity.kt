// ChatActivity.kt
package com.radwrld.wami

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.adapter.ChatAdapter
import com.radwrld.wami.api.WaApi
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.RetrofitManager
import com.radwrld.wami.network.SocketManager
import com.radwrld.wami.network.MessageSender
import com.radwrld.wami.storage.ServerConfigStorage
import io.socket.client.Socket

class ChatActivity : AppCompatActivity() {
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var tvStatus: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var rvChat: RecyclerView

    private lateinit var config: ServerConfigStorage
    private lateinit var socketManager: SocketManager
    private lateinit var messageSender: MessageSender
    private var initialJid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        config = ServerConfigStorage(this)
        initialJid = intent.getStringExtra("EXTRA_JID") ?: ""

        tvStatus = findViewById(R.id.tv_last_seen)
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById(R.id.btn_send)
        btnMic = findViewById(R.id.btn_mic)
        rvChat = findViewById(R.id.rv_messages)

        adapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvChat.adapter = adapter

        RetrofitManager(this, config) {
            setConnected()
        }.start()

        socketManager = SocketManager(this, config, initialJid, messages, adapter, rvChat, tvStatus)
        socketManager.connect()

        messageSender = MessageSender(this, config, ::setConnected, tvStatus)

        btnSend.setOnClickListener { sendMessage() }

        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                btnSend.visibility = if (hasText) View.VISIBLE else View.GONE
                btnMic.visibility = if (hasText) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun sendMessage() {
        val jidBase = initialJid.substringBefore("@")
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        etMessage.text.clear()
        btnSend.isEnabled = false

        val msg = Message(jidBase, text, "", jidBase, false, true)
        messages.add(msg)
        adapter.notifyItemInserted(messages.size - 1)
        rvChat.scrollToPosition(messages.size - 1)

        messageSender.sendMessage(initialJid, text, ::setConnected)

        btnSend.postDelayed({ btnSend.isEnabled = true }, 500)
    }

    private fun setConnected() {
        tvStatus.text = "Connected"
        tvStatus.setTextColor(Color.GREEN)
        etMessage.isEnabled = true
        btnSend.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager.cleanup()
    }
}
