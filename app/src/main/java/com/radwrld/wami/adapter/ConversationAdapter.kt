// @path: app/src/main/java/com/radwrld/wami/adapter/ConversationAdapter.kt
package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.radwrld.wami.R
import com.radwrld.wami.databinding.ItemConversationBinding
import com.radwrld.wami.model.Contact // <<< CHANGE 1: Import Contact instead of Message

//                                                                      vvvvvvv
class ConversationAdapter(
    private val contacts: List<Contact>, // <<< CHANGE 2: The list now holds Contacts
    private val onItemClicked: (Contact) -> Unit, // <<< CHANGE 3: The listener provides a Contact
    private val onItemLongClicked: (Contact, Int) -> Unit // <<< CHANGE 4: This listener also provides a Contact
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    inner class ConversationViewHolder(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root) {

        // This function now binds a Contact object to the view
        fun bind(contact: Contact) {
            binding.tvContactName.text = contact.name
            // The Contact model doesn't have a "last message", so we use a placeholder.
            binding.tvLastMessage.text = "Tap to start chatting"

            // Use Glide to load the contact's avatar image
            // Make sure you have an ImageView with the id 'ivAvatar' in your item_conversation.xml layout
            Glide.with(binding.root.context)
                .load(contact.avatarUrl)
                .placeholder(R.drawable.ic_profile_placeholder) // IMPORTANT: Create a placeholder drawable
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.ivAvatar) // Make sure this ID matches your layout XML

            binding.root.setOnClickListener {
                onItemClicked(contact)
            }

            binding.root.setOnLongClickListener {
                onItemLongClicked(contact, adapterPosition)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        // Pass the Contact at the current position to the bind function
        holder.bind(contacts[position]) // <<< CHANGE 5
    }

    override fun getItemCount() = contacts.size // <<< CHANGE 6
}
