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
import com.radwrld.wami.network.Contact
import com.radwrld.wami.ui.TextFormatter
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val onItemClicked: (Contact) -> Unit,
    private val onItemLongClicked: (Contact, View) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    private var conversations = emptyList<Contact>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount() = conversations.size

    fun submitList(newList: List<Contact>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = conversations.size
            override fun getNewListSize(): Int = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return conversations[oldItemPosition].id == newList[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return conversations[oldItemPosition] == newList[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        conversations = newList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ConversationViewHolder(private val b: ItemConversationBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(contact: Contact) {

            b.tvName.text = contact.name
            b.tvLastMessage.text = contact.lastMessage?.let {
                TextFormatter.format(b.root.context, it)
            } ?: "Tap to start chatting"

            b.tvUnreadCount.visibility = if (contact.unreadCount > 0) View.VISIBLE else View.GONE
            b.tvUnreadCount.text = contact.unreadCount.toString()

            b.tvTimestamp.text = contact.lastMessageTimestamp?.let(::formatTimestamp).orEmpty()

            val placeholder = if (contact.isGroup)
                R.drawable.ic_group_placeholder else R.drawable.ic_profile_placeholder

            Glide.with(b.root.context)
                .load(contact.avatarUrl)
                .placeholder(placeholder)
                .error(placeholder)
                .into(b.avatarImageView)

            b.root.setOnClickListener { onItemClicked(contact) }
            b.root.setOnLongClickListener {
                onItemLongClicked(contact, it)
                true
            }
        }
    }

    private fun formatTimestamp(ts: Long): String {
        val msgCal = Calendar.getInstance().apply { timeInMillis = ts }
        val now = Calendar.getInstance()
        return when {
            now.get(Calendar.DATE) == msgCal.get(Calendar.DATE) ->
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
            now.get(Calendar.DATE) - msgCal.get(Calendar.DATE) == 1 -> "Yesterday"
            else -> SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(ts))
        }
    }
}
