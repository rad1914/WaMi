package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private val contactsList = mutableListOf<Contact>()
    private lateinit var api: WhatsAppApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize storage and the centralized, authenticated API client
        contactStorage = ContactStorage(this)
        serverConfigStorage = ServerConfigStorage(this)
        api = ApiClient.getInstance(this)

        setupRecyclerView()
        loadLocalContacts()

        binding.ivBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        syncContactsFromServer()
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter(contactsList) { contact ->
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_JID", contact.id)
                putExtra("EXTRA_NAME", contact.name)
            })
        }

        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            adapter = contactAdapter
        }
    }

    private fun loadLocalContacts() {
        val localContacts = contactStorage.getContacts()
        if (localContacts.isNotEmpty()) {
            updateContactList(localContacts)
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
                Log.e("ContactsSync", "Failed to sync contacts from server", e)
                if (e is HttpException && e.code() == 401) {
                    // Unauthorized, token is likely invalid. Force logout.
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

    private fun mapConversationsToContacts(conversations: List<Conversation>): List<Contact> {
        return conversations.mapNotNull { conversation ->
            // Filter out group chats, which are handled in the main conversation list
            if (conversation.jid.endsWith("@s.whatsapp.net")) {
                val phone = conversation.jid.substringBefore("@")
                Contact(id = conversation.jid, name = conversation.name ?: phone, phoneNumber = phone)
            } else {
                null
            }
        }
    }

    private fun updateContactList(newContacts: List<Contact>) {
        contactsList.clear()
        contactsList.addAll(newContacts.sortedBy { it.name })
        contactAdapter.notifyDataSetChanged()
    }

    private fun logout() {
        // This function doesn't need a coroutine context for this part
        // but is kept inside one for consistency if server calls were needed.
        lifecycleScope.launch {
            // No need to call server logout as we are already unauthorized.
            // Just perform client-side cleanup.
            ApiClient.close()
            serverConfigStorage.saveSessionId(null)
            serverConfigStorage.saveLoginState(false)

            // Navigate to LoginActivity and clear the back stack
            val intent = Intent(this@ContactsActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
