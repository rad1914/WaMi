// @path: app/src/main/java/com/radwrld/wami/adapter/MessageAdapter.kt
package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.R
import com.radwrld.wami.databinding.ItemMessageBinding // This binding might need renaming to ItemConversationBinding
import com.radwrld.wami.model.Contact // This adapter now correctly uses the Contact model
import com.squareup.picasso.Picasso

class MessageAdapter(
    private val items: List<Contact>,
    private val itemClickListener: ((Contact) -> Unit)? = null
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(contact: Contact) {
            binding.tvName.text = contact.name
            binding.tvLastMessage.text = contact.lastMessage
            
            if (!contact.avatarUrl.isNullOrBlank()) {
                Picasso.get().load(contact.avatarUrl).into(binding.avatarImageView)
            } else {
                binding.avatarImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }

            binding.root.setOnClickListener {
                itemClickListener?.invoke(contact)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
