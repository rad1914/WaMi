// adapter/ChatAdapter.kt
package com.radwrld.wami.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.R
import com.radwrld.wami.model.Message

class ChatAdapter(
    private val items: List<Message>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_INCOMING = 0
        private const val TYPE_OUTGOING = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isOutgoing) TYPE_OUTGOING else TYPE_INCOMING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_OUTGOING) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_outgoing, parent, false)
            OutgoingHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_incoming, parent, false)
            IncomingHolder(v)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = items[position]
        when (holder) {
            is OutgoingHolder -> holder.tvMessage.text = msg.lastMessage
            is IncomingHolder -> holder.tvMessage.text = msg.lastMessage
        }
    }

    class IncomingHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessageIn)
    }

    class OutgoingHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessageOut)
    }
}
