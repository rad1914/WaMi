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
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.radwrld.wami.databinding.ActivityMediaViewBinding
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.DataSource

class MediaViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemUI()

        val uri = intent.data
        val type = intent.type

        if (uri == null || type == null) {
            toast("Media not found"); finish(); return
        }

        binding.btnCloseMedia.setOnClickListener { finish() }

        when {
            type.startsWith("image/") -> loadImage(uri)
            type.startsWith("video/") -> loadVideo(uri)
            else -> toast("Unsupported media type").also { finish() }
        }
    }

    private fun loadImage(uri: Uri) {
        binding.ivMediaFull.visibility = View.VISIBLE
        binding.progressBarMedia.visibility = View.VISIBLE

        Glide.with(this).load(uri).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean
            ) = false.also {
                binding.progressBarMedia.visibility = View.GONE
                toast("Failed to load image")
            }

            override fun onResourceReady(
                resource: Drawable, model: Any, target: Target<Drawable>,
                dataSource: DataSource, isFirstResource: Boolean
            ) = false.also {
                binding.progressBarMedia.visibility = View.GONE
            }
        }).into(binding.ivMediaFull)
    }

    private fun loadVideo(uri: Uri) {
        binding.vvMediaFull.visibility = View.VISIBLE
        binding.progressBarMedia.visibility = View.VISIBLE

        binding.vvMediaFull.apply {
            setMediaController(MediaController(this@MediaViewActivity).also { it.setAnchorView(this) })
            setVideoURI(uri)
            setOnPreparedListener {
                binding.progressBarMedia.visibility = View.GONE
                it.start()
            }
            setOnErrorListener { _, _, _ ->
                binding.progressBarMedia.visibility = View.GONE
                toast("Failed to play video")
                true
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
