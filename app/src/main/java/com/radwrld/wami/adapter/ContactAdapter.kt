// @path: app/src/main/java/com/radwrld/wami/adapter/ContactAdapter.kt
// adapter/ContactAdapter.kt

package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.radwrld.wami.R
import com.radwrld.wami.databinding.ItemContactBinding
import com.radwrld.wami.model.Contact

class ContactAdapter(
    private val contacts: List<Contact>,
    private val onContactClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact) {
            binding.tvContactName.text = contact.name
            binding.tvContactNumber.text = contact.phoneNumber
            Glide.with(binding.root.context)
                .load(contact.avatarUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
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
}
