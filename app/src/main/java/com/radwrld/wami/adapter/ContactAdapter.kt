// @path: app/src/main/java/com/radwrld/wami/adapter/ContactAdapter.kt

package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.radwrld.wami.R
import com.radwrld.wami.databinding.ItemContactBinding
import com.radwrld.wami.model.Contact

class ContactAdapter(
    // The list is no longer passed in the constructor.
    private val onContactClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    // The adapter manages its own list internally.
    private var contacts = emptyList<Contact>()

    inner class ContactViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact) {
            // The binding logic for a single item remains the same.
            // NOTE: Ensure your item_contact.xml has views with these exact IDs.
            binding.tvContactName.text = contact.name
            binding.tvContactNumber.text = contact.phoneNumber
            Glide.with(binding.root.context)
                .load(contact.avatarUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop() // Added for a better avatar look
                .into(binding.ivContactAvatar)

            binding.root.setOnClickListener { onContactClick(contact) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    /**
     * The new, efficient way to update the adapter's data.
     */
    fun submitList(newContacts: List<Contact>) {
        val diffCallback = ContactDiffCallback(this.contacts, newContacts)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        // Update the internal list and dispatch the calculated updates.
        this.contacts = newContacts
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * A private helper class that tells DiffUtil how to compute the differences
     * between two lists.
     */
    private class ContactDiffCallback(
        private val oldList: List<Contact>,
        private val newList: List<Contact>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        // DiffUtil first checks if items are the same. A unique ID is perfect for this.
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        // If items are the same, DiffUtil checks if their contents have changed.
        // The data class's generated equals() method handles this perfectly.
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
