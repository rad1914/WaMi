// MainActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ConversationAdapter
import com.radwrld.wami.databinding.ActivityMainBinding
import com.radwrld.wami.model.Contact
import com.radwrld.wami.model.Message
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.ServerConfigStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var contactStorage: ContactStorage
    private lateinit var serverConfigStorage: ServerConfigStorage

    private val conversations = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactStorage = ContactStorage(this)
        serverConfigStorage = ServerConfigStorage(this)

        binding.header.setOnLongClickListener {
            showSetServersDialog()
            true
        }

        binding.llContacts.setOnClickListener {
            startActivity(Intent(this, Contacts::class.java))
        }

        setupRecyclerView()
        loadConversations()

        binding.fabAdd.setOnClickListener {
            AddContactDialog(this) { name, number, avatarUrl ->
                val contact = Contact(name, number, avatarUrl)
                contactStorage.addContact(contact)

                val message = Message(name, "Say hi!", avatarUrl, number)
                conversations.add(0, message)
                conversationAdapter.notifyItemInserted(0)
                binding.rvMessages.scrollToPosition(0)
            }.show()
        }
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(
            conversations,
            onItemClicked = {
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra("EXTRA_JID", "${it.phoneNumber}@s.whatsapp.net")
                    putExtra("EXTRA_NAME", it.name)
                })
            },
            onItemLongClicked = { conversation, position ->
                confirmDelete(conversation, position)
            }
        )
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }

    private fun loadConversations() {
        conversations.clear()
        conversations.addAll(contactStorage.getContacts().map {
            Message(it.name, "Say hi!", it.avatarUrl, it.phoneNumber)
        })
    }

    private fun confirmDelete(conversation: Message, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Delete ${conversation.name}?")
            .setPositiveButton("Delete") { _, _ ->
                contactStorage.deleteContact(Contact(conversation.name, conversation.phoneNumber, conversation.avatarUrl))
                conversations.removeAt(position)
                conversationAdapter.notifyItemRemoved(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    }
