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
import com.radwrld.wami.network.Conversation // Correct model from API interface
import com.radwrld.wami.network.WhatsAppApi   // Correct API interface
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.HiddenConversationStorage
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var contactStorage: ContactStorage
    private lateinit var serverConfigStorage: ServerConfigStorage
    private lateinit var hiddenConversationStorage: HiddenConversationStorage
    private lateinit var api: WhatsAppApi // Use the API interface directly

    private val conversations = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize storage classes
        contactStorage = ContactStorage(this)
        serverConfigStorage = ServerConfigStorage(this)
        hiddenConversationStorage = HiddenConversationStorage(this)

        setupApi() // Initialize the API client
        setupClickListeners()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // Sync with the server every time the activity is resumed to get latest chats
        syncConversationsFromServer()
    }

    private fun setupApi() {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        val serverUrl = serverConfigStorage.getCurrentServer()

        api = Retrofit.Builder()
            .baseUrl("http://$serverUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WhatsAppApi::class.java)
    }

    private fun setupClickListeners() {
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.llContacts.setOnClickListener {
             startActivity(Intent(this, ContactsActivity::class.java))
        }

        binding.fabAdd.setOnClickListener {
            val addContactDialog = AddContactDialog(this) { name, number, avatarUrl ->
                val newContact = Contact(
                    id = "$number@s.radwrld.com", // Creating a placeholder JID
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
                    putExtra("EXTRA_JID", contact.id) // The JID
                    putExtra("EXTRA_NAME", contact.name)
                })
            },
            onItemLongClicked = { contact, _ ->
                confirmDelete(contact)
            }
        )
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }

    private fun syncConversationsFromServer() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // Call the correct method from the API interface
                val serverConversations = api.getConversations()
                val newConversations = mapConversationsToContacts(serverConversations)
                updateConversationList(newConversations)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch conversations", e)
                Toast.makeText(this@MainActivity, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
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
                avatarUrl = null // Explicitly null as API doesn't provide it
            )
        }
    }

    private fun updateConversationList(newConversations: List<Contact>) {
        val hiddenJids = hiddenConversationStorage.getHiddenJids()
        val visibleConversations = newConversations.filterNot { it.id in hiddenJids }

        conversations.clear()
        conversations.addAll(visibleConversations)
        if (::conversationAdapter.isInitialized) {
            conversationAdapter.notifyDataSetChanged()
        }
    }

    private fun confirmDelete(contact: Contact) {
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
}
