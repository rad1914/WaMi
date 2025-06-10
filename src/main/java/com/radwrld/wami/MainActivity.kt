// app/src/main/java/com/radwrld/wami/MainActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ConversationAdapter // NOTE: Using the new ConversationAdapter
import com.radwrld.wami.databinding.ActivityMainBinding
import com.radwrld.wami.model.Chat
import com.radwrld.wami.model.Contact
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.ServerConfigStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationAdapter: ConversationAdapter // Using the new adapter
    private lateinit var contactStorage: ContactStorage
    private lateinit var serverConfigStorage: ServerConfigStorage

    private val chats = mutableListOf<Chat>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactStorage = ContactStorage(this)
        serverConfigStorage = ServerConfigStorage(this)

        Log.d("MainActivity", "Server (primary/fallback): ${serverConfigStorage.primaryServer} / ${serverConfigStorage.fallbackServer}")

        binding.header.setOnLongClickListener { showSetServersDialog(); true }

        // Clear previous list to avoid duplication on activity recreation
        chats.clear()
        chats.addAll(contactStorage.getContacts().map { contact ->
            Chat(
                contactName = contact.name,
                lastMessage = "Say hi!",
                avatarUrl = contact.avatarUrl,
                phoneNumber = contact.phoneNumber
            )
        })

        // --- APPLIED FIX: Using the new ConversationAdapter ---
        conversationAdapter = ConversationAdapter(chats) { chat ->
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_JID", "${chat.phoneNumber}@s.whatsapp.net")
                putExtra("EXTRA_NAME", chat.contactName)
            })
        }

        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = conversationAdapter

        binding.fabAdd.setOnClickListener {
            AddContactDialog(this) { name, number, avatarUrl ->
                val newContact = Contact(name, number, avatarUrl)

                // --- APPLIED FIX: This call will now work ---
                contactStorage.addContact(newContact)

                val newChat = Chat(name, "Say hi!", avatarUrl, number)
                chats.add(0, newChat)
                conversationAdapter.notifyItemInserted(0)
                binding.rvMessages.scrollToPosition(0)
            }.show()
        }
    }

    private fun showSetServersDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.padding)
            setPadding(padding, padding, padding, padding)
        }

        val inputPrimary = EditText(this).apply {
            hint = "Primary Server"
            setText(serverConfigStorage.primaryServer)
        }
        val inputFallback = EditText(this).apply {
            hint = "Fallback Server"
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
                    Log.d("MainActivity", "Saved: primary=$primary, fallback=$fallback")
                } else {
                    Log.d("MainActivity", "Both fields must be filled")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
