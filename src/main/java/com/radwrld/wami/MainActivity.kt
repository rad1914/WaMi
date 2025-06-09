// app/src/main/java/com/radwrld/wami/MainActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.MessageAdapter
import com.radwrld.wami.databinding.ActivityMainBinding
import com.radwrld.wami.model.Contact
import com.radwrld.wami.model.Message
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.ServerConfigStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var contactStorage: ContactStorage
    private lateinit var serverConfigStorage: ServerConfigStorage

    private val messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactStorage = ContactStorage(this)
        serverConfigStorage = ServerConfigStorage(this)

        // Show both primary and fallback on startup
        val primary = serverConfigStorage.primaryServer
        val fallback = serverConfigStorage.fallbackServer
        Toast.makeText(
            this,
            "Server (primary/fallback): $primary / $fallback",
            Toast.LENGTH_LONG
        ).show()

        // Long-press header to open dialog to set primary & fallback
        binding.header.setOnLongClickListener {
            showSetServersDialog()
            true
        }

        // Load saved contacts into messages list
        val savedContacts = contactStorage.getContacts()
        messages.addAll(savedContacts.map { contact ->
            Message(
                name = contact.name,
                lastMessage = "Say hi!",
                avatarUrl = contact.avatarUrl,
                phoneNumber = contact.phoneNumber,
                isOnline = false
            )
        })

        // Set up RecyclerView adapter
        adapter = MessageAdapter(messages) { msg ->
            val jid = "${msg.phoneNumber}@s.whatsapp.net"
            startActivity(
                Intent(this, ChatActivity::class.java).apply {
                    putExtra("EXTRA_JID", jid)
                    putExtra("EXTRA_NAME", msg.name)
                }
            )
        }
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        // FAB to add a new contact
        binding.fabAdd.setOnClickListener {
            AddContactDialog(this) { name, number, avatarUrl ->
                val newContact = Contact(name, number, avatarUrl)
                val currentContacts = contactStorage.getContacts().toMutableList()
                currentContacts.add(newContact)
                contactStorage.saveContacts(currentContacts)

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

    /**
     * Show an AlertDialog allowing the user to enter and save primary and fallback servers.
     */
    private fun showSetServersDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.padding)
            setPadding(padding, padding, padding, padding)
        }

        val inputPrimary = EditText(this).apply {
            hint = "Primary Server (e.g., ip:port)"
            setText(serverConfigStorage.primaryServer)
        }
        val inputFallback = EditText(this).apply {
            hint = "Fallback Server (e.g., ip:port)"
            setText(serverConfigStorage.fallbackServer)
        }
        layout.addView(inputPrimary)
        layout.addView(inputFallback)

        AlertDialog.Builder(this)
            .setTitle("Set WAALT Servers")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val primary = inputPrimary.text.toString().trim()
                val fallback = inputFallback.text.toString().trim()
                if (primary.isNotEmpty() && fallback.isNotEmpty()) {
                    serverConfigStorage.saveServers(primary, fallback)
                    Toast.makeText(
                        this,
                        "Saved: primary=$primary, fallback=$fallback",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Both fields must be filled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
