// adapter/ChatAdapter.kt
package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.databinding.ItemChatMessageBinding
import com.radwrld.wami.model.Message
import com.radwrld.wami.R

class ChatAdapter(private val messages: MutableList<Message>) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MessageViewHolder(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        with(holder.binding) {
            val msg = messages[position]
            tvMessage.text = msg.text
            tvTimestamp.text = android.text.format.DateFormat.format("hh:mm a", msg.timestamp)
            tvStatus.text = msg.status
            tvMessage.setBackgroundResource(
                if (msg.isOutgoing) R.drawable.bg_outgoing_message else R.drawable.bg_incoming_message
            )
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
