// @path: app/src/main/java/com/radwrld/wami/MainActivity.kt
// MainActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ConversationAdapter
import com.radwrld.wami.databinding.ActivityMainBinding
import com.radwrld.wami.model.Contact
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.ui.viewmodel.ConversationListViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var serverConfigStorage: ServerConfigStorage

    private val viewModel: ConversationListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        serverConfigStorage = ServerConfigStorage(this)
        ApiClient.initializeSocket(this)

        setupClickListeners()
        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        ApiClient.connectSocket()
        viewModel.load()
    }

    override fun onPause() {
        super.onPause()
        ApiClient.disconnectSocket()
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_contacts -> {
                    Toast.makeText(this, "Contacts clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        binding.fabAdd.setOnClickListener {
            Toast.makeText(this, "New Chat clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(
            onItemClicked = { contact ->
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra("EXTRA_JID", contact.id)
                    putExtra("EXTRA_NAME", contact.name)
                })
            },
            onItemLongClicked = { contact, _ -> confirmHide(contact) }
        )

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    // FIXED: Removed reference to binding.progressBar to prevent a crash
                    // if the ID does not exist in your activity_main.xml layout.
                    // You can add a ProgressBar with id="progressBar" to your XML to re-enable this.
                    // binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Update Conversation List
                    conversationAdapter.submitList(state.conversations)

                    // Show Error Messages
                    state.error?.let { errorMsg ->
                        Toast.makeText(this@MainActivity, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                        if (errorMsg.contains("401")) {
                           logout(false)
                        }
                    }
                }
            }
        }
    }

    private fun confirmHide(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Hide Conversation")
            .setMessage("Are you sure you want to hide the conversation with ${contact.name}?")
            .setPositiveButton("Hide") { _, _ ->
                viewModel.hide(contact.id)
                Toast.makeText(this, "Conversation hidden", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout(callServer: Boolean) {
        lifecycleScope.launch {
            if (callServer) {
                try {
                    ApiClient.getInstance(this@MainActivity).logout()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Server logout failed", e)
                }
            }

            ApiClient.close()
            serverConfigStorage.saveSessionId(null)
            serverConfigStorage.saveLoginState(false)

            val intent = Intent(this@MainActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
