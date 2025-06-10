package com.radwrld.wami

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.radwrld.wami.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // You can add settings-specific logic here in the future.
    }
}
