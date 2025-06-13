// @path: app/src/main/java/com/radwrld/wami/adapter/ConversationAdapter.kt
package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.radwrld.wami.R
import com.radwrld.wami.databinding.ItemConversationBinding
import com.radwrld.wami.model.Contact

class ConversationAdapter(
    private val contacts: List<Contact>,
    private val onItemClicked: (Contact) -> Unit,
    private val onItemLongClicked: (Contact, Int) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    inner class ConversationViewHolder(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.tvContactName.text = contact.name
            binding.tvLastMessage.text = "Tap to start chatting"

            Glide.with(binding.root.context)
                .load(contact.avatarUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.ivAvatar)

            binding.root.setOnClickListener {
                onItemClicked(contact)
            }

            binding.root.setOnLongClickListener {
                // Use bindingAdapterPosition instead of the deprecated adapterPosition
                onItemLongClicked(contact, bindingAdapterPosition)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount() = contacts.size
}
