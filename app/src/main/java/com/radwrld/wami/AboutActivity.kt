// @path: app/src/main/java/com/radwrld/wami/AboutActivity.kt
package com.radwrld.wami

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.radwrld.wami.databinding.ActivityAboutBinding
import com.radwrld.wami.storage.ContactStorage

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding
    private val jid by lazy { intent.getStringExtra(EXTRA_JID).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (jid.isBlank()) {
            Toast.makeText(this, "Error: Contact not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadAndDisplayContactInfo()
        setupActionButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadAndDisplayContactInfo() {
        val contactStorage = ContactStorage(this)
        val contact = contactStorage.getContacts().find { it.id == jid }

        if (contact == null) {
            Toast.makeText(this, "Could not load contact details.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        with(binding) {
            tvProfileName.text = contact.name
            
            // La fecha de "unión" es estática según el diseño original.
            // Si tuvieras este dato en tu modelo, lo cargarías aquí.
            tvJoinedDate.text = "Joined in 2021"

            // TODO: Cargar la imagen del avatar usando una librería como Glide o Coil
            // Glide.with(this@AboutActivity).load(contact.avatarUrl).into(profileImage)
            profileImage.setImageResource(R.drawable.profile_picture_placeholder)

            // Mostrar el número de teléfono si existe
            if (!contact.phoneNumber.isNullOrBlank()) {
                layoutPhone.visibility = View.VISIBLE
                tvPhone.text = contact.phoneNumber
            } else {
                layoutPhone.visibility = View.GONE
            }

            // El campo de email no existe en el modelo `Contact`, por lo que se mantiene oculto.
            // Si lo agregaras al modelo, aquí iría la lógica para mostrarlo.
            layoutEmail.visibility = View.GONE

            // Ocultar el encabezado "Contact" si no hay información que mostrar
            if (layoutPhone.visibility == View.GONE && layoutEmail.visibility == View.GONE) {
                headerContact.visibility = View.GONE
            }
        }
    }

    private fun setupActionButtons() {
        binding.btnBlock.setOnClickListener {
            // TODO: Implementar lógica para bloquear contacto
            Toast.makeText(this, "Block contact (not implemented)", Toast.LENGTH_SHORT).show()
        }

        binding.btnReport.setOnClickListener {
            // TODO: Implementar lógica para reportar contacto
            Toast.makeText(this, "Report contact (not implemented)", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_JID = "EXTRA_JID"
    }
}
