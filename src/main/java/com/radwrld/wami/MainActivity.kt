package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
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

        Log.d("MainActivity", "Server (primary/fallback): ${serverConfigStorage.primaryServer} / ${serverConfigStorage.fallbackServer}")

        binding.header.setOnLongClickListener { showSetServersDialog(); true }

        binding.ivProfile.setOnClickListener {
            // TODO: Create and navigate to SettingsActivity if it exists
            // startActivity(Intent(this, SettingsActivity::class.java))
        }

        // The OnClickListener for the Contacts navigation element
        binding.llContacts.setOnClickListener {
            // This line launches the Contacts activity
            startActivity(Intent(this, Contacts::class.java))
        }

        loadConversationsFromStorage()

        conversationAdapter = ConversationAdapter(
            conversations,
            onItemClicked = { conversation ->
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra("EXTRA_JID", "${conversation.phoneNumber}@s.whatsapp.net")
                    putExtra("EXTRA_NAME", conversation.name)
                })
            },
            onItemLongClicked = { conversation, position ->
                showDeleteConfirmationDialog(conversation, position)
            }
        )

        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = conversationAdapter

        binding.fabAdd.setOnClickListener {
            // "Add Contact" functionality is now fully active
            AddContactDialog(this) { name, number, avatarUrl ->
                val newContact = Contact(name, number, avatarUrl)
                contactStorage.addContact(newContact)

                val newConversation = Message(
                    name = name,
                    lastMessage = "Say hi!",
                    avatarUrl = avatarUrl,
                    phoneNumber = number
                )
                conversations.add(0, newConversation)
                conversationAdapter.notifyItemInserted(0)
                binding.rvMessages.scrollToPosition(0)
            }.show()
        }
    }

    private fun loadConversationsFromStorage() {
        conversations.clear()
        conversations.addAll(contactStorage.getContacts().map { contact ->
            Message(
                name = contact.name,
                lastMessage = "Say hi!",
                avatarUrl = contact.avatarUrl,
                phoneNumber = contact.phoneNumber
            )
        })
    }

    private fun showDeleteConfirmationDialog(conversation: Message, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${conversation.name} from your contacts?")
            .setPositiveButton("Delete") { _, _ ->
                val contactToDelete = Contact(
                    name = conversation.name,
                    phoneNumber = conversation.phoneNumber,
                    avatarUrl = conversation.avatarUrl
                )
                contactStorage.deleteContact(contactToDelete)
                conversations.removeAt(position)
                conversationAdapter.notifyItemRemoved(position)
                conversationAdapter.notifyItemRangeChanged(position, conversations.size)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSetServersDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.padding_standard) // Assumes you have a dimen resource
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
