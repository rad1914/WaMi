// @path: app/src/main/java/com/radwrld/wami/ContactsActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ContactAdapter
import com.radwrld.wami.data.ContactRepository
import com.radwrld.wami.databinding.ActivityContactsBinding
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.ui.viewmodel.ContactsViewModel
import kotlinx.coroutines.launch

class ContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactsBinding
    private lateinit var contactAdapter: ContactAdapter
    private val viewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()

        viewModel.syncContacts()
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter { contact ->
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_JID", contact.id)
                putExtra("EXTRA_NAME", contact.name)
                putExtra("EXTRA_AVATAR_URL", contact.avatarUrl)
            })
        }
        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            adapter = contactAdapter
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    contactAdapter.submitList(state.contacts)

                    state.error?.let { errorMsg ->
                        Toast.makeText(this@ContactsActivity, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                        if (errorMsg.contains("401")) {
                           logout(false)
                        }
                    }
                }
            }
        }
    }

    private fun logout(callServer: Boolean) {
        lifecycleScope.launch {
            if (callServer) {
                try { ApiClient.getInstance(this@ContactsActivity).logout() } catch (e: Exception) {  }
            }
            ApiClient.close()

            ServerConfigStorage(this@ContactsActivity).apply {
                saveSessionId(null)
                saveLoginState(false)
            }

            val intent = Intent(this@ContactsActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
