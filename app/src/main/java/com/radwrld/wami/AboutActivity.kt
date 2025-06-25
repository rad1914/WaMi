// @path: app/src/main/java/com/radwrld/wami/AboutActivity.kt
package com.radwrld.wami

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.radwrld.wami.databinding.ActivityAboutBinding
import com.radwrld.wami.storage.ContactStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        // CAMBIO: Se mantiene tu implementación original, es más directa.
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

            // TODO: Cargar la imagen del avatar usando una librería como Glide o Coil
            // Glide.with(this@AboutActivity).load(contact.avatarUrl).into(profileImage)
            profileImage.setImageResource(R.drawable.profile_picture_placeholder)

            // CAMBIO: 'tvJoinedDate' se reemplazó por 'tvLastSeen'.
            // TODO: Cargar este dato real desde tu modelo de datos.
            val lastSeenTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            tvLastSeen.text = "últ. vez hoy a las $lastSeenTime"

            // NUEVO: Se asigna texto a los nuevos campos.
            // TODO: Cargar la info/about real del contacto.
            tvAbout.text = "¡Hola! Estoy usando Wami."

            // TODO: Cargar la cuenta real de archivos compartidos.
            tvMediaCount.text = "0"

            // TODO: Calcular y mostrar la hora local real del contacto si tienes su zona horaria.
            tvLocalTime.text = "--:--"
            
            // TODO: Cargar el número real de grupos en común.
            tvCommonGroupsCount.text = "0"

            // CAMBIO: La lógica para el teléfono se mantiene, pero se simplifica.
            if (!contact.phoneNumber.isNullOrBlank()) {
                layoutPhone.visibility = View.VISIBLE
                tvPhone.text = contact.phoneNumber
            } else {
                layoutPhone.visibility = View.GONE
            }

            // CAMBIO: Se actualiza el texto de los botones para ser más específico.
            btnBlock.findTextView()?.text = "Bloquear a ${contact.name}"
            btnReport.findTextView()?.text = "Reportar a ${contact.name}"
        }
    }

    private fun setupActionButtons() {
        // NUEVO: Listeners para los nuevos elementos clickables.
        binding.btnSharedMedia.setOnClickListener {
            Toast.makeText(this, "Ver multimedia (no implementado)", Toast.LENGTH_SHORT).show()
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

    companion object {
        const val EXTRA_JID = "EXTRA_JID"
    }
}

// NUEVO: Función de ayuda para encontrar el TextView dentro de los LinearLayout de acción.
private fun LinearLayout.findTextView(): TextView? {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is TextView) {
            return child
        }
    }
    return null
}
