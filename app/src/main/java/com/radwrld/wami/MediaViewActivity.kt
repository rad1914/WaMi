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
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.DataSource
import com.radwrld.wami.databinding.ActivityMediaViewBinding

class MediaViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemUI()

        val uri = intent.data ?: return finishWithToast("Media not found")
        val type = intent.type ?: return finishWithToast("Media not found")

        binding.btnCloseMedia.setOnClickListener { finish() }

        when {
            type.startsWith("image/") -> showImage(uri)
            type.startsWith("video/") -> showVideo(uri)
            else -> finishWithToast("Unsupported media type")
        }
    }

    private fun showImage(uri: Uri) {
        binding.ivMediaFull.visibility = View.VISIBLE
        binding.progressBarMedia.visibility = View.VISIBLE
        Glide.with(this).load(uri).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, first: Boolean) = false.also {
                binding.progressBarMedia.visibility = View.GONE
                toast("Failed to load image")
            }
            override fun onResourceReady(r: Drawable, model: Any, t: Target<Drawable>, d: DataSource, first: Boolean) = false.also {
                binding.progressBarMedia.visibility = View.GONE
            }
        }).into(binding.ivMediaFull)
    }

    private fun showVideo(uri: Uri) {
        binding.vvMediaFull.visibility = View.VISIBLE
        binding.progressBarMedia.visibility = View.VISIBLE
        binding.vvMediaFull.apply {
            setMediaController(MediaController(this@MediaViewActivity).apply { setAnchorView(this@apply) })
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
    private fun finishWithToast(msg: String) = toast(msg).also { finish() }
}
