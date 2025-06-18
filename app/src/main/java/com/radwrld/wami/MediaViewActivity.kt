// @path: app/src/main/java/com/radwrld/wami/MediaViewActivity.kt
package com.radwrld.wami

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.radwrld.wami.databinding.ActivityMediaViewBinding

class MediaViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaViewBinding

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_MIMETYPE = "extra_mimetype"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()

        val mediaUrl = intent.getStringExtra(EXTRA_URL)
        val mimeType = intent.getStringExtra(EXTRA_MIMETYPE)

        if (mediaUrl.isNullOrEmpty() || mimeType.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Media not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.btnCloseMedia.setOnClickListener { finish() }

        when {
            mimeType.startsWith("image/") -> loadImage(mediaUrl)
            mimeType.startsWith("video/") -> loadVideo(mediaUrl)
            else -> {
                Toast.makeText(this, "Unsupported media type", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun loadImage(url: String) {
        binding.ivMediaFull.visibility = View.VISIBLE
        binding.progressBarMedia.visibility = View.VISIBLE

        Glide.with(this)
            .load(url)
            .listener(object : RequestListener<Drawable> {
                // Corrected onLoadFailed signature
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.progressBarMedia.visibility = View.GONE
                    Toast.makeText(this@MediaViewActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                    return false
                }

                // Corrected onResourceReady signature
                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.progressBarMedia.visibility = View.GONE
                    return false
                }
            })
            .into(binding.ivMediaFull)
    }

    private fun loadVideo(url: String) {
        binding.vvMediaFull.visibility = View.VISIBLE
        binding.progressBarMedia.visibility = View.VISIBLE

        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.vvMediaFull)
        binding.vvMediaFull.setMediaController(mediaController)
        binding.vvMediaFull.setVideoURI(Uri.parse(url))

        binding.vvMediaFull.setOnPreparedListener {
            binding.progressBarMedia.visibility = View.GONE
            binding.vvMediaFull.start()
        }

        binding.vvMediaFull.setOnErrorListener { _, _, _ ->
            binding.progressBarMedia.visibility = View.GONE
            Toast.makeText(this, "Failed to play video", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
