// @path: app/src/main/java/com/radwrld/wami/adapter/SearchResultAdapter.kt
package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.R
import com.radwrld.wami.network.Contact
import com.radwrld.wami.ui.viewmodel.SearchResultItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val VIEW_TYPE_CONTACT = 1
private const val VIEW_TYPE_MESSAGE = 2

class SearchResultAdapter(
    private val onItemClicked: (contact: Contact) -> Unit
) : ListAdapter<SearchResultItem, RecyclerView.ViewHolder>(SearchDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchResultItem.ContactItem -> VIEW_TYPE_CONTACT
            is SearchResultItem.MessageItem -> VIEW_TYPE_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_CONTACT -> {
                val view = inflater.inflate(R.layout.item_search_contact_result, parent, false)
                ContactResultViewHolder(view)
            }
            VIEW_TYPE_MESSAGE -> {
                val view = inflater.inflate(R.layout.item_search_message_result, parent, false)
                MessageResultViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SearchResultItem.ContactItem -> (holder as ContactResultViewHolder).bind(item.contact)
            is SearchResultItem.MessageItem -> (holder as MessageResultViewHolder).bind(item)
        }
    }

    
    inner class ContactResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contactName: TextView = itemView.findViewById(R.id.tvContactName)
        private val contactIdentifier: TextView = itemView.findViewById(R.id.tvContactIdentifier)

        fun bind(contact: Contact) {
            contactName.text = contact.name
            contactIdentifier.text = "Contacto"
            itemView.setOnClickListener { onItemClicked(contact) }
        }
    }

    
    inner class MessageResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatName: TextView = itemView.findViewById(R.id.tvChatName)
        private val messageContent: TextView = itemView.findViewById(R.id.tvMessageContent)
        private val messageDate: TextView = itemView.findViewById(R.id.tvMessageDate)

        fun bind(item: SearchResultItem.MessageItem) {
            chatName.text = item.contact.name
            messageContent.text = item.message.text
            messageDate.text = dateFormat.format(Date(item.message.timestamp))
            itemView.setOnClickListener { onItemClicked(item.contact) }
        }
    }
}

class SearchDiffCallback : DiffUtil.ItemCallback<SearchResultItem>() {
    override fun areItemsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem): Boolean {
        return when {
            oldItem is SearchResultItem.ContactItem && newItem is SearchResultItem.ContactItem ->
                oldItem.contact.id == newItem.contact.id
            oldItem is SearchResultItem.MessageItem && newItem is SearchResultItem.MessageItem ->
                oldItem.message.id == newItem.message.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem): Boolean {
        return oldItem == newItem
    }
}
