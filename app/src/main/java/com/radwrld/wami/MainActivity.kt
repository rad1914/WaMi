package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ConversationAdapter
import com.radwrld.wami.adapter.SearchResultAdapter
import com.radwrld.wami.databinding.ActivityMainBinding
import com.radwrld.wami.model.Contact
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.sync.SyncService
import com.radwrld.wami.ui.viewmodel.ConversationListViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ConversationListViewModel by viewModels()
    private lateinit var serverConfig: ServerConfigStorage

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var searchAdapter: SearchResultAdapter
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        serverConfig = ServerConfigStorage(this)
        setupUI()
        observeViewModel()

        startService(Intent(this, SyncService::class.java).apply {
            action = SyncService.ACTION_START
        })
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        searchView = (menu.findItem(R.id.action_search).actionView as? SearchView)?.apply {
            queryHint = "Search..."
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(q: String?) = true
                override fun onQueryTextChange(q: String?) = viewModel.onSearchQueryChanged(q.orEmpty()).let { true }
            })
            setOnCloseListener {
                viewModel.onSearchQueryChanged("")
                false
            }
        }
        return true
    }

    private fun setupUI() {
        with(binding) {
            toolbar.setNavigationOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }

            navMessages.isSelected = true

            navMessages.setOnClickListener {
                navMessages.isSelected = true
                navContacts.isSelected = false
            }

            navAdd.setOnClickListener { showQuickChatDialog() }

            navContacts.setOnClickListener {
                navContacts.isSelected = true
                navMessages.isSelected = false
                startActivity(Intent(this@MainActivity, ContactsActivity::class.java))
            }

            swipeRefreshLayout.setOnRefreshListener { viewModel.load() }

            conversationAdapter = ConversationAdapter(
                onItemClicked = ::openChat,
                onItemLongClicked = { c, _ -> confirmHide(c) }
            )

            searchAdapter = SearchResultAdapter(::openChat)

            rvMessages.layoutManager = LinearLayoutManager(this@MainActivity)
            rvMessages.adapter = conversationAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.conversationState.collect {
                        binding.swipeRefreshLayout.isRefreshing = it.isLoading
                        if (viewModel.searchState.value.query.isBlank()) {
                            conversationAdapter.submitList(it.conversations)
                        }
                        it.error?.let { e ->
                            Toast.makeText(this@MainActivity, "Error: $e", Toast.LENGTH_LONG).show()
                            if ("401" in e) logout(false)
                        }
                    }
                }
                launch {
                    viewModel.searchState.collect {
                        binding.rvMessages.adapter =
                            if (it.query.isBlank()) conversationAdapter.also {
                                it.submitList(viewModel.conversationState.value.conversations)
                            } else searchAdapter.also { adapter ->
                                adapter.submitList(it.results)
                            }
                    }
                }
            }
        }
    }

    private fun showQuickChatDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            hint = "e.g., 15551234567"
        }
        AlertDialog.Builder(this)
            .setTitle("Fast Contact")
            .setMessage("Enter a phone number to start chat.")
            .setView(input)
            .setPositiveButton("Chat") { d, _ ->
                val number = input.text.toString().trim()
                if (number.isNotEmpty()) {
                    startActivity(Intent(this, ChatActivity::class.java).apply {
                        putExtra("EXTRA_JID", "$number@s.whatsapp.net")
                        putExtra("EXTRA_NAME", number)
                    })
                } else {
                    Toast.makeText(this, "Empty number.", Toast.LENGTH_SHORT).show()
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openChat(contact: Contact) {
        searchView?.apply {
            if (!isIconified) {
                setQuery("", false)
                isIconified = true
            }
        }
        startActivity(Intent(this, ChatActivity::class.java).apply {
            putExtra("EXTRA_JID", contact.id)
            putExtra("EXTRA_NAME", contact.name)
        })
    }

    private fun confirmHide(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Hide Conversation")
            .setMessage("Hide conversation with ${contact.name}?")
            .setPositiveButton("Hide") { _, _ ->
                viewModel.hide(contact.id)
                Toast.makeText(this, "Hidden", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout(callServer: Boolean) {
        startService(Intent(this, SyncService::class.java).apply {
            action = SyncService.ACTION_STOP
        })

        lifecycleScope.launch {
            if (callServer) {
                try {
                    ApiClient.getInstance(this@MainActivity).logout()
                } catch (_: Exception) {}
            }
            ApiClient.close()
            serverConfig.saveSessionId(null)
            serverConfig.saveLoginState(false)
            startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}
