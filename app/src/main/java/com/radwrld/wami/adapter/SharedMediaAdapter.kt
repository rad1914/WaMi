// @path: app/src/main/java/com/radwrld/wami/adapter/SharedMediaAdapter.kt
package com.radwrld.wami.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.radwrld.wami.MediaViewActivity
import com.radwrld.wami.R
import com.radwrld.wami.network.Message
import com.radwrld.wami.util.MediaCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SharedMediaAdapter(
    private val context: Context,
    private var messages: List<Message>
) : RecyclerView.Adapter<SharedMediaAdapter.MediaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun updateData(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMediaThumbnail: ImageView = itemView.findViewById(R.id.iv_media_thumbnail)
        private val ivVideoPlayIcon: ImageView = itemView.findViewById(R.id.iv_video_play_icon)

        fun bind(message: Message) {
            ivVideoPlayIcon.visibility = if (message.isVideo()) View.VISIBLE else View.GONE
            val mediaUri: Any? = when {
                !message.localMediaPath.isNullOrBlank() -> File(message.localMediaPath!!)
                !message.mediaUrl.isNullOrBlank() -> message.mediaUrl
                else -> null
            }
            Glide.with(context)
                .load(mediaUri)
                .placeholder(R.drawable.ic_media_placeholder)
                .error(R.drawable.ic_image_error)
                .into(ivMediaThumbnail)
            itemView.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    getUriForViewing(message)?.let { (uri, mime) ->
                        val intent = Intent(context, MediaViewActivity::class.java).apply {
                            setDataAndType(uri, mime)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
        private suspend fun getUriForViewing(message: Message): Pair<Uri, String>? {
            val mimeType = message.mimetype ?: return null
            message.localMediaPath?.let {
                val file = File(it)
                if (file.exists()) return providerUri(file) to mimeType
            }
            message.mediaSha256?.let { hash ->
                val ext = MediaCache.fileExt(mimeType)
                MediaCache.getCachedFile(context, hash, ext)?.let { return providerUri(it) to mimeType }
            }
            if (!message.mediaUrl.isNullOrBlank() && !message.mediaSha256.isNullOrBlank()) {
                val ext = MediaCache.fileExt(mimeType)
                MediaCache.downloadAndCache(context, message.mediaUrl, message.mediaSha256, ext)?.let { return providerUri(it) to mimeType }
            }
            return null
        }
        private fun providerUri(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }
}
