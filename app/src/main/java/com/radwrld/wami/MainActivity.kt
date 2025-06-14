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

    // The current list of conversations shown in the UI
    private var currentConversations = listOf<Contact>()

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
            // This implementation is unchanged as it only affects local storage
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

    /**
     * UPDATED: This is now much more efficient. Instead of a full server sync on every
     * message, it updates the specific conversation in-place.
     */
    private fun setupSocketListeners() {
        socket?.on("whatsapp-message") { args ->
            Log.d("MainActivity", "New message received via socket.")
            val type = object : TypeToken<List<MessageHistoryItem>>() {}.type
            val messages: List<MessageHistoryItem> = gson.fromJson(args[0].toString(), type)
            
            messages.firstOrNull()?.let { incomingMsg ->
                runOnUiThread { handleIncomingMessage(incomingMsg) }
            }
        }
        // Other socket listeners remain the same...
    }

    private fun handleIncomingMessage(msg: MessageHistoryItem) {
        val updatedList = currentConversations.toMutableList()
        val existingContactIndex = updatedList.indexOfFirst { it.id == msg.jid }

        if (existingContactIndex != -1) {
            val oldContact = updatedList.removeAt(existingContactIndex)
            val updatedContact = oldContact.copy(
                lastMessage = msg.text ?: "Media", // Use a placeholder for media
                lastMessageTimestamp = msg.timestamp,
                unreadCount = oldContact.unreadCount + 1
            )
            updatedList.add(0, updatedContact) // Add to top of the list
            updateConversationList(updatedList)
        } else {
            // If the conversation is new or was hidden, a full sync is the easiest way to add it.
            Log.d("MainActivity", "Message for new/hidden conversation, performing full sync.")
            syncConversationsFromServer()
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
                    Toast.makeText(this@MainActivity, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                    logout(false)
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
                lastMessage = convo.lastMessage,
                lastMessageTimestamp = convo.lastMessageTimestamp,
                unreadCount = convo.unreadCount ?: 0
            )
        }
    }

    /**
     * UPDATED: Now filters the list and submits it to the adapter for an efficient update.
     */
    private fun updateConversationList(newConversations: List<Contact>) {
        val hiddenJids = hiddenConversationStorage.getHiddenJids()
        val visibleConversations = newConversations.filterNot { it.id in hiddenJids }
        
        // Store the current state and submit it to the adapter
        this.currentConversations = visibleConversations
        conversationAdapter.submitList(visibleConversations)
    }

    private fun confirmHide(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Hide Conversation")
            .setMessage("Hide conversation with ${contact.name}?")
            .setPositiveButton("Hide") { _, _ ->
                hiddenConversationStorage.hideConversation(contact.id)
                // Filter the list locally instead of a full server sync
                val updatedList = currentConversations.filterNot { it.id == contact.id }
                updateConversationList(updatedList)
                Toast.makeText(this, "Conversation hidden", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    // The logout function does not require changes.
    private fun logout(callServer: Boolean) {
        lifecycleScope.launch {
            if (callServer) {
                try { api.logout() } catch (e: Exception) {
                    Log.e("MainActivity", "Server logout failed", e)
                }
            }
            ApiClient.close()
            serverConfigStorage.saveSessionId(null)
            serverConfigStorage.saveLoginState(false)
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
