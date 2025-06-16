// @path: app/src/main/java/com/radwrld/wami/adapter/ConversationAdapter.kt
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
import com.radwrld.wami.util.TextFormatter
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val onItemClicked: (Contact) -> Unit,
    private val onItemLongClicked: (Contact, View) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    private var conversations = emptyList<Contact>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ConversationViewHolder(
            ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) =
        holder.bind(conversations[position])

    override fun getItemCount() = conversations.size

    fun submitList(newList: List<Contact>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = conversations.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(o: Int, n: Int) = conversations[o].id == newList[n].id
            override fun areContentsTheSame(o: Int, n: Int) = conversations[o] == newList[n]
        })
        conversations = newList
        diff.dispatchUpdatesTo(this)
    }

    inner class ConversationViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) = with(binding) {
            tvContactName.text = contact.name
            
            tvLastMessage.text = if (contact.lastMessage != null) {
                TextFormatter.format(root.context, contact.lastMessage)
            } else {
                "Tap to start chatting"
            }

            tvUnreadCount.apply {
                visibility = if (contact.unreadCount > 0) View.VISIBLE else View.GONE
                text = contact.unreadCount.toString()
            }
            tvTimestamp.text = contact.lastMessageTimestamp?.let(::formatTimestamp).orEmpty()

            // ++ Applied suggestion: Use the contact's isGroup flag to determine the correct placeholder.
            val placeholder = if (contact.isGroup) {
                R.drawable.ic_group_placeholder
            } else {
                R.drawable.ic_profile_placeholder
            }
            
            Glide.with(root.context)
                .load(contact.avatarUrl)
                .placeholder(placeholder)
                .error(placeholder)
                .circleCrop()
                .into(ivAvatar)

            root.setOnClickListener { onItemClicked(contact) }
            root.setOnLongClickListener {
                onItemLongClicked(contact, it)
                true
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()
        return when {
            now.get(Calendar.DATE) == messageDate.get(Calendar.DATE) ->
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
            now.get(Calendar.DATE) - messageDate.get(Calendar.DATE) == 1 -> "Yesterday"
            else -> SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
