// app/src/main/java/com/radwrld/wami/MainActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.databinding.ActivityMainBinding
import com.radwrld.wami.model.Contact
import com.radwrld.wami.model.Message          // <-- Import Message class
import com.radwrld.wami.adapter.MessageAdapter // <-- Import MessageAdapter
import com.radwrld.wami.storage.ContactStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var contactStorage: ContactStorage

    private val messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactStorage = ContactStorage(this)

        // Load saved contacts
        val savedContacts = contactStorage.getContacts()

        // Display saved contacts in RecyclerView
        val savedMessages = savedContacts.map { contact ->
            Message(
                name = contact.name,
                lastMessage = "Say hi!",
                avatarUrl = contact.avatarUrl,
                phoneNumber = contact.phoneNumber,
                isOnline = false
            )
        }

        messages.addAll(savedMessages)

        adapter = MessageAdapter(messages) { msg ->
            val jid = "${msg.phoneNumber}@s.whatsapp.net"
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_JID", jid)
                putExtra("EXTRA_NAME", msg.name)
            })
        }

        with(binding.rvMessages) {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        // FAB to add a new contact
        binding.fabAdd.setOnClickListener {
            AddContactDialog(this) { name, number, avatarUrl ->
                val newContact = Contact(name, number, avatarUrl)

                // Save contact to SharedPreferences
                val currentContacts = contactStorage.getContacts().toMutableList()
                currentContacts.add(newContact)
                contactStorage.saveContacts(currentContacts)

                // Add to message list
                val newMessage = Message(
                    name = name,
                    lastMessage = "Say hi!",
                    avatarUrl = avatarUrl,
                    phoneNumber = number,
                    isOnline = false
                )
                messages.add(0, newMessage)
                adapter.notifyItemInserted(0)
                binding.rvMessages.scrollToPosition(0)
            }.show()
        }
    }
}
