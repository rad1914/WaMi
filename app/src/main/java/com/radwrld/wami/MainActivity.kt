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
import com.radwrld.wami.adapter.ConversationAdapter
import com.radwrld.wami.databinding.ActivityMainBinding
import com.radwrld.wami.model.Contact
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.Conversation
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

    private val conversations = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize storage and API client
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
        ApiClient.connectSocket() // Connect socket for real-time updates
        syncConversationsFromServer() // Sync on resume to catch up
    }

    override fun onPause() {
        super.onPause()
        ApiClient.disconnectSocket() // Disconnect to save resources
    }

    private fun setupClickListeners() {
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Add logout functionality on long-press
        binding.ivProfile.setOnLongClickListener {
            confirmLogout()
            true // Consume the event
        }

        binding.llContacts.setOnClickListener {
             startActivity(Intent(this, ContactsActivity::class.java))
        }

        binding.fabAdd.setOnClickListener {
            val addContactDialog = AddContactDialog(this) { name, number, avatarUrl ->
                val newContact = Contact(
                    id = "$number@s.radwrld.com",
                    name = name,
                    phoneNumber = number,
                    avatarUrl = avatarUrl.ifEmpty { null }
                )
                contactStorage.addContact(newContact)
                Toast.makeText(this, "Contact '$name' added", Toast.LENGTH_SHORT).show()
            }
            addContactDialog.show()
        }
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(
            conversations,
            onItemClicked = { contact ->
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra("EXTRA_JID", contact.id)
                    putExtra("EXTRA_NAME", contact.name)
                })
            },
            onItemLongClicked = { contact, _ ->
                confirmHide(contact)
            }
        )
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }
    
    private fun setupSocketListeners() {
        socket?.on(Socket.EVENT_CONNECT) {
            runOnUiThread { Log.d("MainActivity", "Socket connected!") }
        }
        socket?.on("whatsapp-message") {
            // A new message arrived, refresh the conversation list
            runOnUiThread {
                Log.d("MainActivity", "New message received via socket, refreshing list.")
                Toast.makeText(this, "New message received", Toast.LENGTH_SHORT).show()
                syncConversationsFromServer()
            }
        }
        socket?.on(Socket.EVENT_DISCONNECT) {
             runOnUiThread { Log.d("MainActivity", "Socket disconnected.") }
        }
    }

    private fun syncConversationsFromServer() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val serverConversations = api.getConversations()
                val newConversations = mapConversationsToContacts(serverConversations)
                updateConversationList(newConversations)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch conversations", e)
                if (e is HttpException && e.code() == 401) {
                    // Unauthorized, token is likely invalid. Force logout.
                    Toast.makeText(this@MainActivity, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                    logout(false) // Don't call server logout as we're already unauthorized
                } else {
                    Toast.makeText(this@MainActivity, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun mapConversationsToContacts(serverConvos: List<Conversation>): List<Contact> {
        return serverConvos.map { convo ->
            val phoneNumber = convo.jid.split('@').firstOrNull() ?: "Unknown"
            Contact(
                id = convo.jid,
                name = convo.name ?: phoneNumber,
                phoneNumber = phoneNumber,
                avatarUrl = null,
                lastMessage = convo.lastMessage,
                lastMessageTimestamp = convo.lastMessageTimestamp,
                unreadCount = convo.unreadCount ?: 0
            )
        }
    }

    private fun updateConversationList(newConversations: List<Contact>) {
        val hiddenJids = hiddenConversationStorage.getHiddenJids()
        val visibleConversations = newConversations.filterNot { it.id in hiddenJids }

        conversations.clear()
        conversations.addAll(visibleConversations)
        conversationAdapter.notifyDataSetChanged()
    }

    private fun confirmHide(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Hide Conversation")
            .setMessage("Hide conversation with ${contact.name}?\nIt will reappear if a new message is received.")
            .setPositiveButton("Hide") { _, _ ->
                hiddenConversationStorage.hideConversation(contact.id)
                syncConversationsFromServer()
                Toast.makeText(this, "Conversation hidden", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Logout") { _, _ ->
                logout(true) // Perform full logout
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
                    Log.e("MainActivity", "Server logout failed, proceeding with client-side cleanup", e)
                }
            }
            
            // Client-side cleanup
            ApiClient.close()
            serverConfigStorage.saveSessionId(null)
            serverConfigStorage.saveLoginState(false)
            
            // Navigate to LoginActivity
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
