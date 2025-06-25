// @path: app/src/main/java/com/radwrld/wami/MainActivity.kt
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
import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.SyncService
import com.radwrld.wami.storage.ServerConfigStorage
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

    serverConfig = ServerConfigStorage(this)
    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayShowTitleEnabled(false)

    with(binding) {
      toolbar.setNavigationOnClickListener {
        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
      }

      navMessages.setOnClickListener {
        navMessages.isSelected = true
        navSocial.isSelected = false
        navContacts.isSelected = false
      }
      navSocial.setOnClickListener {
        navMessages.isSelected = false
        navSocial.isSelected = true
        navContacts.isSelected = false
        startActivity(Intent(this@MainActivity, SocialActivity::class.java))
      }
      navContacts.setOnClickListener {
        navMessages.isSelected = false
        navSocial.isSelected = false
        navContacts.isSelected = true
        startActivity(Intent(this@MainActivity, ContactsActivity::class.java))
      }
      navAdd.setOnClickListener { showQuickChatDialog() }

      swipeRefreshLayout.setOnRefreshListener { viewModel.load() }

      conversationAdapter = ConversationAdapter(::openChat) { c, _ ->
        AlertDialog.Builder(this@MainActivity)
          .setTitle("Hide Conversation")
          .setMessage("Hide conversation with ${c.name}?")
          .setPositiveButton("Hide") { _, _ ->
            viewModel.hide(c.id)
            Toast.makeText(this@MainActivity, "Hidden", Toast.LENGTH_SHORT).show()
          }
          .setNegativeButton("Cancel", null)
          .show()
      }

      searchAdapter = SearchResultAdapter(::openChat)

      rvMessages.layoutManager = LinearLayoutManager(this@MainActivity)
      rvMessages.adapter = conversationAdapter
    }

    observeViewModel()

    startService(
      Intent(this, SyncService::class.java)
        .setAction(SyncService.ACTION_START)
    )
  }

  override fun onResume() {
    super.onResume()

    binding.navMessages.isSelected = true
    binding.navSocial.isSelected = false
    binding.navContacts.isSelected = false
  }

  override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
    menuInflater.inflate(R.menu.main_toolbar_menu, menu)
    searchView = (menu.findItem(R.id.action_search).actionView as? SearchView)?.apply {
      queryHint = "Search..."
      setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(q: String?) = true
        override fun onQueryTextChange(q: String?) =
          viewModel.onSearchQueryChanged(q.orEmpty()).let { true }
      })
      setOnCloseListener {
        viewModel.onSearchQueryChanged("")
        false
      }
    }
    return true
  }

  private fun observeViewModel() {
    lifecycleScope.launch {
      repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {

        launch {
          viewModel.conversationState.collect { state ->
            binding.swipeRefreshLayout.isRefreshing = state.isLoading
            conversationAdapter.submitList(state.conversations)
            state.error?.let { e ->
              Toast.makeText(this@MainActivity, "Error: $e", Toast.LENGTH_LONG).show()
              if ("401" in e) logout(false)
            }
          }
        }

        launch {
          viewModel.searchState.collect { s ->
            binding.rvMessages.adapter =
              if (s.query.isBlank()) conversationAdapter else searchAdapter
            if (s.query.isNotBlank()) searchAdapter.submitList(s.results)
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
      .setPositiveButton("Chat") { dialog, _ ->
        val num = input.text.toString().trim()
        if (num.isNotEmpty()) openChat(Contact("$num@s.whatsapp.net", num, null))
        else Toast.makeText(this, "Empty number.", Toast.LENGTH_SHORT).show()
        dialog.dismiss()
      }
      .setNegativeButton("Cancel", null)
      .show()
  }

  private fun openChat(contact: Contact) {
    searchView?.takeIf { !it.isIconified }?.apply {
      setQuery("", false)
      isIconified = true
    }
    startActivity(
      Intent(this, ChatActivity::class.java).apply {
        putExtra("EXTRA_JID", contact.id)
        putExtra("EXTRA_NAME", contact.name)
        putExtra("EXTRA_AVATAR_URL", contact.avatarUrl)
      }
    )
  }

  private fun logout(callServer: Boolean) {
    startService(
      Intent(this, SyncService::class.java)
        .setAction(SyncService.ACTION_STOP)
    )
    lifecycleScope.launch {
      if (callServer) runCatching { ApiClient.getInstance(this@MainActivity).logout() }
      ApiClient.close()
      serverConfig.apply {
        saveSessionId(null)
        saveLoginState(false)
      }
      startActivity(
        Intent(this@MainActivity, LoginActivity::class.java)
          .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      )
      finish()
    }
  }
}
