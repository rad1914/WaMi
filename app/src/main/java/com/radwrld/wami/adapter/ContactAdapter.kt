// @path: app/src/main/java/com/radwrld/wami/adapter/ContactAdapter.kt
package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.radwrld.wami.databinding.ItemContactBinding
import com.radwrld.wami.network.Contact

class ContactAdapter(
    private val onClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    private var items = emptyList<Contact>()

    inner class ViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact) = with(binding) {
            tvContactName.text = contact.name
            tvContactNumber.text = contact.phoneNumber
            Glide.with(root.context)
                .load(contact.avatarUrl)
                .circleCrop()
                .into(ivContactAvatar)
            root.setOnClickListener { onClick(contact) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size

    fun submitList(newItems: List<Contact>) {
        items = newItems
        notifyDataSetChanged()
    }
}
