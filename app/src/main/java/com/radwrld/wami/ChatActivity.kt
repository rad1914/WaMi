// @path: app/src/main/java/com/radwrld/wami/ChatActivity.kt
package com.radwrld.wami

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ChatAdapter
import com.radwrld.wami.adapter.ChatListItem
import com.radwrld.wami.databinding.ActivityChatBinding
import com.radwrld.wami.model.Message
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.ui.viewmodel.ChatViewModel
import com.radwrld.wami.ui.viewmodel.ChatViewModelFactory // FIXED: This import will now resolve.
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class ChatActivity : AppCompatActivity() {

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private lateinit var jid: String
    private lateinit var contactName: String

    // FIXED: This delegate will now compile correctly as ChatViewModelFactory is found.
    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(
            application,
            intent.getStringExtra("EXTRA_JID") ?: "",
            intent.getStringExtra("EXTRA_NAME") ?: "Unknown"
        )
    }

    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jid = intent.getStringExtra("EXTRA_JID") ?: ""
        contactName = intent.getStringExtra("EXTRA_NAME") ?: "Unknown"

        if (jid.isBlank() || !isValidJid(jid)) {
            Toast.makeText(this, "Error: Invalid or missing contact JID.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri -> viewModel.sendMediaMessage(uri) }
            }
        }

        setupUI()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        ApiClient.connectSocket()
    }

    private fun setupUI() {
        binding.tvContactName.text = contactName
        binding.tvLastSeen.visibility = View.GONE
        binding.btnBack.setOnClickListener { finish() }

        adapter = ChatAdapter()
        adapter.onMediaClickListener = { message ->
            if (message.mediaUrl != null && message.mimetype != null) {
                startActivity(Intent(this, MediaViewActivity::class.java).apply {
                    putExtra(MediaViewActivity.EXTRA_URL, message.mediaUrl)
                    putExtra(MediaViewActivity.EXTRA_MIMETYPE, message.mimetype)
                })
            } else {
                Toast.makeText(this, "Media is not available.", Toast.LENGTH_SHORT).show()
            }
        }

        adapter.onReactionClicked = { message, emoji ->
            viewModel.sendReaction(message, emoji)
        }

        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter
        binding.rvMessages.itemAnimator = null

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                binding.btnSend.visibility = if (hasText) View.VISIBLE else View.GONE
                binding.btnMic.visibility = if (hasText) View.GONE else View.VISIBLE
                binding.btnAttach.visibility = if (hasText) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendTextMessage(text)
                binding.etMessage.text?.clear()
            }
        }

        binding.btnAttach.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            filePickerLauncher.launch(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // The isLoading state is available if you add a ProgressBar to your layout
                    // binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    processAndSubmitMessages(state.messages)

                    state.error?.let {
                        Toast.makeText(this@ChatActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun processAndSubmitMessages(messageList: List<Message>) {
        val newChatItems = createListWithDividers(messageList)
        
        val layoutManager = binding.rvMessages.layoutManager as LinearLayoutManager
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        val shouldScroll = lastVisible == -1 || lastVisible >= adapter.itemCount - 2

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

    private fun isValidJid(jid: String): Boolean {
        return jid.endsWith("@s.whatsapp.net") || jid.endsWith("@g.us")
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
