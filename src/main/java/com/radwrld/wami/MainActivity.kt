// app/src/main/java/com/radwrld/wami/MainActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
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

        Log.d("MainActivity", "Server (primary/fallback): ${serverConfigStorage.primaryServer} / ${serverConfigStorage.fallbackServer}")

        binding.header.setOnLongClickListener { showSetServersDialog() ; true }

        messages.addAll(contactStorage.getContacts().map {
            Message(it.name, "Say hi!", it.avatarUrl, it.phoneNumber, "false")
        })

        adapter = MessageAdapter(messages) { msg ->
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_JID", "${msg.phoneNumber}@s.whatsapp.net")
                putExtra("EXTRA_NAME", msg.name)
            })
        }

        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        binding.fabAdd.setOnClickListener {
            AddContactDialog(this) { name, number, avatarUrl ->
                val newContact = Contact(name, number, avatarUrl)
                contactStorage.saveContacts(contactStorage.getContacts() + newContact)
                messages.add(0, Message(name, "Say hi!", avatarUrl, number, "false"))
                adapter.notifyItemInserted(0)
                binding.rvMessages.scrollToPosition(0)
            }.show()
        }
    }

    private fun showSetServersDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                resources.getDimensionPixelSize(R.dimen.padding),
                resources.getDimensionPixelSize(R.dimen.padding),
                resources.getDimensionPixelSize(R.dimen.padding),
                resources.getDimensionPixelSize(R.dimen.padding)
            )
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

        androidx.appcompat.app.AlertDialog.Builder(this)
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
