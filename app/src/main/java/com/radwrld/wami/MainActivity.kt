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
import com.radwrld.wami.model.Chat
import com.radwrld.wami.model.Contact
import com.radwrld.wami.network.RetrofitClient
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.HiddenConversationStorage
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var contactStorage: ContactStorage
    private lateinit var serverConfigStorage: ServerConfigStorage
    private lateinit var hiddenConversationStorage: HiddenConversationStorage

    private val conversations = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize storage classes
        contactStorage = ContactStorage(this)
        serverConfigStorage = ServerConfigStorage(this)
        hiddenConversationStorage = HiddenConversationStorage(this)

        setupClickListeners()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // Sync with the server every time the activity is resumed to get latest chats
        syncConversationsFromServer()
    }

    private fun setupClickListeners() {
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.llContacts.setOnClickListener {
             startActivity(Intent(this, ContactsActivity::class.java))
        }

        // The FAB should open the AddContactDialog to add a new contact
        binding.fabAdd.setOnClickListener {
            val addContactDialog = AddContactDialog(this) { name, number, avatarUrl ->
                // A new contact is created from the dialog input.
                // NOTE: The implementation assumes `contactStorage.addContact` exists
                // to persist the new contact.
                val newContact = Contact(
                    id = "$number@s.radwrld.com", // Creating a placeholder JID
                    name = name,
                    phoneNumber = number,
                    avatarUrl = avatarUrl.ifEmpty { null }
                )
                // Assuming a method in ContactStorage to save the contact
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
                    putExtra("EXTRA_JID", contact.id) // The JID
                    putExtra("EXTRA_NAME", contact.name)
                })
            },
            onItemLongClicked = { contact, _ -> // Position is no longer needed for direct removal
                confirmDelete(contact)
            }
        )
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }

    /**
     * Fetches the list of active chats from the server and updates the UI.
     */
    private fun syncConversationsFromServer() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getChats()
                if (response.isSuccessful && response.body() != null) {
                    val serverChats = response.body()!!
                    val newConversations = mapChatsToContacts(serverChats)
                    updateConversationList(newConversations)
                } else {
                    Toast.makeText(this@MainActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch conversations", e)
                Toast.makeText(this@MainActivity, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Maps the server Chat response to the Contact model used by the UI.
     */
    private fun mapChatsToContacts(chats: List<Chat>): List<Contact> {
        return chats.map { chat ->
            val phoneNumber = chat.jid.split('@').firstOrNull() ?: "Unknown"
            Contact(
                id = chat.jid,
                name = chat.name ?: phoneNumber,
                phoneNumber = phoneNumber,
                avatarUrl = null // Explicitly null as API doesn't provide it
            )
        }
    }

    /**
     * Safely updates the RecyclerView's data list, filtering out hidden conversations.
     */
    private fun updateConversationList(newConversations: List<Contact>) {
        val hiddenJids = hiddenConversationStorage.getHiddenJids()
        val visibleConversations = newConversations.filterNot { it.id in hiddenJids }

        conversations.clear()
        conversations.addAll(visibleConversations)
        if (::conversationAdapter.isInitialized) {
            conversationAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Shows a confirmation dialog and hides the conversation persistently if confirmed.
     */
    private fun confirmDelete(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Hide Conversation")
            .setMessage("Hide conversation with ${contact.name}?\nIt will reappear if a new message is received.")
            .setPositiveButton("Hide") { _, _ ->
                // Persistently hide the conversation
                hiddenConversationStorage.hideConversation(contact.id)
                // Re-sync from the server to apply the filter
                syncConversationsFromServer()
                Toast.makeText(this, "Conversation hidden", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
