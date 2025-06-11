// @path: app/src/main/java/com/radwrld/wami/ContactsActivity.kt
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
import com.radwrld.wami.network.Conversation
import com.radwrld.wami.network.WhatsAppApi
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

        contactStorage = ContactStorage(this)
        serverConfigStorage = ServerConfigStorage(this)

        setupApi()
        setupRecyclerView()
        loadLocalContacts()

        binding.ivBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        syncContactsFromServer()
    }

    private fun setupApi() {
        val serverUrl = serverConfigStorage.getCurrentServer()
        if (serverUrl.isBlank()) {
            // Handle case where server URL is not set
            Toast.makeText(this, "Server not configured", Toast.LENGTH_LONG).show()
            return
        }

        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()

        api = Retrofit.Builder()
            .baseUrl("http://$serverUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WhatsAppApi::class.java)
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
        // Ensure API is initialized before trying to use it
        if (!::api.isInitialized) {
            Log.w("ContactsSync", "API not initialized, skipping sync.")
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // Directly call the suspend function. Retrofit handles the background thread.
                // The result is the body itself, or an exception is thrown on failure.
                val conversations = api.getConversations()

                val newContacts = mapConversationsToContacts(conversations)
                contactStorage.saveContacts(newContacts)
                updateContactList(newContacts)

            } catch (e: Exception) {
                Log.e("ContactsSync", "Failed to sync contacts from server", e)
                Toast.makeText(this@ContactsActivity, "Could not refresh contacts", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun mapConversationsToContacts(conversations: List<Conversation>): List<Contact> {
        return conversations.mapNotNull { conversation ->
            // Filter out group chats, which may not have a simple name or phone number
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
}
