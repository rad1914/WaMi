package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.radwrld.wami.R
import com.radwrld.wami.databinding.ItemConversationBinding
import com.radwrld.wami.model.Contact
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ConversationAdapter(
    private val onItemClicked: (Contact) -> Unit,
    private val onItemLongClicked: (Contact, View) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    // The adapter no longer holds a direct reference to the list in its constructor.
    // It's managed internally and updated via submitList.
    private var conversations = emptyList<Contact>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount() = conversations.size

    inner class ConversationViewHolder(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            // --- DATA BINDING ---
            binding.tvContactName.text = contact.name
            binding.tvLastMessage.text = contact.lastMessage ?: "Tap to start chatting"

            // --- UNREAD COUNT ---
            if (contact.unreadCount > 0) {
                binding.tvUnreadCount.visibility = View.VISIBLE
                binding.tvUnreadCount.text = contact.unreadCount.toString()
            } else {
                binding.tvUnreadCount.visibility = View.GONE
            }

            // --- TIMESTAMP ---
            binding.tvTimestamp.text = contact.lastMessageTimestamp?.let {
                formatTimestamp(it)
            } ?: ""

            // --- AVATAR IMAGE ---
            Glide.with(binding.root.context)
                .load(contact.avatarUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop() // Use circleCrop for a typical avatar look
                .into(binding.ivAvatar)

            // --- CLICK LISTENERS ---
            itemView.setOnClickListener {
                onItemClicked(contact)
            }
            itemView.setOnLongClickListener {
                onItemLongClicked(contact, it)
                true
            }
        }
    }

    /**
     * Helper function to format the timestamp into a user-friendly string like
     * "10:45 PM", "Yesterday", or "06/12/2025".
     */
    private fun formatTimestamp(timestamp: Long): String {
        val messageCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()

        return when {
            now.get(Calendar.DATE) == messageCal.get(Calendar.DATE) -> {
                // Today: "10:45 PM"
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(messageCal.time)
            }
            now.get(Calendar.DATE) - messageCal.get(Calendar.DATE) == 1 -> {
                // Yesterday
                "Yesterday"
            }
            else -> {
                // Older: "MM/dd/yy"
                SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(messageCal.time)
            }
        }
    }

    /**
     * Submits a new list to the adapter and lets DiffUtil calculate the most
     * efficient way to update the RecyclerView.
     */
    fun submitList(newConversations: List<Contact>) {
        val diffCallback = ConversationDiffCallback(this.conversations, newConversations)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.conversations = newConversations
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * DiffUtil.Callback to efficiently update the contacts list.
     */
    private class ConversationDiffCallback(
        private val oldList: List<Contact>,
        private val newList: List<Contact>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
