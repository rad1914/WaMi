// @path: app/src/main/java/com/radwrld/wami/ChatActivity.kt
package com.radwrld.wami

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ChatAdapter
import com.radwrld.wami.adapter.ChatListItem
import com.radwrld.wami.databinding.ActivityChatBinding
import com.radwrld.wami.model.Message
import com.radwrld.wami.ui.viewmodel.ChatViewModel
import com.radwrld.wami.ui.viewmodel.ChatViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter

    private val jid      by lazy { intent.getStringExtra("EXTRA_JID").orEmpty() }
    private val name     by lazy { intent.getStringExtra("EXTRA_NAME") ?: "Unknown" }
    private val isGroup  get() = jid.endsWith("@g.us")

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(application, jid, name)
    }

    private val pickFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                viewModel.sendMedia(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (jid.isBlank()) {
            Toast.makeText(this, "Error: Invalid JID.", Toast.LENGTH_LONG).show()
            finish(); return
        }
        binding = ActivityChatBinding.inflate(layoutInflater).also { setContentView(it.root) }
        setupUI()
        observeVM()
    }

    override fun onResume() {
        super.onResume()
        // ELIMINADO: ApiClient.connectSocket()
        // SyncManager maneja la conexión automáticamente.
    }

    private fun setupUI() = with(binding) {
        tvContactName.text = name
        tvLastSeen.visibility = View.GONE
        btnBack.setOnClickListener { finish() }

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadOlderMessages()
        }

        adapter = ChatAdapter(isGroup).apply {
            onMediaClickListener = { msg ->
                lifecycleScope.launch {
                    progressBar.visibility = View.VISIBLE
                    val file = viewModel.getMediaFile(msg)
                    progressBar.visibility = View.GONE
                    if (file != null) {
                        startActivity(Intent(this@ChatActivity, MediaViewActivity::class.java).apply {
                            setDataAndType(file.toUri(), msg.mimetype)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    } else {
                        Toast.makeText(this@ChatActivity, "Media downloading…", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            onReactionClicked = viewModel::sendReaction
        }

        rvMessages.apply {
            layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
            adapter = this@ChatActivity.adapter
            itemAnimator = null
            setOnTouchListener { _, e ->
                if (e.action == MotionEvent.ACTION_DOWN) {
                    this@ChatActivity.adapter.collapseExpandedViewHolder()
                }
                false
            }
        }

        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val has = !s.isNullOrBlank()
                btnSend.visibility   = if (has) View.VISIBLE else View.GONE
                btnMic.visibility    = if (has) View.GONE    else View.VISIBLE
                btnAttach.visibility = if (has) View.GONE    else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSend.setOnClickListener {
            etMessage.text.toString().trim().takeIf { it.isNotEmpty() }?.let {
                viewModel.sendText(it)
                etMessage.text?.clear()
            }
        }

        btnAttach.setOnClickListener {
            pickFile.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*","video/*","image/webp"))
                addCategory(Intent.CATEGORY_OPENABLE)
            })
        }
    }

    private fun observeVM() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect {
                        binding.progressBar.visibility = if (it.loading) View.VISIBLE else View.GONE
                        binding.swipeRefreshLayout.isRefreshing = it.loadingOlder
                    }
                }
                launch {
                    viewModel.visibleMessages.collectLatest { msgs -> process(msgs) }
                }
                launch {
                    viewModel.errors.collect {
                        Toast.makeText(this@ChatActivity, it, Toast.LENGTH_LONG).show()
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
    }

    private fun process(messages: List<Message>) {
        val lm = binding.rvMessages.layoutManager as LinearLayoutManager
        val atBottom = lm.findLastVisibleItemPosition() >= adapter.itemCount - 2

        val items = mutableListOf<ChatListItem>().apply {
            add(ChatListItem.WarningItem)
            var lastTs = 0L
            for (m in messages) {
                if (shouldShowDivider(lastTs, m.timestamp)) add(ChatListItem.DividerItem(m.timestamp))
                add(ChatListItem.MessageItem(m))
                lastTs = m.timestamp
            }
        }

        adapter.submitList(items) {
            if (atBottom) binding.rvMessages.scrollToPosition(items.lastIndex)
        }
    }

    private fun shouldShowDivider(prev: Long, cur: Long): Boolean {
        if (prev == 0L) return true
        val gap = cur - prev
        return gap > TimeUnit.MINUTES.toMillis(30) || isDiffDay(prev, cur)
    }

    private fun isDiffDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR)  != c2.get(Calendar.YEAR) ||
               c1.get(Calendar.DAY_OF_YEAR) != c2.get(Calendar.DAY_OF_YEAR)
    }
}
