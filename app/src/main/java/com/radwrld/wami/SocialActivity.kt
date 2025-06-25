// @path: app/src/main/java/com/radwrld/wami/SocialActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.radwrld.wami.databinding.ActivitySocialBinding
import com.radwrld.wami.databinding.ItemStatusCardBinding
import com.radwrld.wami.network.StatusItem
import com.radwrld.wami.repository.WhatsAppRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel Factory to inject the repository
class SocialViewModelFactory(private val repository: WhatsAppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SocialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SocialViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ViewModel to manage UI data
class SocialViewModel(private val repository: WhatsAppRepository) : ViewModel() {
    private val _statuses = MutableStateFlow<List<StatusItem>>(emptyList())
    val statuses: StateFlow<List<StatusItem>> = _statuses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchStatuses() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getStatuses()
                .onSuccess { _statuses.value = it }
                .onFailure { /* Handle error, e.g., show a toast */ }
            _isLoading.value = false
        }
    }
}

class SocialActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySocialBinding
    private val viewModel: SocialViewModel by viewModels {
        SocialViewModelFactory(WhatsAppRepository(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupClickListeners()
        observeViewModel()

        viewModel.fetchStatuses()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.statuses.collect { statuses ->
                    populateStatuses(statuses)
                }
            }
        }
    }

    private fun populateStatuses(statuses: List<StatusItem>) {
        binding.statusContainer.removeAllViews() // Clear old views
        statuses.forEach { status ->
            val statusBinding = ItemStatusCardBinding.inflate(LayoutInflater.from(this), binding.statusContainer, false)

            statusBinding.statusUserName.text = status.senderName
            Glide.with(this)
                .load(status.mediaUrl)
                .placeholder(R.drawable.ic_media_placeholder)
                .into(statusBinding.statusImage)

            statusBinding.root.setOnClickListener {
                status.mediaUrl?.let { url ->
                    val intent = Intent(this, MediaViewActivity::class.java).apply {
                        data = Uri.parse(url)
                        type = status.mimetype ?: "image/*" // Default to image if mimetype is null
                    }
                    startActivity(intent)
                } ?: showToast("No se puede ver el estado")
            }

            binding.statusContainer.addView(statusBinding.root)
        }
    }

    private fun setupClickListeners() {
        binding.itemAddStatus.setOnClickListener {
            // TODO: Launch image/video picker to add a new status
            showToast("Añadir nuevo estado")
        }

        binding.fabNewChat.setOnClickListener {
            showToast("Iniciar un nuevo mensaje")
        }

        binding.itemNewContact.setOnClickListener {
            showToast("Abrir nuevo contacto")
        }

        binding.itemNewGroup.setOnClickListener {
            showToast("Crear nuevo grupo")
        }

        binding.itemNewCommunity.setOnClickListener {
            showToast("Crear nueva comunidad")
        }

        binding.itemCreateChannel.setOnClickListener {
            showToast("Crear un canal")
        }

        binding.itemFindChannels.setOnClickListener {
            showToast("Buscar canales")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
