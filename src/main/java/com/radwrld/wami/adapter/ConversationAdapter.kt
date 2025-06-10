// app/src/main/java/com/radwrld/wami/adapter/ConversationAdapter.kt
package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.databinding.ItemConversationBinding // NOTE: Create this binding from item_conversation.xml
import com.radwrld.wami.model.Chat

class ConversationAdapter(
    private val chats: List<Chat>,
    private val onItemClicked: (Chat) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    // You need to create a layout file named `item_conversation.xml`.
    // It should contain at least:
    // - A TextView with the id `tvContactName`
    // - A TextView with the id `tvLastMessage`
    // - An ImageView with the id `ivAvatar` (optional, for displaying avatar)
    inner class ConversationViewHolder(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Chat) {
            binding.tvContactName.text = chat.contactName
            binding.tvLastMessage.text = chat.lastMessage
            // Here you would load the avatar using a library like Glide or Picasso
            // For example: Glide.with(binding.root.context).load(chat.avatarUrl).into(binding.ivAvatar)
            binding.root.setOnClickListener {
                onItemClicked(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount() = chats.size
}
