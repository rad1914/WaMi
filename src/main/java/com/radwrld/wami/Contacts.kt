// Contacts.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ContactAdapter
import com.radwrld.wami.databinding.ActivityContactsBinding
import com.radwrld.wami.storage.ContactStorage

class Contacts : AppCompatActivity() {

    private lateinit var binding: ActivityContactsBinding
    private lateinit var contactStorage: ContactStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactStorage = ContactStorage(this)
        val contacts = contactStorage.getContacts()

        // Initialize the adapter and set it on the RecyclerView
        val contactAdapter = ContactAdapter(contacts) { contact ->
            // When a contact is clicked, open the chat screen with their info
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_JID", "${contact.phoneNumber}@s.whatsapp.net")
                putExtra("EXTRA_NAME", contact.name)
            }
            startActivity(intent)
            finish() // Finish Contacts activity to return to MainActivity after chat
        }

        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(this@Contacts)
            adapter = contactAdapter
        }

        // Handle the back button click to return to MainActivity
        binding.ivBack.setOnClickListener {
            finish()
        }
    }
}
