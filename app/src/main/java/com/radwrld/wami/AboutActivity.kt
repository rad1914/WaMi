// @path: app/src/main/java/com/radwrld/wami/AboutActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.radwrld.wami.databinding.ActivityAboutBinding
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding
    private val jid by lazy { intent.getStringExtra(EXTRA_JID).orEmpty() }

    private lateinit var repository: WhatsAppRepository
    private lateinit var messageStorage: MessageStorage

    private var isBlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (jid.isBlank()) {
            Toast.makeText(this, "Error: Contacto no encontrado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WhatsAppRepository(this)
        messageStorage = MessageStorage(this)

        setupToolbar()
        loadAndDisplayContactInfo()
        setupActionButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadAndDisplayContactInfo() {
        lifecycleScope.launch {
            val contact = repository.getCachedConversations().find { it.id == jid }
            if (contact == null) {
                Toast.makeText(this@AboutActivity, "No se pudieron cargar los detalles del contacto.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val mediaCount = messageStorage.getMessages(jid).count { it.hasMedia() }
            
            with(binding) {
                tvProfileName.text = contact.name
                toolbar.title = contact.name
                tvMediaCount.text = mediaCount.toString()

                Glide.with(this@AboutActivity)
                    .load(contact.avatarUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.profile_picture_placeholder)
                    .error(R.drawable.profile_picture_placeholder)
                    .into(profileImage)

                contact.lastMessageTimestamp?.let {
                    tvLastSeen.text = formatTimestamp(it)
                    tvLastSeen.visibility = View.VISIBLE
                } ?: run {
                    tvLastSeen.visibility = View.GONE
                }

                if (contact.isGroup) {
                    setupGroupUI(contact)
                } else {
                    setupUserUI(contact)
                }
            }
        }
    }

    private fun setupUserUI(contact: com.radwrld.wami.network.Contact) {
        with(binding) {

            layoutPhone.visibility = if (!contact.phoneNumber.isNullOrBlank()) View.VISIBLE else View.GONE
            tvPhone.text = contact.phoneNumber

            btnCommonGroups.visibility = View.VISIBLE
            layoutLocalTime.visibility = View.VISIBLE
            btnBlock.visibility = View.VISIBLE
            
            tvAbout.text = "¡Hola! Estoy usando Wami."
            tvLocalTime.text = "--:--"
            updateBlockButtonText(contact.name)
            btnReport.findTextView()?.text = "Reportar a ${contact.name}"

            lifecycleScope.launch {
                repository.getCommonGroups(contact.id)
                    .onSuccess { commonGroups ->
                        tvCommonGroupsCount.text = commonGroups.size.toString()
                        btnCommonGroups.isEnabled = commonGroups.isNotEmpty()
                    }
                    .onFailure {
                        tvCommonGroupsCount.text = "0"
                        btnCommonGroups.isEnabled = false
                    }
            }
        }
    }

    private fun setupGroupUI(contact: com.radwrld.wami.network.Contact) {
         with(binding) {

            layoutPhone.visibility = View.GONE

            btnCommonGroups.visibility = View.GONE
            layoutLocalTime.visibility = View.GONE
            btnBlock.visibility = View.GONE

            btnReport.findTextView()?.text = "Reportar grupo"

             lifecycleScope.launch {
                 repository.getGroupInfo(contact.id)
                     .onSuccess { groupInfo ->
                         tvAbout.text = if (!groupInfo.desc.isNullOrBlank()) groupInfo.desc else "Sin descripción."
                     }
                     .onFailure {
                         tvAbout.text = "No se pudo cargar la descripción."
                     }
             }
        }
    }

    private fun setupActionButtons() {
        binding.btnSharedMedia.setOnClickListener {
            val intent = Intent(this, SharedMediaActivity::class.java).apply {
                putExtra(EXTRA_JID, jid)
            }
            startActivity(intent)
        }

        binding.btnCommonGroups.setOnClickListener {
            val count = binding.tvCommonGroupsCount.text.toString().toIntOrNull() ?: 0
            if (count > 0) {
                Toast.makeText(this, "Función para ver grupos en común no implementada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No hay grupos en común", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBlock.setOnClickListener {
            val contactName = binding.tvProfileName.text.toString()
            lifecycleScope.launch {
                val result = if (isBlocked) repository.unblockContact(jid) else repository.blockContact(jid)

                result.onSuccess {
                    isBlocked = !isBlocked
                    updateBlockButtonText(contactName)
                    val actionText = if(isBlocked) "bloqueado" else "desbloqueado"
                    Toast.makeText(this@AboutActivity, "$contactName ha sido $actionText.", Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    Toast.makeText(this@AboutActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.btnReport.setOnClickListener {
            val contactName = binding.tvProfileName.text.toString()
            lifecycleScope.launch {
                repository.reportContact(jid)
                    .onSuccess {
                        Toast.makeText(this@AboutActivity, "$contactName ha sido reportado.", Toast.LENGTH_LONG).show()
                        binding.btnReport.isEnabled = false
                        binding.btnReport.alpha = 0.5f
                    }
                    .onFailure { e ->
                        Toast.makeText(this@AboutActivity, "Error al reportar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
    
    private fun updateBlockButtonText(name: String) {
        binding.btnBlock.findTextView()?.text = if (isBlocked) "Desbloquear" else "Bloquear"
    }

    private fun formatTimestamp(timestamp: Long): String {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        return when {
            now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR) ->
                "últ. vez hoy a las ${timeFormat.format(Date(timestamp))}"
            now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) - 1 == messageDate.get(Calendar.DAY_OF_YEAR) ->
                "últ. vez ayer a las ${timeFormat.format(Date(timestamp))}"
            else -> {
                val dateFormat = SimpleDateFormat("d MMM yy", Locale.getDefault())
                "últ. vez el ${dateFormat.format(Date(timestamp))}"
            }
        }
    }

    companion object {
        const val EXTRA_JID = "EXTRA_JID"
    }
}

private fun LinearLayout.findTextView(): TextView? {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is TextView) {
            return child
        }
    }
    return null
}
