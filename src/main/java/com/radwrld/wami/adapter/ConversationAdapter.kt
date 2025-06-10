// app/src/main/java/com/radwrld/wami/adapter/ConversationAdapter.kt
package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.databinding.ItemConversationBinding
import com.radwrld.wami.model.Message // CHANGED: Import Message instead of Chat

class ConversationAdapter(
    // CHANGED: The list now holds Message objects
    private val conversations: List<Message>,
    private val onItemClicked: (Message) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    inner class ConversationViewHolder(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root) {
        // CHANGED: The bind function now accepts a Message object
        fun bind(message: Message) {
            // CHANGED: Use properties from the Message model
            binding.tvContactName.text = message.name
            binding.tvLastMessage.text = message.lastMessage
            // Example for Glide/Picasso would now use message.avatarUrl
            binding.root.setOnClickListener {
                onItemClicked(message)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position]) // CHANGED: Pass item from the new list
    }

    override fun getItemCount() = conversations.size // CHANGED: Get size of the new list
}
