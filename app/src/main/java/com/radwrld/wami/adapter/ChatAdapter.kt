package com.radwrld.wami.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.flexbox.FlexboxLayout
import com.radwrld.wami.R
import com.radwrld.wami.databinding.*
import com.radwrld.wami.model.Message
import com.radwrld.wami.ui.TextFormatter
import java.io.File
import java.io.IOException
import java.util.*

sealed class ChatListItem {
    data class MessageItem(val message: Message) : ChatListItem()
    data class DividerItem(val timestamp: Long) : ChatListItem()
    object WarningItem : ChatListItem()
}

class ChatDiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
    override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
        return when {
            oldItem is ChatListItem.MessageItem && newItem is ChatListItem.MessageItem -> oldItem.message.id == newItem.message.id
            oldItem is ChatListItem.DividerItem && newItem is ChatListItem.DividerItem -> oldItem.timestamp == newItem.timestamp
            oldItem is ChatListItem.WarningItem && newItem is ChatListItem.WarningItem -> true
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
        private const val VIEW_TYPE_WARNING = 4
        private val REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
    }

    abstract inner class BaseMessageViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        abstract val bubble: View
        abstract val reactionPanel: View
        abstract val messageContent: ItemMessageContentBinding
        abstract val reactionsLayout: FlexboxLayout
        abstract val tvTimestamp: TextView

        abstract fun bindQuote(message: Message)

        open fun bind(message: Message) {
            collapsePanels()
            bindText(message)
            bindMedia(message)
            bindQuote(message)
            bindReactions(message)
            bindReactionPanel(message)
            setupListeners(message)
            tvTimestamp.text = DateFormat.getTimeFormat(itemView.context).format(Date(message.timestamp))
        }

        fun collapsePanels() {
            reactionPanel.visibility = View.GONE
        }

        private fun bindText(message: Message) {
            messageContent.tvMessage.isVisible = message.hasText()
            if (message.hasText()) {
                messageContent.tvMessage.text = TextFormatter.format(itemView.context, message.text!!)
            }
        }

        private fun bindMedia(message: Message) {
            messageContent.mediaContainer.isVisible = message.hasMedia()
            messageContent.ivPlayIcon.isVisible = message.isVideo()
            if (message.hasMedia()) {
                val mediaPath = message.getMediaPath()

                Glide.with(itemView.context)
                    .load(if (mediaPath?.startsWith("/") == true) File(mediaPath) else Uri.parse(mediaPath))
                    .placeholder(R.drawable.ic_media_placeholder)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                            Log.e("ChatAdapter", "Glide FAILED for $mediaPath", e)
                            
                            // MODIFIED LOGIC: Check for any network exception (IOException)
                            // This is compatible with the OkHttp integration.
                            val isNetworkError = e?.causes?.any { it is IOException } == true
                            if (isNetworkError) {
                                // Set our specific "media not found" icon for any network issue (404, timeout, etc)
                                val view = (target as? com.bumptech.glide.request.target.ViewTarget<*,*>)?.view
                                if (view is ImageView) {
                                    view.setImageResource(R.drawable.ic_image_broken_variant)
                                }
                                return true // Return true to prevent error placeholder from showing
                            }
                            return false // Return false to show the generic error placeholder for other errors
                        }

                        override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                           // Load was successful, do nothing special.
                            return false
                        }
                    })
                    .error(R.drawable.ic_image_error) // Generic error for non-network issues
                    .into(messageContent.ivMedia)

                messageContent.mediaContainer.setOnClickListener { onMediaClickListener?.invoke(message) }
            } else {
                messageContent.mediaContainer.setOnClickListener(null)
            }
        }

        private fun bindReactions(message: Message) {
            reactionsLayout.removeAllViews()
            reactionsLayout.isVisible = message.reactions.isNotEmpty()
            if (message.reactions.isNotEmpty()) {
                val inflater = LayoutInflater.from(itemView.context)
                message.reactions.entries.forEach { entry ->
                    val reactionView = ItemReactionChipBinding.inflate(inflater, reactionsLayout, false)
                    reactionView.tvEmoji.text = entry.key
                    reactionView.tvCount.text = entry.value.toString()
                    reactionsLayout.addView(reactionView.root)
                }
            }
        }

        private fun bindReactionPanel(message: Message) {
            REACTIONS.forEachIndexed { index, emoji ->
                (reactionPanel.findViewWithTag<TextView>("reaction_${index + 1}"))?.setOnClickListener {
                    onReactionClicked?.invoke(message, emoji)
                    collapsePanels()
                }
            }
        }

        private fun setupListeners(message: Message) {
            val gestureDetector = GestureDetector(itemView.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    collapseExpandedViewHolder(); return true
                }
                override fun onLongPress(e: MotionEvent) {
                    if (expandedViewHolder != this@BaseMessageViewHolder) collapseExpandedViewHolder()
                    reactionPanel.fadeIn()
                    bubble.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    expandedViewHolder = this@BaseMessageViewHolder
                }
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onReactionClicked?.invoke(message, REACTIONS.first()); return true
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

    inner class OutgoingMessageViewHolder(private val binding: ItemOutgoingMessageBinding) : BaseMessageViewHolder(binding) {
        override val bubble = binding.bubbleLayout
        override val reactionPanel = binding.reactionPanel
        override val messageContent = binding.messageContent
        override val reactionsLayout = binding.reactionGroup
        override val tvTimestamp = binding.tvTimestamp

        override fun bind(message: Message) {
            super.bind(message)
            val statusIconRes = when (message.status) {
                "read" -> R.drawable.ic_read_receipt
                "delivered" -> R.drawable.ic_delivered_receipt
                "sent", "sending" -> R.drawable.ic_sending
                else -> 0
            }
            if (statusIconRes != 0) {
                val statusDrawable = ContextCompat.getDrawable(itemView.context, statusIconRes)
                binding.tvStatus.setCompoundDrawablesWithIntrinsicBounds(null, null, statusDrawable, null)
                binding.tvStatus.isVisible = true
            } else {
                binding.tvStatus.isVisible = false
            }
        }

        override fun bindQuote(message: Message) {
            val isQuoted = message.quotedMessageId != null
            binding.replyView.root.isVisible = isQuoted
            if (isQuoted) {
                binding.replyView.tvReplySender.text = message.name
                binding.replyView.tvReplyText.text = message.quotedMessageText ?: "Media"
            }
        }
    }

    inner class IncomingMessageViewHolder(private val binding: ItemIncomingMessageBinding) : BaseMessageViewHolder(binding) {
        override val bubble = binding.bubbleLayout
        override val reactionPanel = binding.reactionPanel
        override val messageContent = binding.messageContent
        override val reactionsLayout = binding.reactionGroup
        override val tvTimestamp = binding.tvTimestamp

        override fun bind(message: Message) {
            super.bind(message)
            binding.tvSenderName.isVisible = isGroup
            if(isGroup) {
                binding.tvSenderName.text = message.senderName
            }
        }

        override fun bindQuote(message: Message) {
            val isQuoted = message.quotedMessageId != null
            binding.replyView.root.isVisible = isQuoted
            if (isQuoted) {
                binding.replyView.tvReplySender.text = message.name
                binding.replyView.tvReplyText.text = message.quotedMessageText ?: "Media"
            }
        }
    }

    class DividerViewHolder(private val binding: ItemDividerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatListItem.DividerItem) {
            binding.tvDivider.text = DateUtils.getRelativeTimeSpanString(item.timestamp, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_SHOW_WEEKDAY)
        }
    }

    class WarningViewHolder(binding: ItemWarningBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatListItem.DividerItem -> VIEW_TYPE_DIVIDER
            is ChatListItem.MessageItem -> if (item.message.isOutgoing) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
            is ChatListItem.WarningItem -> VIEW_TYPE_WARNING
        }
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
            is ChatListItem.MessageItem -> (holder as BaseMessageViewHolder).bind(item.message)
            is ChatListItem.DividerItem -> (holder as DividerViewHolder).bind(item)
            is ChatListItem.WarningItem -> {}
        }
    }
}
