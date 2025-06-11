// @path: app/src/main/java/com/radwrld/wami/ContactsActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ContactAdapter
import com.radwrld.wami.databinding.ActivityContactsBinding
import com.radwrld.wami.model.Contact // Ensure your Contact model is being imported
import com.radwrld.wami.storage.ContactStorage

// Renamed class to follow standard Android convention (ContactsActivity)
class ContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactsBinding
    private lateinit var contactStorage: ContactStorage
    private lateinit var contactAdapter: ContactAdapter
    private val contactsList = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactStorage = ContactStorage(this)
        setupRecyclerView()

        // Handle the back button click to return to MainActivity
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Load contacts in onResume to ensure the list is always fresh
        // if you go back from another activity.
        loadContacts()
    }

    private fun setupRecyclerView() {
        // Initialize the adapter with an empty list first.
        contactAdapter = ContactAdapter(contactsList) { contact ->
            // When a contact is clicked, open the chat screen with their info.
            val intent = Intent(this, ChatActivity::class.java).apply {
                // **THE FIX: Use the 'id' field, which is the pre-formatted JID.**
                // This ensures consistency with MainActivity and the rest of your app.
                putExtra("EXTRA_JID", contact.id)
                putExtra("EXTRA_NAME", contact.name)
            }
            startActivity(intent)
        }

        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            adapter = contactAdapter
        }
    }

    private fun loadContacts() {
        contactsList.clear()
        contactsList.addAll(contactStorage.getContacts())
        contactAdapter.notifyDataSetChanged() // Update the adapter with the new data
    }
}
