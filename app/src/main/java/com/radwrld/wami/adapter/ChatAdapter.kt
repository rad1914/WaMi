// @path: app/src/main/java/com/radwrld/wami/adapter/ChatAdapter.kt
package com.radwrld.wami.adapter

import android.annotation.SuppressLint
import android.text.method.LinkMovementMethod
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.R
import com.radwrld.wami.databinding.ItemDividerBinding
import com.radwrld.wami.databinding.ItemIncomingMessageBinding
import com.radwrld.wami.databinding.ItemOutgoingMessageBinding
import com.radwrld.wami.databinding.ItemWarningBinding
import com.radwrld.wami.model.Message
import java.util.Calendar

// Sealed class to represent different items in our chat list (unchanged)
sealed class ChatListItem {
    object WarningItem : ChatListItem()
    data class MessageItem(val message: Message) : ChatListItem()
    data class DividerItem(val timestamp: Long, val isNewDay: Boolean) : ChatListItem()
}

// DiffUtil callback for calculating list differences efficiently.
class ChatDiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
    override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
        return when {
            oldItem is ChatListItem.MessageItem && newItem is ChatListItem.MessageItem ->
                oldItem.message.id == newItem.message.id
            oldItem is ChatListItem.DividerItem && newItem is ChatListItem.DividerItem ->
                oldItem.timestamp == newItem.timestamp
            oldItem is ChatListItem.WarningItem && newItem is ChatListItem.WarningItem ->
                true // Only one type of warning item
            else -> oldItem::class == newItem::class
        }
    }

    override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
        // Data classes have a built-in `equals` method that compares all properties.
        return oldItem == newItem
    }
}

// The adapter now extends ListAdapter, using the ChatDiffCallback.
class ChatAdapter : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(ChatDiffCallback()) {

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
            binding.tvMessage.text = message.text
            // Respects user's 12/24 hour setting
            binding.tvTimestamp.text = DateFormat.getTimeFormat(context).format(message.timestamp)
            
            // UPDATED: Display status with symbols for better UX.
            // For an ideal implementation, you would use an ImageView with drawable assets.
            binding.tvStatus.text = when (message.status) {
                "read" -> "✓✓" // Should be a blue checkmark icon
                "delivered" -> "✓✓" // Should be a grey checkmark icon
                "sent" -> "✓" // Should be a single grey checkmark icon
                "sending" -> "…" // Should be a clock icon
                "failed" -> "!" // Should be a warning icon
                else -> "" // Hide status otherwise
            }
        }
    }

    inner class IncomingMessageViewHolder(val binding: ItemIncomingMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            setupListeners(binding.bubbleLayout, binding.infoContainer)
        }
        fun bind(message: Message) {
            val context = binding.root.context
            binding.tvMessage.text = message.text
            // Respects user's 12/24 hour setting
            binding.tvTimestamp.text = DateFormat.getTimeFormat(context).format(message.timestamp)

            if (!message.senderName.isNullOrEmpty()) {
                binding.tvSenderName.text = message.senderName
                binding.tvSenderName.visibility = View.VISIBLE
            } else {
                binding.tvSenderName.visibility = View.GONE
            }
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
                // Respects user's 12/24 hour setting
                DateFormat.getTimeFormat(context).format(timestamp)
            }
        }
    }

    inner class WarningViewHolder(binding: ItemWarningBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Linkify URLs in the static warning message
            Linkify.addLinks(binding.tvWarning, Linkify.WEB_URLS)
            binding.tvWarning.movementMethod = LinkMovementMethod.getInstance()
        }
        // No bind method needed as the content is static from XML.
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
        // Use getItem(position) from ListAdapter
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
                // Inflate from XML instead of creating programmatically
                val binding = ItemDividerBinding.inflate(inflater, parent, false)
                DividerViewHolder(binding)
            }
            VIEW_TYPE_WARNING -> {
                // Inflate from XML instead of creating programmatically
                val binding = ItemWarningBinding.inflate(inflater, parent, false)
                WarningViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Use getItem(position) from ListAdapter
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
                // No binding needed for the static warning view.
            }
        }
    }
    
    // No need for getItemCount(); ListAdapter handles it.
    // No need for updateStatus(); submit a new list from your ViewModel/Activity instead.
}
