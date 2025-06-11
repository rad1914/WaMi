// @path: app/src/main/java/com/radwrld/wami/MainActivity.kt

package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ConversationAdapter
import com.radwrld.wami.databinding.ActivityMainBinding
import com.radwrld.wami.model.Contact
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.ServerConfigStorage

// Note: We will represent conversations using the Contact model directly
// to avoid confusion and keep the JID readily available.
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var contactStorage: ContactStorage
    private lateinit var serverConfigStorage: ServerConfigStorage

    // Use a list of Contact objects to hold conversation data
    private val conversations = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactStorage = ContactStorage(this)
        serverConfigStorage = ServerConfigStorage(this)

        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.llContacts.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        setupRecyclerView()

        binding.fabAdd.setOnClickListener {
            AddContactDialog(this) { name, number, avatarUrl ->
                // **FIXED HERE: Create the JID and the full Contact object**
                val jid = "$number@s.whatsapp.net"
                val contact = Contact(
                    id = jid,
                    name = name,
                    phoneNumber = number,
                    avatarUrl = avatarUrl
                )
                contactStorage.addContact(contact)

                // Refresh the list from storage to ensure consistency
                loadConversations()
            }.show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload conversations every time the activity is resumed to reflect changes
        loadConversations()
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(
            conversations,
            onItemClicked = { contact ->
                // **FIXED HERE: Use the 'id' field which already is the JID**
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra("EXTRA_JID", contact.id) // The JID
                    putExtra("EXTRA_NAME", contact.name)
                })
            },
            onItemLongClicked = { contact, position ->
                confirmDelete(contact, position)
            }
        )
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }

    private fun loadConversations() {
        conversations.clear()
        // Load contacts directly from storage
        conversations.addAll(contactStorage.getContacts())
        // Tell the adapter the data has changed
        if(::conversationAdapter.isInitialized) {
            conversationAdapter.notifyDataSetChanged()
        }
    }

    private fun confirmDelete(contact: Contact, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Delete ${contact.name}?")
            .setPositiveButton("Delete") { _, _ ->
                // **FIXED HERE: We already have the full contact object to delete**
                contactStorage.deleteContact(contact)
                conversations.removeAt(position)
                conversationAdapter.notifyItemRemoved(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
