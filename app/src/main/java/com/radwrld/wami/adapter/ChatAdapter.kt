// @path: app/src/main/java/com/radwrld/wami/adapter/ChatAdapter.kt
package com.radwrld.wami.adapter

import android.content.Context
import android.text.format.DateFormat
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexboxLayout
import com.radwrld.wami.R
import com.radwrld.wami.databinding.*
import com.radwrld.wami.model.Message
import com.radwrld.wami.ui.TextFormatter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

sealed class ChatListItem {
    data class MessageItem(val message: Message) : ChatListItem()
    data class DividerItem(val timestamp: Long) : ChatListItem()
    object WarningItem : ChatListItem()
}

class ChatDiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
    override fun areItemsTheSame(old: ChatListItem, new: ChatListItem): Boolean = when {
        old is ChatListItem.MessageItem && new is ChatListItem.MessageItem -> old.message.id == new.message.id
        old is ChatListItem.DividerItem && new is ChatListItem.DividerItem -> old.timestamp == new.timestamp
        else -> old == new
    }
    override fun areContentsTheSame(old: ChatListItem, new: ChatListItem): Boolean = old == new
}

class ChatAdapter(private val isGroup: Boolean) : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    var onMediaClickListener: ((Message) -> Unit)? = null
    var onReactionClicked: ((Message, String) -> Unit)? = null

    private var expandedViewHolder: BaseMessageViewHolder? = null

    fun collapseExpandedViewHolder() {
        expandedViewHolder?.collapse()
        expandedViewHolder = null
    }

    override fun getItemViewType(position: Int): Int = when (val item = getItem(position)) {
        is ChatListItem.MessageItem -> if (item.message.isOutgoing) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
        is ChatListItem.DividerItem -> VIEW_TYPE_DIVIDER
        is ChatListItem.WarningItem -> VIEW_TYPE_WARNING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_OUTGOING -> OutgoingMessageViewHolder(ItemOutgoingMessageBinding.inflate(inflater, parent, false))
            VIEW_TYPE_INCOMING -> IncomingMessageViewHolder(ItemIncomingMessageBinding.inflate(inflater, parent, false))
            VIEW_TYPE_DIVIDER -> DividerViewHolder(ItemDividerBinding.inflate(inflater, parent, false))
            VIEW_TYPE_WARNING -> WarningViewHolder(ItemWarningBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatListItem.MessageItem -> (holder as? BaseMessageViewHolder)?.bind(item.message)
            is ChatListItem.DividerItem -> (holder as? DividerViewHolder)?.bind(item)
            is ChatListItem.WarningItem -> { /* No-op */ }
        }
    }

    abstract inner class BaseMessageViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        abstract val bubble: View
        abstract val ivSticker: ImageView
        abstract val reactionPanel: View
        abstract val messageContent: ItemMessageContentBinding
        abstract val reactionsLayout: FlexboxLayout
        abstract val messageInfo: ItemMessageInfoBinding
        abstract val replyView: ViewReplyContentBinding

        private val gestureDetector = GestureDetector(itemView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent) = true.also { collapseExpandedViewHolder() }
            override fun onLongPress(e: MotionEvent) {
                if (expandedViewHolder != this@BaseMessageViewHolder) collapseExpandedViewHolder()
                expand(bindingAdapterPosition)
                expandedViewHolder = this@BaseMessageViewHolder
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                (getItem(bindingAdapterPosition) as? ChatListItem.MessageItem)?.message?.let {
                    onReactionClicked?.invoke(it, REACTIONS.first())
                }
                return true
            }
        })

        open fun bind(message: Message) = if (message.isSticker()) bindSticker(message) else bindBubble(message)

        fun collapse() {
            reactionPanel.isVisible = false
            reactionsLayout.isVisible = false
            messageInfo.root.isVisible = false
        }

        private fun expand(position: Int) {
            val message = (getItem(position) as? ChatListItem.MessageItem)?.message ?: return
            reactionPanel.fadeIn()
            reactionsLayout.isVisible = message.reactions.isNotEmpty()
            messageInfo.root.isVisible = true
            bubble.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        
        private fun bindBubble(message: Message) {
            collapse()
            bubble.isVisible = true
            ivSticker.isVisible = false
            
            bubble.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event); true }
            
            bindBubbleContent(message)
            bindReactions(message)
            bindReactionPanel(message)

            val fifteenMinutesAgo = System.currentTimeMillis() - (15 * 60 * 1000)
            if (message.timestamp > fifteenMinutesAgo) messageInfo.root.isVisible = true
        }

        private fun bindSticker(message: Message) {
            collapse()
            bubble.isVisible = false
            ivSticker.isVisible = true
            ivSticker.setOnClickListener { onMediaClickListener?.invoke(message) }
            
            val path = message.localMediaPath
            if (!path.isNullOrBlank()) {
                Glide.with(itemView.context).load(File(path)).into(ivSticker)
            } else {
                ivSticker.setImageResource(R.drawable.ic_media_placeholder)
            }
        }

        private fun bindBubbleContent(message: Message) {
            messageContent.tvMessage.apply {
                isVisible = message.hasText()
                if (isVisible) text = TextFormatter.format(context, message.text!!)
            }

            val hasMedia = !message.localMediaPath.isNullOrBlank() || !message.mediaUrl.isNullOrBlank()
            messageContent.mediaContainer.isVisible = hasMedia
            if (hasMedia) {
                messageContent.mediaContainer.setOnClickListener { onMediaClickListener?.invoke(message) }
                val localPath = message.localMediaPath
                messageContent.ivPlayIcon.isVisible = message.isVideo() && !localPath.isNullOrBlank()

                if (!localPath.isNullOrBlank()) {
                    Glide.with(itemView.context).load(File(localPath))
                        .placeholder(R.drawable.ic_media_placeholder).error(R.drawable.ic_image_error)
                        .into(messageContent.ivMedia)
                } else {
                    messageContent.ivMedia.setImageResource(R.drawable.ic_media_placeholder)
                }
            }

            replyView.root.isVisible = message.quotedMessageId != null
            if (replyView.root.isVisible) {
                replyView.tvReplySender.text = message.name
                replyView.tvReplyText.text = message.quotedMessageText ?: "Media"
            }
            
            messageInfo.tvTimestamp.text = DateFormat.getTimeFormat(itemView.context).format(Date(message.timestamp))
        }

        private fun bindReactions(message: Message) {
            reactionsLayout.removeAllViews()
            if (message.reactions.isNotEmpty()) {
                val inflater = LayoutInflater.from(itemView.context)
                message.reactions.forEach { (emoji, count) ->
                    val reactionView = ItemReactionChipBinding.inflate(inflater, reactionsLayout, false).apply {
                        tvEmoji.text = emoji
                        tvCount.text = count.toString()
                    }
                    reactionsLayout.addView(reactionView.root)
                }
            }
        }

        private fun bindReactionPanel(message: Message) {
            REACTIONS.forEachIndexed { index, emoji ->
                reactionPanel.findViewWithTag<TextView>("reaction_${index + 1}")?.setOnClickListener {
                    onReactionClicked?.invoke(message, emoji)
                    collapseExpandedViewHolder()
                }
            }
        }

        private fun View.fadeIn() {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(200).start()
        }
    }

    inner class OutgoingMessageViewHolder(private val binding: ItemOutgoingMessageBinding) : BaseMessageViewHolder(binding) {
        override val bubble get() = binding.bubbleLayout
        override val ivSticker get() = binding.ivSticker
        override val reactionPanel get() = binding.reactionPanel
        override val messageContent get() = binding.messageContent
        override val reactionsLayout get() = binding.reactionGroup
        override val messageInfo get() = binding.messageInfo
        override val replyView get() = binding.replyView

        override fun bind(message: Message) {
            super.bind(message)
            if (!message.isSticker()) {
                messageInfo.ivStatus.isVisible = true
                messageInfo.ivStatus.setImageResource(when (message.status) {
                    "read" -> R.drawable.ic_read_receipt
                    "delivered" -> R.drawable.ic_delivered_receipt
                    else -> R.drawable.ic_sending
                })
            }
        }
    }
    
    inner class IncomingMessageViewHolder(private val binding: ItemIncomingMessageBinding) : BaseMessageViewHolder(binding) {
        override val bubble get() = binding.bubbleLayout
        override val ivSticker get() = binding.ivSticker
        override val reactionPanel get() = binding.reactionPanel
        override val messageContent get() = binding.messageContent
        override val reactionsLayout get() = binding.reactionGroup
        override val messageInfo get() = binding.messageInfo
        override val replyView get() = binding.replyView

        override fun bind(message: Message) {
            super.bind(message)
            if (!message.isSticker()) {
                binding.tvSenderName.isVisible = isGroup
                if (isGroup) binding.tvSenderName.text = message.senderName
                messageInfo.ivStatus.isVisible = false
            }
        }
    }

        class DividerViewHolder(private val binding: ItemDividerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatListItem.DividerItem) {
            binding.tvDivider.text = getFormattedDate(itemView.context, item.timestamp)
        }

        private fun getFormattedDate(context: Context, timestamp: Long): String {
            val messageCal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val todayCal = Calendar.getInstance()
            
            // Corrige el caso para "hoy" para que muestre la hora
            if (messageCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
                return DateFormat.getTimeFormat(context).format(Date(timestamp))
            }
            
            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            if (messageCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)) {
                return context.getString(R.string.yesterday)
            }
            
            return SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }


    class WarningViewHolder(binding: ItemWarningBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val VIEW_TYPE_OUTGOING = 1
        private const val VIEW_TYPE_INCOMING = 2
        private const val VIEW_TYPE_DIVIDER = 3
        private const val VIEW_TYPE_WARNING = 4
        private val REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
    }
}
