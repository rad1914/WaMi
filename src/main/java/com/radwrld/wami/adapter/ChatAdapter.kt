package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.databinding.ItemChatMessageBinding
import com.radwrld.wami.model.Message
import com.radwrld.wami.R

class ChatAdapter(
    private val messages: MutableList<Message>
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        with(holder.binding) {
            tvMessage.text = msg.text
            tvTimestamp.text = android.text.format.DateFormat.format("hh:mm a", msg.timestamp)
            tvStatus.text = msg.status

            // Example styling per outgoing vs. incoming:
            root.apply {
                if (msg.isOutgoing) {
                    // Align to the end, change background, etc.
                    tvMessage.setBackgroundResource(R.drawable.bg_outgoing_message)
                } else {
                    // Align to the start, other style
                    tvMessage.setBackgroundResource(R.drawable.bg_incoming_message)
                }
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    /** Adds a new message to the bottom and scrolls to it */
    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    /** Finds a message by its id, updates status, and refreshes that item */
    fun updateStatus(msgId: String, newStatus: String) {
        val idx = messages.indexOfFirst { it.id == msgId }
        if (idx != -1) {
            messages[idx].status = newStatus
            notifyItemChanged(idx)
        }
    }
}
