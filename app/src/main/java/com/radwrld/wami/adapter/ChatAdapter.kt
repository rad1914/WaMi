// @path: app/src/main/java/com/radwrld/wami/adapter/ChatAdapter.kt
package com.radwrld.wami.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.method.LinkMovementMethod
import android.text.format.DateUtils
import android.text.util.Linkify
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.radwrld.wami.R
import com.radwrld.wami.databinding.ItemIncomingMessageBinding
import com.radwrld.wami.databinding.ItemOutgoingMessageBinding
import com.radwrld.wami.model.Message
import java.util.Calendar

// Sealed class to represent different items in our chat list
sealed class ChatListItem {
    object WarningItem : ChatListItem()
    data class MessageItem(val message: Message) : ChatListItem()
    data class DividerItem(val timestamp: Long, val isNewDay: Boolean) : ChatListItem()
}

class ChatAdapter(private val chatItems: MutableList<ChatListItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            binding.tvMessage.text = message.text
            binding.tvTimestamp.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
            binding.tvStatus.text = message.status
        }
    }

    inner class IncomingMessageViewHolder(val binding: ItemIncomingMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            setupListeners(binding.bubbleLayout, binding.infoContainer)
        }
        fun bind(message: Message) {
            binding.tvMessage.text = message.text
            binding.tvTimestamp.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
            if (!message.senderName.isNullOrEmpty()) {
                binding.tvSenderName.text = message.senderName
                binding.tvSenderName.visibility = View.VISIBLE
            } else {
                binding.tvSenderName.visibility = View.GONE
            }
        }
    }

    inner class DividerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: ChatListItem.DividerItem) {
            val textView = (itemView as ViewGroup).getChildAt(0) as TextView
            textView.text = formatDividerTimestamp(item.timestamp, item.isNewDay)
        }

        private fun formatDividerTimestamp(timestamp: Long, isNewDay: Boolean): String {
            return if (isNewDay) {
                DateUtils.getRelativeTimeSpanString(timestamp, Calendar.getInstance().timeInMillis, DateUtils.DAY_IN_MILLIS).toString()
            } else {
                android.text.format.DateFormat.format("hh:mm a", timestamp).toString()
            }
        }
    }

    inner class WarningViewHolder(view: View) : RecyclerView.ViewHolder(view)
    
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
        return when (chatItems[position]) {
            is ChatListItem.WarningItem -> VIEW_TYPE_WARNING
            is ChatListItem.DividerItem -> VIEW_TYPE_DIVIDER
            is ChatListItem.MessageItem -> if ((chatItems[position] as ChatListItem.MessageItem).message.isOutgoing) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val context = parent.context
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
                val textView = TextView(context).apply {
                    gravity = Gravity.CENTER
                    val hPadding = (8 * resources.displayMetrics.density).toInt()
                    val vPadding = (4 * resources.displayMetrics.density).toInt()
                    setPadding(hPadding, vPadding, hPadding, vPadding)

                    val textColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.WHITE)
                    setTextColor(textColor)

                    val backgroundColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, Color.DKGRAY)
                    val backgroundDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(backgroundColor)
                        cornerRadius = 20 * resources.displayMetrics.density
                    }
                    background = backgroundDrawable
                }
                val wrapper = android.widget.FrameLayout(context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    ).apply{
                        val verticalMargin = (8 * resources.displayMetrics.density).toInt()
                        setMargins(0, verticalMargin, 0, verticalMargin)
                    }
                    addView(textView, android.widget.FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_HORIZONTAL
                    ))
                }
                DividerViewHolder(wrapper)
            }
            VIEW_TYPE_WARNING -> {
                val textView = TextView(context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    ).apply {
                        val margin = (8 * resources.displayMetrics.density).toInt()
                        setMargins(margin, margin, margin, margin)
                    }
                    val padding = (12 * resources.displayMetrics.density).toInt()
                    setPadding(padding, padding, padding, padding)
                    
                    val backgroundColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorTertiaryContainer, Color.YELLOW)
                    val backgroundDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(backgroundColor)
                        cornerRadius = (8 * resources.displayMetrics.density)
                    }
                    background = backgroundDrawable

                    val textColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnTertiaryContainer, Color.BLACK)
                    setTextColor(textColor)
                    
                    textSize = 14f
                    text = context.getString(R.string.chat_warning_message)
                    Linkify.addLinks(this, Linkify.WEB_URLS)
                    movementMethod = LinkMovementMethod.getInstance()
                }
                WarningViewHolder(textView)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = chatItems[position]) {
            is ChatListItem.MessageItem -> {
                if (holder.itemViewType == VIEW_TYPE_OUTGOING) {
                    (holder as OutgoingMessageViewHolder).bind(item.message)
                } else {
                    (holder as IncomingMessageViewHolder).bind(item.message)
                }
            }
            is ChatListItem.DividerItem -> {
                (holder as DividerViewHolder).bind(item)
            }
            is ChatListItem.WarningItem -> {
                // ViewHolder is static, no binding needed.
            }
        }
    }

    override fun getItemCount() = chatItems.size

    fun updateStatus(msgId: String, newStatus: String, newId: String? = null) {
        val index = chatItems.indexOfFirst { it is ChatListItem.MessageItem && it.message.id == msgId }
        if (index != -1) {
            val item = chatItems[index] as ChatListItem.MessageItem
            item.message.status = newStatus
            if (newId != null) {
                item.message.id = newId
            }
            notifyItemChanged(index)
        }
    }
}
