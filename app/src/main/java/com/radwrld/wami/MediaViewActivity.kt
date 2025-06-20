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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()

        // ++ Use intent.data (Uri) and intent.type (MIME type) as the primary way to receive media.
        val mediaUri = intent.data
        val mimeType = intent.type

        if (mediaUri == null || mimeType.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Media not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.btnCloseMedia.setOnClickListener { finish() }

        // ++ Conditionally load content based on the MIME type.
        when {
            mimeType.startsWith("image/") -> loadImage(mediaUri)
            mimeType.startsWith("video/") -> loadVideo(mediaUri)
            else -> {
                Toast.makeText(this, "Unsupported media type", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun loadImage(uri: Uri) {
        binding.ivMediaFull.visibility = View.VISIBLE
        binding.progressBarMedia.visibility = View.VISIBLE

        Glide.with(this)
            .load(uri) // Load directly from the provided Uri
            .listener(object : RequestListener<Drawable> {
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

    private fun loadVideo(uri: Uri) {
        binding.vvMediaFull.visibility = View.VISIBLE
        binding.progressBarMedia.visibility = View.VISIBLE

        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.vvMediaFull)
        binding.vvMediaFull.setMediaController(mediaController)
        binding.vvMediaFull.setVideoURI(uri) // Set video directly from Uri

        binding.vvMediaFull.setOnPreparedListener {
            binding.progressBarMedia.visibility = View.GONE
            it.start()
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
