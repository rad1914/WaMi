// @path: app/src/main/java/com/radwrld/wami/SocialActivity.kt
package com.radwrld.wami

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.radwrld.wami.databinding.ActivitySocialBinding

class SocialActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySocialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupClickListeners()
    }

    private fun setupClickListeners() {
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
