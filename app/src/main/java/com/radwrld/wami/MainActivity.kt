package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
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
        
        setupClickListeners()
        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // ++ Applied suggestion: Restore the call to load data. This ensures that
        //    conversations are fetched every time the activity becomes active,
        //    guaranteeing the list is populated and fresh.
        viewModel.load()
        
        ApiClient.connectSocket() 
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
                    startActivity(Intent(this, ContactsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.fabAdd.setOnClickListener {
            showFastContactDialog()
        }
    }

    private fun showFastContactDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Fast Contact")
        builder.setMessage("Enter a phone number to start a new chat.")

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            hint = "e.g., 15551234567"
        }
        builder.setView(input)

        builder.setPositiveButton("Chat") { dialog, _ ->
            val number = input.text.toString().trim()
            if (number.isNotEmpty()) {
                val jid = "$number@s.whatsapp.net"
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("EXTRA_JID", jid)
                    putExtra("EXTRA_NAME", number)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Phone number cannot be empty.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
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
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    conversationAdapter.submitList(state.conversations)

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
