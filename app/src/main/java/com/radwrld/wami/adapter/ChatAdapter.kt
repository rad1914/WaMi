// @path: app/src/main/java/com/radwrld/wami/adapter/ChatAdapter.kt
package com.radwrld.wami.adapter

import android.annotation.SuppressLint
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.radwrld.wami.R
import com.radwrld.wami.databinding.*
import com.radwrld.wami.model.Message
import com.radwrld.wami.util.TextFormatter
import java.util.*

sealed class ChatListItem {
    data class MessageItem(val message: Message) : ChatListItem()
    data class DividerItem(val timestamp: Long) : ChatListItem()
}

class ChatDiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
    override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
        return when {
            oldItem is ChatListItem.MessageItem && newItem is ChatListItem.MessageItem -> oldItem.message.id == newItem.message.id
            oldItem is ChatListItem.DividerItem && newItem is ChatListItem.DividerItem -> oldItem.timestamp == newItem.timestamp
            else -> false
        }
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
        return oldItem == newItem
    }
}

class ChatAdapter(private val isGroup: Boolean) : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    var onMediaClickListener: ((Message) -> Unit)? = null
    var onReactionClicked: ((Message, String) -> Unit)? = null

    private var expandedViewHolder: BaseMessageViewHolder? = null
    
    fun collapseExpandedViewHolder() {
        expandedViewHolder?.collapsePanels()
        expandedViewHolder = null
    }

    companion object {
        private const val VIEW_TYPE_OUTGOING = 1
        private const val VIEW_TYPE_INCOMING = 2
        private const val VIEW_TYPE_DIVIDER = 3
        private val REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
    }

    abstract inner class BaseMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun collapsePanels()
        abstract fun bind(message: Message)

        fun setupListeners(message: Message, bubble: View, reactionPanel: View, infoContainer: View) {
            val gestureDetector = GestureDetector(itemView.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (reactionPanel.isVisible) {
                        collapseExpandedViewHolder()
                    }
                    return true
                }
                override fun onLongPress(e: MotionEvent) {
                    if (expandedViewHolder != this@BaseMessageViewHolder) {
                        collapseExpandedViewHolder()
                    }
                    reactionPanel.fadeIn()
                    infoContainer.fadeIn()
                    expandedViewHolder = this@BaseMessageViewHolder
                }
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onReactionClicked?.invoke(message, REACTIONS.first())
                    return true
                }
            })
            bubble.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
        }
        
        private fun View.fadeIn() {
            this.visibility = View.VISIBLE
            this.alpha = 0f
            this.animate().alpha(1f).setDuration(200).setListener(null).start()
        }
    }

    inner class OutgoingMessageViewHolder(private val binding: ItemOutgoingMessageBinding) : BaseMessageViewHolder(binding.root) {
        override fun collapsePanels() {
            binding.reactionPanel.visibility = View.GONE
            binding.infoContainer.visibility = View.GONE
        }
        override fun bind(message: Message) {
            setupListeners(message, binding.bubbleLayout, binding.reactionPanel, binding.infoContainer)
            // ... (Binding logic for media, text, quotes, reactions)
            binding.messageContent.tvMessage.text = message.text
            binding.tvTimestamp.text = DateFormat.getTimeFormat(itemView.context).format(message.timestamp)
        }
    }

    inner class IncomingMessageViewHolder(private val binding: ItemIncomingMessageBinding) : BaseMessageViewHolder(binding.root) {
        override fun collapsePanels() {
            binding.reactionPanel.visibility = View.GONE
            binding.infoContainer.visibility = View.GONE
        }
        override fun bind(message: Message) {
            setupListeners(message, binding.bubbleLayout, binding.reactionPanel, binding.infoContainer)
            // ... (Binding logic for media, text, quotes, reactions, and sender name)
            binding.tvSenderName.isVisible = isGroup
            binding.tvSenderName.text = message.name
            binding.messageContent.tvMessage.text = message.text
            binding.tvTimestamp.text = DateFormat.getTimeFormat(itemView.context).format(message.timestamp)
        }
    }
    
    class DividerViewHolder(private val binding: ItemDividerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatListItem.DividerItem) {
            binding.tvDivider.text = DateUtils.getRelativeTimeSpanString(item.timestamp, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS)
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatListItem.DividerItem -> VIEW_TYPE_DIVIDER
            is ChatListItem.MessageItem -> if (item.message.isOutgoing) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_OUTGOING -> OutgoingMessageViewHolder(ItemOutgoingMessageBinding.inflate(inflater, parent, false))
            VIEW_TYPE_INCOMING -> IncomingMessageViewHolder(ItemIncomingMessageBinding.inflate(inflater, parent, false))
            VIEW_TYPE_DIVIDER -> DividerViewHolder(ItemDividerBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatListItem.MessageItem -> (holder as BaseMessageViewHolder).bind(item.message)
            is ChatListItem.DividerItem -> (holder as DividerViewHolder).bind(item)
        }
    }
}
