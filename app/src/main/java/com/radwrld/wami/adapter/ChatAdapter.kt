// @path: app/src/main/java/com/radwrld/wami/adapter/ChatAdapter.kt
// @path: app/src/main/java/com/radwrld/wami/adapter/ChatAdapter.kt
package com.radwrld.wami.adapter

import android.annotation.SuppressLint
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
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

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
        return when {
            oldItem is ChatListItem.MessageItem && newItem is ChatListItem.MessageItem -> oldItem.message == newItem.message
            oldItem is ChatListItem.DividerItem && newItem is ChatListItem.DividerItem -> oldItem == newItem
            else -> oldItem == newItem
        }
    }
}

class ChatAdapter : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    var onMediaClickListener: ((Message) -> Unit)? = null
    var onReactionClicked: ((Message, String) -> Unit)? = null

    companion object {
        private const val VIEW_TYPE_OUTGOING = 1
        private const val VIEW_TYPE_INCOMING = 2
        private const val VIEW_TYPE_DIVIDER = 3
        private const val VIEW_TYPE_WARNING = 4
    }

    private fun showReactionMenu(view: View, message: Message) {
        val popup = PopupMenu(view.context, view)
        val reactions = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
        reactions.forEach { popup.menu.add(it) }

        popup.setOnMenuItemClickListener { menuItem ->
            onReactionClicked?.invoke(message, menuItem.title.toString())
            true
        }
        popup.show()
    }

    inner class OutgoingMessageViewHolder(val binding: ItemOutgoingMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            val context = binding.root.context
            // CORRECTED: Access included views via 'messageContent'
            val contentBinding = binding.messageContent
            
            contentBinding.mediaContainer.setOnClickListener { onMediaClickListener?.invoke(message) }
            binding.bubbleLayout.setOnLongClickListener {
                showReactionMenu(it, message)
                binding.infoContainer.visibility = View.VISIBLE
                true
            }
            binding.bubbleLayout.setOnClickListener {
                binding.infoContainer.visibility = View.GONE
            }

            // FIXED: Sticker logic is now enabled and functional.
            if (message.type == "sticker") {
                binding.bubbleLayout.visibility = View.GONE
                binding.ivSticker.visibility = View.VISIBLE
                Glide.with(context).load(message.mediaUrl).into(binding.ivSticker)
            } else {
                binding.bubbleLayout.visibility = View.VISIBLE
                binding.ivSticker.visibility = View.GONE

                if (message.mediaUrl != null && (message.type == "image" || message.type == "video")) {
                    contentBinding.mediaContainer.visibility = View.VISIBLE
                    contentBinding.ivPlayIcon.visibility = if (message.type == "video") View.VISIBLE else View.GONE
                    Glide.with(context).load(message.mediaUrl).placeholder(R.drawable.ic_media_placeholder).into(contentBinding.ivMedia)
                } else {
                    contentBinding.mediaContainer.visibility = View.GONE
                }

                if (!message.text.isNullOrEmpty()) {
                    contentBinding.tvMessage.visibility = View.VISIBLE
                    contentBinding.tvMessage.text = TextFormatter.format(context, message.text)
                } else {
                    contentBinding.tvMessage.visibility = View.GONE
                }

                if (message.quotedMessageText != null) {
                    binding.replyLayout.visibility = View.VISIBLE
                    binding.tvReplySender.text = "You"
                    binding.tvReplyText.text = message.quotedMessageText
                } else {
                    binding.replyLayout.visibility = View.GONE
                }
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
        fun bind(message: Message) {
            val context = binding.root.context
            // CORRECTED: Access included views via 'messageContent'
            val contentBinding = binding.messageContent

            contentBinding.mediaContainer.setOnClickListener { onMediaClickListener?.invoke(message) }
            binding.bubbleLayout.setOnLongClickListener {
                showReactionMenu(it, message)
                binding.infoContainer.visibility = View.VISIBLE
                true
            }
            binding.bubbleLayout.setOnClickListener {
                binding.infoContainer.visibility = View.GONE
            }
            
            // FIXED: Sticker logic is now enabled and functional.
            if (message.type == "sticker") {
                binding.bubbleLayout.visibility = View.GONE
                binding.ivSticker.visibility = View.VISIBLE
                Glide.with(context).load(message.mediaUrl).into(binding.ivSticker)
            } else {
                binding.bubbleLayout.visibility = View.VISIBLE
                binding.ivSticker.visibility = View.GONE

                binding.tvSenderName.isVisible = !message.senderName.isNullOrEmpty()
                binding.tvSenderName.text = message.senderName

                if (message.mediaUrl != null && (message.type == "image" || message.type == "video")) {
                    contentBinding.mediaContainer.visibility = View.VISIBLE
                    contentBinding.ivPlayIcon.visibility = if (message.type == "video") View.VISIBLE else View.GONE
                    Glide.with(context).load(message.mediaUrl).placeholder(R.drawable.ic_media_placeholder).into(contentBinding.ivMedia)
                } else {
                    contentBinding.mediaContainer.visibility = View.GONE
                }
                
                if (!message.text.isNullOrEmpty()) {
                    contentBinding.tvMessage.visibility = View.VISIBLE
                    contentBinding.tvMessage.text = TextFormatter.format(context, message.text)
                } else {
                    contentBinding.tvMessage.visibility = View.GONE
                }
                
                if (message.quotedMessageText != null) {
                    binding.replyLayout.visibility = View.VISIBLE
                    binding.tvReplySender.text = message.senderName ?: message.name
                    binding.tvReplyText.text = message.quotedMessageText
                } else {
                    binding.replyLayout.visibility = View.GONE
                }
            }
            
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

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatListItem.WarningItem -> VIEW_TYPE_WARNING
            is ChatListItem.DividerItem -> VIEW_TYPE_DIVIDER
            is ChatListItem.MessageItem -> if (item.message.isOutgoing) {
                VIEW_TYPE_OUTGOING
            } else {
                VIEW_TYPE_INCOMING
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_OUTGOING -> OutgoingMessageViewHolder(ItemOutgoingMessageBinding.inflate(inflater, parent, false))
            VIEW_TYPE_INCOMING -> IncomingMessageViewHolder(ItemIncomingMessageBinding.inflate(inflater, parent, false))
            VIEW_TYPE_DIVIDER -> DividerViewHolder(ItemDividerBinding.inflate(inflater, parent, false))
            VIEW_TYPE_WARNING -> WarningViewHolder(ItemWarningBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatListItem.MessageItem -> {
                when (holder) {
                    is OutgoingMessageViewHolder -> holder.bind(item.message)
                    is IncomingMessageViewHolder -> holder.bind(item.message)
                }
            }
            is ChatListItem.DividerItem -> (holder as DividerViewHolder).bind(item)
            is ChatListItem.WarningItem -> { /* No binding needed */ }
        }
    }
}
