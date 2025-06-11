// app/src/main/java/com/radwrld/wami/adapter/ConversationAdapter.kt
package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.databinding.ItemConversationBinding
import com.radwrld.wami.model.Message

class ConversationAdapter(
    private val conversations: List<Message>,
    private val onItemClicked: (Message) -> Unit,
    private val onItemLongClicked: (Message, Int) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    inner class ConversationViewHolder(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvContactName.text = message.name
            binding.tvLastMessage.text = message.lastMessage

            binding.root.setOnClickListener {
                onItemClicked(message)
            }

            binding.root.setOnLongClickListener {
                onItemLongClicked(message, adapterPosition)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount() = conversations.size
}
