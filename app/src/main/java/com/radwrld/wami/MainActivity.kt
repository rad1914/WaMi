// @path: app/src/main/java/com/radwrld/wami/MainActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.adapter.ConversationAdapter
import com.radwrld.wami.databinding.ActivityMainBinding
import com.radwrld.wami.model.Contact
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.Conversation
import com.radwrld.wami.network.MessageHistoryItem
import com.radwrld.wami.network.WhatsAppApi
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.HiddenConversationStorage
import com.radwrld.wami.storage.ServerConfigStorage
import io.socket.client.Socket
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var contactStorage: ContactStorage
    private lateinit var serverConfigStorage: ServerConfigStorage
    private lateinit var hiddenConversationStorage: HiddenConversationStorage
    private lateinit var api: WhatsAppApi
    private var socket: Socket? = null
    private val gson = Gson()
    private var currentConversations = emptyList<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactStorage = ContactStorage(this)
        serverConfigStorage = ServerConfigStorage(this)
        hiddenConversationStorage = HiddenConversationStorage(this)
        api = ApiClient.getInstance(this)

        ApiClient.initializeSocket(this)
        socket = ApiClient.getSocket()

        setupClickListeners()
        setupRecyclerView()
        setupSocketListeners()
    }

    override fun onResume() {
        super.onResume()
        ApiClient.connectSocket()
        syncConversationsFromServer()
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
            // Placeholder for FAB click action
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

    private fun setupSocketListeners() {
        socket?.off("whatsapp-message") // Prevent duplicate listener
        socket?.on("whatsapp-message") { args ->
            val payload = args.firstOrNull()?.toString() ?: return@on
            Log.d("MainActivity", "New message received via socket.")

            val type = object : TypeToken<List<MessageHistoryItem>>() {}.type
            try {
                val messages: List<MessageHistoryItem> = gson.fromJson(payload, type)
                messages.firstOrNull()?.let { msg ->
                    runOnUiThread { handleIncomingMessage(msg) }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to parse incoming message", e)
            }
        }
    }

    private fun handleIncomingMessage(msg: MessageHistoryItem) {
        val updatedList = currentConversations.toMutableList()
        val index = updatedList.indexOfFirst { it.id == msg.jid }

        if (index != -1) {
            val old = updatedList.removeAt(index)
            val updated = old.copy(
                lastMessage = msg.text ?: "Media",
                lastMessageTimestamp = msg.timestamp,
                unreadCount = old.unreadCount + 1
            )
            updatedList.add(0, updated)
            updateConversationList(updatedList)
        } else {
            Log.d("MainActivity", "New contact or hidden conversation, syncing.")
            syncConversationsFromServer()
        }
    }

    private fun syncConversationsFromServer() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val serverConversations = api.getConversations()
                val mapped = mapConversationsToContacts(serverConversations)
                updateConversationList(mapped)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to sync", e)
                when (e) {
                    is HttpException -> {
                        if (e.code() == 401) {
                            Toast.makeText(
                                this@MainActivity,
                                "Session expired. Please log in again.",
                                Toast.LENGTH_LONG
                            ).show()
                            logout(false)
                        } else {
                            Toast.makeText(this@MainActivity, "HTTP error: ${e.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {
                        Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun mapConversationsToContacts(serverConvos: List<Conversation>): List<Contact> {
        return serverConvos.map { convo ->
            val phone = convo.jid.substringBefore('@', "Unknown")
            Contact(
                id = convo.jid,
                name = convo.name ?: phone,
                phoneNumber = phone,
                lastMessage = convo.lastMessage,
                lastMessageTimestamp = convo.lastMessageTimestamp,
                unreadCount = convo.unreadCount ?: 0
            )
        }
    }

    private fun updateConversationList(newConversations: List<Contact>) {
        val hidden = hiddenConversationStorage.getHiddenJids()
        val visible = newConversations.filterNot { it.id in hidden }

        currentConversations = visible
        conversationAdapter.submitList(visible)
    }

    private fun confirmHide(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Hide Conversation")
            .setMessage("Hide conversation with ${contact.name}?")
            .setPositiveButton("Hide") { _, _ ->
                hiddenConversationStorage.hideConversation(contact.id)
                updateConversationList(currentConversations.filterNot { it.id == contact.id })
                Toast.makeText(this, "Conversation hidden", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout(callServer: Boolean) {
        lifecycleScope.launch {
            if (callServer) {
                try {
                    api.logout()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Logout failed", e)
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
