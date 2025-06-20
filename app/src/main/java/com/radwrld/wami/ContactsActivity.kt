// @path: app/src/main/java/com/radwrld/wami/ContactsActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
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
        updateContactList(contactStorage.getContacts())
    }

    override fun onResume() {
        super.onResume()
        syncContactsFromServer()
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter { contact ->
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
            }
        }
    }

    private fun mapConversationsToContacts(conversations: List<Conversation>): List<Contact> {
        val serverUrl = serverConfigStorage.getCurrentServer().removeSuffix("/")
        return conversations.map { conversation ->
            Contact(
                id = conversation.jid,
                name = conversation.name ?: conversation.jid.split('@').first(),
                isGroup = conversation.isGroup,
                phoneNumber = if (conversation.isGroup) null else conversation.jid.split('@').first(),
                lastMessage = conversation.lastMessage,
                lastMessageTimestamp = conversation.lastMessageTimestamp,
                unreadCount = conversation.unreadCount ?: 0,
                avatarUrl = "$serverUrl/avatar/${conversation.jid}"
            )
        }
    }
    
    private fun updateContactList(contacts: List<Contact>) {
        val (groups, individuals) = contacts.partition { it.isGroup }
        val sortedIndividuals = individuals.sortedBy { it.name.lowercase() }
        val sortedGroups = groups.sortedBy { it.name.lowercase() }
        contactAdapter.submitList(sortedIndividuals + sortedGroups)
    }

    private fun logout() {
        lifecycleScope.launch {
            try { api.logout() } catch (e: Exception) { /* Ignore errors */ }
            ApiClient.close()
            serverConfigStorage.saveSessionId(null)
            serverConfigStorage.saveLoginState(false)
            contactStorage.saveContacts(emptyList())

            val intent = Intent(this@ContactsActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
