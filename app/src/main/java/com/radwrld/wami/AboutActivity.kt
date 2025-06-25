// @path: app/src/main/java/com/radwrld/wami/AboutActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.radwrld.wami.databinding.ActivityAboutBinding
import com.radwrld.wami.storage.ContactStorage
import com.radwrld.wami.storage.ConversationStorage
import com.radwrld.wami.storage.MessageStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding
    private val jid by lazy { intent.getStringExtra(EXTRA_JID).orEmpty() }

    private lateinit var contactStorage: ContactStorage
    private lateinit var conversationStorage: ConversationStorage
    private lateinit var messageStorage: MessageStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (jid.isBlank()) {
            Toast.makeText(this, "Error: Contact not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactStorage = ContactStorage(this)
        conversationStorage = ConversationStorage(this)
        messageStorage = MessageStorage(this)

        setupToolbar()
        loadAndDisplayContactInfo()
        setupActionButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadAndDisplayContactInfo() {
        val contact = contactStorage.getContacts().find { it.id == jid }
        if (contact == null) {
            Toast.makeText(this, "Could not load contact details.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val conversation = conversationStorage.getConversations().find { it.id == jid }
        val mediaCount = messageStorage.getMessages(jid).count { it.hasMedia() }

        with(binding) {
            tvProfileName.text = contact.name

            profileImage.setImageResource(R.drawable.profile_picture_placeholder)

            conversation?.lastMessageTimestamp?.let {
                tvLastSeen.text = formatTimestamp(it)
                tvLastSeen.visibility = View.VISIBLE
            } ?: run {
                tvLastSeen.visibility = View.GONE
            }

            tvAbout.text = "¡Hola! Estoy usando Wami."
            tvMediaCount.text = mediaCount.toString()
            tvLocalTime.text = "--:--"
            tvCommonGroupsCount.text = "0"

            if (!contact.phoneNumber.isNullOrBlank()) {
                layoutPhone.visibility = View.VISIBLE
                tvPhone.text = contact.phoneNumber
            } else {
                layoutPhone.visibility = View.GONE
            }

            btnBlock.findTextView()?.text = "Bloquear a ${contact.name}"
            btnReport.findTextView()?.text = "Reportar a ${contact.name}"
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
            Toast.makeText(this, "Ver grupos en común (no implementado)", Toast.LENGTH_SHORT).show()
        }

        binding.btnBlock.setOnClickListener {
            Toast.makeText(this, "Bloquear contacto (no implementado)", Toast.LENGTH_SHORT).show()
        }

        binding.btnReport.setOnClickListener {
            Toast.makeText(this, "Reportar contacto (no implementado)", Toast.LENGTH_SHORT).show()
        }
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
                val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
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
