// @path: app/src/main/java/com/radwrld/wami/SharedMediaActivity.kt
package com.radwrld.wami

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.radwrld.wami.AboutActivity.Companion.EXTRA_JID
import com.radwrld.wami.adapter.SharedMediaAdapter
import com.radwrld.wami.databinding.ActivitySharedMediaBinding
import com.radwrld.wami.storage.MessageStorage

class SharedMediaActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySharedMediaBinding
    private val jid by lazy { intent.getStringExtra(EXTRA_JID).orEmpty() }

    private lateinit var messageStorage: MessageStorage
    private lateinit var sharedMediaAdapter: SharedMediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySharedMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (jid.isBlank()) {
            finish()
            return
        }

        messageStorage = MessageStorage(this)

        setupToolbar()
        setupRecyclerView()
        loadMediaMessages()
    }

    private fun setupToolbar() {
        binding.toolbarSharedMedia.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        sharedMediaAdapter = SharedMediaAdapter(this, emptyList())
        binding.rvSharedMedia.apply {
            adapter = sharedMediaAdapter
            layoutManager = GridLayoutManager(this@SharedMediaActivity, 3)
            setHasFixedSize(true)
        }
    }

    private fun loadMediaMessages() {
        val mediaMessages = messageStorage.getMessages(jid)
            .filter { it.hasMedia() }
            .sortedByDescending { it.timestamp }

        if (mediaMessages.isEmpty()) {
            binding.tvNoMedia.visibility = View.VISIBLE
            binding.rvSharedMedia.visibility = View.GONE
        } else {
            binding.tvNoMedia.visibility = View.GONE
            binding.rvSharedMedia.visibility = View.VISIBLE
            sharedMediaAdapter.updateData(mediaMessages)
        }
    }
}

