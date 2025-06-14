// @path: app/src/main/java/com/radwrld/wami/ContactsActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ContactAdapter
import com.radwrld.wami.databinding.ActivityContactsBinding
import com.radwrld.wami.model.Contact
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.Conversation
import com.radwrld.wami.network.WhatsAppApi
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactsBinding
    private lateinit var contactStorage: ContactStorage
    private lateinit var serverConfigStorage: ServerConfigStorage
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var api: WhatsAppApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactStorage = ContactStorage(this)
        serverConfigStorage = ServerConfigStorage(this)
        api = ApiClient.getInstance(this)

        setupRecyclerView()
        
        // NOTE: We load local contacts first for a quick UI update,
        // then refresh from the server in onResume.
        updateContactList(contactStorage.getContacts())

        binding.ivBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        syncContactsFromServer()
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter { contact ->
            // NOTE: The name and avatar are primarily for the contact list UI.
            // The ChatActivity will re-fetch details if needed, but passing them
            // provides a smoother immediate experience.
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_JID", contact.id)
                putExtra("EXTRA_NAME", contact.name)
                putExtra("EXTRA_AVATAR_URL", contact.avatarUrl)
            })
        }

        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            adapter = contactAdapter
        }
    }

    private fun syncContactsFromServer() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val conversations = api.getConversations()
                val newContacts = mapConversationsToContacts(conversations)
                contactStorage.saveContacts(newContacts)
                updateContactList(newContacts)

            } catch (e: Exception) {
                if (e is HttpException && e.code() == 401) {
                    Toast.makeText(this@ContactsActivity, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                    logout()
                } else {
                    Toast.makeText(this@ContactsActivity, "Could not refresh contacts: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    // MODIFIED: This function now maps all relevant fields from the Conversation object
    // to the Contact object and includes both individual and group chats.
    private fun mapConversationsToContacts(conversations: List<Conversation>): List<Contact> {
        return conversations.map { conversation ->
            val identifier = conversation.jid.substringBefore("@")
            Contact(
                id = conversation.jid,
                name = conversation.name ?: identifier,
                phoneNumber = if (conversation.jid.endsWith("@s.whatsapp.net")) identifier else conversation.jid,
                lastMessage = conversation.lastMessage,
                lastMessageTimestamp = conversation.lastMessageTimestamp,
                unreadCount = conversation.unreadCount ?: 0,
                avatarUrl = conversation.avatarUrl
            )
        }
    }

    // MODIFIED: The list is now sorted by the last message timestamp in descending order,
    // which is the standard behavior for chat applications.
    private fun updateContactList(contacts: List<Contact>) {
        val sortedContacts = contacts.sortedByDescending { it.lastMessageTimestamp }
        contactAdapter.submitList(sortedContacts)
    }

    private fun logout() {
        lifecycleScope.launch {
            // Attempt to log out from the server first.
            try { api.logout() } catch (e: Exception) { /* Ignore errors */ }

            ApiClient.close()
            serverConfigStorage.saveSessionId(null)
            serverConfigStorage.saveLoginState(false)
            contactStorage.saveContacts(emptyList()) // Clear local contacts on logout

            val intent = Intent(this@ContactsActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
