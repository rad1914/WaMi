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
import com.radwrld.wami.network.Contact
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.storage.MessageStorage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding
    private val jid by lazy { intent.getStringExtra(EXTRA_JID).orEmpty() }
    private val repository by lazy { WhatsAppRepository(this) }
    private val messageStorage by lazy { MessageStorage(this) }
    private var isBlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (jid.isBlank()) {
            showToast("Error: Contacto no encontrado.")
            finish()
            return
        }
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        loadContactInfo()
        setupActions()
    }

    private fun loadContactInfo() = lifecycleScope.launch {
        val contact = repository.getCachedConversations().find { it.id == jid }
        if (contact == null) {
            showToast("No se pudieron cargar los detalles del contacto.")
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
            } ?: run { tvLastSeen.visibility = View.GONE }

            if (contact.isGroup) setupGroupUI(contact)
            else setupUserUI(contact)
        }
    }

    private fun setupUserUI(contact: Contact) = with(binding) {
        layoutPhone.visibility = if (contact.phoneNumber.isNullOrBlank()) View.GONE else View.VISIBLE
        tvPhone.text = contact.phoneNumber

        layoutLocalTime.visibility = View.VISIBLE
        btnCommonGroups.visibility = View.VISIBLE
        btnBlock.visibility = View.VISIBLE
        tvLocalTime.text = "--:--"
        tvAbout.text = "¡Hola! Estoy usando Wami."
        updateBlockButton(contact.name)
        btnReport.findTextView()?.text = "Reportar a ${contact.name}"

        lifecycleScope.launch {
            repository.getCommonGroups(contact.id).onSuccess {
                tvCommonGroupsCount.text = it.size.toString()
                btnCommonGroups.isEnabled = it.isNotEmpty()
            }.onFailure {
                tvCommonGroupsCount.text = "0"
                btnCommonGroups.isEnabled = false
            }
        }
    }

    private fun setupGroupUI(contact: Contact) = with(binding) {
        layoutPhone.visibility = View.GONE
        layoutLocalTime.visibility = View.GONE
        btnCommonGroups.visibility = View.GONE
        btnBlock.visibility = View.GONE
        btnReport.findTextView()?.text = "Reportar grupo"

        lifecycleScope.launch {
            repository.getGroupInfo(contact.id).onSuccess {
                tvAbout.text = it.desc.takeUnless { d -> d.isNullOrBlank() } ?: "Sin descripción."
            }.onFailure {
                tvAbout.text = "No se pudo cargar la descripción."
            }
        }
    }

    private fun setupActions() = with(binding) {
        btnSharedMedia.setOnClickListener {
            startActivity(Intent(this@AboutActivity, SharedMediaActivity::class.java).apply {
                putExtra(EXTRA_JID, jid)
            })
        }

        btnCommonGroups.setOnClickListener {
            val count = tvCommonGroupsCount.text.toString().toIntOrNull() ?: 0
            showToast(if (count > 0) "Función para ver grupos en común no implementada" else "No hay grupos en común")
        }

        btnBlock.setOnClickListener {
            val name = tvProfileName.text.toString()
            lifecycleScope.launch {
                val result = if (isBlocked) repository.unblockContact(jid) else repository.blockContact(jid)
                result.onSuccess {
                    isBlocked = !isBlocked
                    updateBlockButton(name)
                    showToast("$name ha sido ${if (isBlocked) "bloqueado" else "desbloqueado"}.")
                }.onFailure {
                    showToast("Error: ${it.message}")
                }
            }
        }

        btnReport.setOnClickListener {
            val name = tvProfileName.text.toString()
            lifecycleScope.launch {
                repository.reportContact(jid).onSuccess {
                    showToast("$name ha sido reportado.")
                    btnReport.isEnabled = false
                    btnReport.alpha = 0.5f
                }.onFailure {
                    showToast("Error al reportar: ${it.message}")
                }
            }
        }
    }

    private fun updateBlockButton(name: String) {
        binding.btnBlock.findTextView()?.text = if (isBlocked) "Desbloquear" else "Bloquear"
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

        return when {
            now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) ->
                "últ. vez hoy a las $time"
            now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - 1 == date.get(Calendar.DAY_OF_YEAR) ->
                "últ. vez ayer a las $time"
            else -> {
                val dateFormat = SimpleDateFormat("d MMM yy", Locale.getDefault())
                "últ. vez el ${dateFormat.format(Date(timestamp))}"
            }
        }
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_JID = "EXTRA_JID"
    }
}

private fun LinearLayout.findTextView(): TextView? =
    (0 until childCount).map { getChildAt(it) }.filterIsInstance<TextView>().firstOrNull()
