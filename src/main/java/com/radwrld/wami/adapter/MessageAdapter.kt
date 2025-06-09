// MessageAdapter.kt
package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.R
import com.radwrld.wami.databinding.ItemMessageBinding
import com.radwrld.wami.model.Message
import com.squareup.picasso.Picasso

class MessageAdapter(
    private val items: List<Message>,
    private val itemClickListener: ((Message) -> Unit)? = null
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvName.text = message.name
            binding.tvLastMessage.text = message.lastMessage
            if (message.avatarUrl.isNotBlank()) {
                Picasso.get().load(message.avatarUrl).into(binding.avatarImageView)
            } else {
                binding.avatarImageView.setImageResource(R.drawable.ic_placeholder_avatar)
            }

            binding.root.setOnClickListener {
                itemClickListener?.invoke(message)
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
