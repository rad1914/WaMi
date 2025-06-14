// @path: app/src/main/java/com/radwrld/wami/adapter/ChatAdapter.kt
// @path: app/src/main/java/com/radwrld/wami/adapter/ChatAdapter.kt
package com.radwrld.wami.adapter

import android.annotation.SuppressLint
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.radwrld.wami.R
import com.radwrld.wami.databinding.ItemDividerBinding
import com.radwrld.wami.databinding.ItemIncomingMessageBinding
import com.radwrld.wami.databinding.ItemOutgoingMessageBinding
import com.radwrld.wami.databinding.ItemWarningBinding
import com.radwrld.wami.model.Message
import com.radwrld.wami.util.TextFormatter
import java.util.Calendar

sealed class ChatListItem {
    object WarningItem : ChatListItem()
    data class MessageItem(val message: Message) : ChatListItem()
    data class DividerItem(val timestamp: Long, val isNewDay: Boolean) : ChatListItem()
}

class ChatDiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
    override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
        return when {
            oldItem is ChatListItem.MessageItem && newItem is ChatListItem.MessageItem ->
                oldItem.message.id == newItem.message.id
            oldItem is ChatListItem.DividerItem && newItem is ChatListItem.DividerItem ->
                oldItem.timestamp == newItem.timestamp
            oldItem is ChatListItem.WarningItem && newItem is ChatListItem.WarningItem -> true
            else -> oldItem::class == newItem::class
        }
    }

    override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
        return oldItem == newItem
    }
}


class ChatAdapter : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    var onMediaClickListener: ((Message) -> Unit)? = null

    companion object {
        private const val VIEW_TYPE_OUTGOING = 1
        private const val VIEW_TYPE_INCOMING = 2
        private const val VIEW_TYPE_DIVIDER = 3
        private const val VIEW_TYPE_WARNING = 4
    }

    inner class OutgoingMessageViewHolder(val binding: ItemOutgoingMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            setupListeners(binding.bubbleLayout, binding.infoContainer)
        }
        fun bind(message: Message) {
            val context = binding.root.context
            binding.mediaContainer.visibility = View.GONE
            binding.replyLayout.visibility = View.GONE

            if (message.mediaUrl != null) {
                binding.mediaContainer.visibility = View.VISIBLE
                binding.ivPlayIcon.visibility = if (message.mimetype?.startsWith("video/") == true) View.VISIBLE else View.GONE
                
                binding.mediaContainer.setOnClickListener {
                    onMediaClickListener?.invoke(message)
                }

                Glide.with(context)
                    .load(message.mediaUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.stat_notify_error)
                    .into(binding.ivMedia)
            }

            if (!message.text.isNullOrEmpty()) {
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = TextFormatter.format(context, message.text)
                binding.tvMessage.movementMethod = LinkMovementMethod.getInstance()
            } else {
                binding.tvMessage.visibility = View.GONE
            }

            if (message.quotedMessageText != null) {
                binding.replyLayout.visibility = View.VISIBLE
                binding.tvReplySender.text = "You"
                binding.tvReplyText.text = message.quotedMessageText
            }

            binding.tvTimestamp.text = DateFormat.getTimeFormat(context).format(message.timestamp)
            binding.tvStatus.text = when (message.status) {
                "read" -> "✓✓"
                "delivered" -> "✓✓"
                "sent" -> "✓"
                "sending" -> "…"
                "failed" -> "!"
                else -> ""
            }
        }
    }

    inner class IncomingMessageViewHolder(val binding: ItemIncomingMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            setupListeners(binding.bubbleLayout, binding.infoContainer)
        }
        fun bind(message: Message) {
            val context = binding.root.context
            binding.mediaContainer.visibility = View.GONE
            binding.replyLayout.visibility = View.GONE

            if (message.mediaUrl != null) {
                binding.mediaContainer.visibility = View.VISIBLE
                binding.ivPlayIcon.visibility = if (message.mimetype?.startsWith("video/") == true) View.VISIBLE else View.GONE
                
                binding.mediaContainer.setOnClickListener {
                    onMediaClickListener?.invoke(message)
                }

                Glide.with(context)
                    .load(message.mediaUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.stat_notify_error)
                    .into(binding.ivMedia)
            }

            if (!message.text.isNullOrEmpty()) {
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = TextFormatter.format(context, message.text)
                binding.tvMessage.movementMethod = LinkMovementMethod.getInstance()
            } else {
                binding.tvMessage.visibility = View.GONE
            }

            if (message.quotedMessageText != null) {
                binding.replyLayout.visibility = View.VISIBLE
                // FIXED: Use the message's senderName for the reply context in groups,
                // falling back to the contact's name. This is more accurate.
                binding.tvReplySender.text = message.senderName ?: message.name
                binding.tvReplyText.text = message.quotedMessageText
            }
            
            binding.tvSenderName.isVisible = !message.senderName.isNullOrEmpty()
            binding.tvSenderName.text = message.senderName
            binding.tvTimestamp.text = DateFormat.getTimeFormat(context).format(message.timestamp)
        }
    }

    inner class DividerViewHolder(private val binding: ItemDividerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatListItem.DividerItem) {
            binding.tvDivider.text = formatDividerTimestamp(item.timestamp, item.isNewDay)
        }

        private fun formatDividerTimestamp(timestamp: Long, isNewDay: Boolean): String {
            val context = binding.root.context
            return if (isNewDay) {
                DateUtils.getRelativeTimeSpanString(timestamp, Calendar.getInstance().timeInMillis, DateUtils.DAY_IN_MILLIS).toString()
            } else {
                DateFormat.getTimeFormat(context).format(timestamp)
            }
        }
    }

    inner class WarningViewHolder(binding: ItemWarningBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            Linkify.addLinks(binding.tvWarning, Linkify.WEB_URLS)
            binding.tvWarning.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners(bubbleView: View, infoView: View) {
        bubbleView.setOnLongClickListener {
            infoView.visibility = View.VISIBLE
            true
        }
        bubbleView.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP || motionEvent.action == MotionEvent.ACTION_CANCEL) {
                infoView.visibility = View.GONE
            }
            false
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatListItem.WarningItem -> VIEW_TYPE_WARNING
            is ChatListItem.DividerItem -> VIEW_TYPE_DIVIDER
            is ChatListItem.MessageItem -> if (item.message.isOutgoing) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_OUTGOING -> {
                val binding = ItemOutgoingMessageBinding.inflate(inflater, parent, false)
                OutgoingMessageViewHolder(binding)
            }
            VIEW_TYPE_INCOMING -> {
                val binding = ItemIncomingMessageBinding.inflate(inflater, parent, false)
                IncomingMessageViewHolder(binding)
            }
            VIEW_TYPE_DIVIDER -> {
                val binding = ItemDividerBinding.inflate(inflater, parent, false)
                DividerViewHolder(binding)
            }
            VIEW_TYPE_WARNING -> {
                val binding = ItemWarningBinding.inflate(inflater, parent, false)
                WarningViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatListItem.MessageItem -> {
                if (holder is OutgoingMessageViewHolder) {
                    holder.bind(item.message)
                } else if (holder is IncomingMessageViewHolder) {
                    holder.bind(item.message)
                }
            }
            is ChatListItem.DividerItem -> {
                (holder as DividerViewHolder).bind(item)
            }
            is ChatListItem.WarningItem -> {
                // No binding needed
            }
        }
    }
}
