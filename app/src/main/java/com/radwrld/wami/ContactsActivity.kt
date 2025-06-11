// @path: app/src/main/java/com/radwrld/wami/ContactsActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ContactAdapter
import com.radwrld.wami.databinding.ActivityContactsBinding
import com.radwrld.wami.model.Chat
import com.radwrld.wami.model.Contact
import com.radwrld.wami.network.ApiService
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactsBinding
    private lateinit var contactStorage: ContactStorage
    private lateinit var serverConfigStorage: ServerConfigStorage
    private lateinit var contactAdapter: ContactAdapter
    private val contactsList = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactStorage = ContactStorage(this)
        serverConfigStorage = ServerConfigStorage(this)

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
        updateContactList(contactStorage.getContacts())
    }

    private fun syncContactsFromServer() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("http://${serverConfigStorage.getCurrentServer()}/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val response = retrofit.create(ApiService::class.java).getChats()

                if (response.isSuccessful && response.body() != null) {
                    val newContacts = mapChatsToContacts(response.body()!!)
                    contactStorage.saveContacts(newContacts)
                    updateContactList(newContacts)
                }
            } catch (_: Exception) {
                // Log or handle as needed
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun mapChatsToContacts(chats: List<Chat>): List<Contact> {
        return chats.map {
            val phone = it.jid.substringBefore("@")
            Contact(id = it.jid, name = it.name ?: phone, phoneNumber = phone)
        }
    }

    private fun updateContactList(newContacts: List<Contact>) {
        contactsList.apply {
            clear()
            addAll(newContacts)
        }
        contactAdapter.notifyDataSetChanged()
    }
}
