// @path: app/src/main/java/com/radwrld/wami/adapter/ChatAdapter.kt
package com.radwrld.wami.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.databinding.ItemIncomingMessageBinding
import com.radwrld.wami.databinding.ItemOutgoingMessageBinding
import com.radwrld.wami.model.Message

class ChatAdapter(private val messages: MutableList<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_OUTGOING = 1
        private const val VIEW_TYPE_INCOMING = 2
    }

    inner class OutgoingMessageViewHolder(val binding: ItemOutgoingMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            // The listener now controls the visibility of the info_container
            setupListeners(binding.bubbleLayout, binding.infoContainer)
        }

        fun bind(message: Message) {
            binding.tvMessage.text = message.text
            // These views are now part of the hidden info_container, but their text still needs to be set
            binding.tvTimestamp.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
            binding.tvStatus.text = message.status
        }
    }

    inner class IncomingMessageViewHolder(val binding: ItemIncomingMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            // The listener now controls the visibility of the info_container
            setupListeners(binding.bubbleLayout, binding.infoContainer)
        }

        fun bind(message: Message) {
            binding.tvMessage.text = message.text
            // This view is now part of the hidden info_container, but its text still needs to be set
            binding.tvTimestamp.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
        }
    }

    /**
     * A helper function to set up the required listeners on a message bubble.
     * It toggles the visibility of the passed-in "infoView".
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners(bubbleView: View, infoView: View) {
        // Show the info view on a long press
        bubbleView.setOnLongClickListener {
            infoView.visibility = View.VISIBLE
            true // Consume the long click event
        }

        // Hide the info view when the touch is released
        bubbleView.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP || motionEvent.action == MotionEvent.ACTION_CANCEL) {
                infoView.visibility = View.GONE
            }
            // Return false so the event is not consumed and can be passed to other listeners
            // like OnLongClickListener.
            false
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isOutgoing) {
            VIEW_TYPE_OUTGOING
        } else {
            VIEW_TYPE_INCOMING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_OUTGOING) {
            val binding = ItemOutgoingMessageBinding.inflate(inflater, parent, false)
            OutgoingMessageViewHolder(binding)
        } else {
            val binding = ItemIncomingMessageBinding.inflate(inflater, parent, false)
            IncomingMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder.itemViewType == VIEW_TYPE_OUTGOING) {
            (holder as OutgoingMessageViewHolder).bind(msg)
        } else {
            (holder as IncomingMessageViewHolder).bind(msg)
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateStatus(msgId: String, newStatus: String) {
        messages.indexOfFirst { it.id == msgId }.takeIf { it != -1 }?.let { idx ->
            messages[idx].status = newStatus
            notifyItemChanged(idx)
        }
    }
}
