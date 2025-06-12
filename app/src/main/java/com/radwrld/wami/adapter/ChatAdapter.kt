// @path: app/src/main/java/com/radwrld/wami/adapter/ChatAdapter.kt
package com.radwrld.wami.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.radwrld.wami.databinding.ItemIncomingMessageBinding
import com.radwrld.wami.databinding.ItemOutgoingMessageBinding
import com.radwrld.wami.model.Message

class ChatAdapter(private val messages: MutableList<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_OUTGOING = 1
        private const val VIEW_TYPE_INCOMING = 2
    }

    inner class OutgoingMessageViewHolder(val binding: ItemOutgoingMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            setupListeners(binding.bubbleLayout, binding.infoContainer)
        }
        fun bind(message: Message) {
            binding.tvMessage.text = message.text
            binding.tvTimestamp.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
            binding.tvStatus.text = message.status
        }
    }

    inner class IncomingMessageViewHolder(val binding: ItemIncomingMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            setupListeners(binding.bubbleLayout, binding.infoContainer)
        }
        fun bind(message: Message) {
            binding.tvMessage.text = message.text
            binding.tvTimestamp.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
            if (!message.senderName.isNullOrEmpty()) {
                binding.tvSenderName.text = message.senderName
                binding.tvSenderName.visibility = View.VISIBLE
            } else {
                binding.tvSenderName.visibility = View.GONE
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners(bubbleView: View, infoView: View) {
        bubbleView.setOnLongClickListener {
            infoView.visibility = View.VISIBLE
            true
        }
        bubbleView.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP || motionEvent.action == MotionEvent.ACTION_CANCEL) {
                infoView.visibility = View.GONE
            }
            false
        }
    }

    override fun getItemViewType(int: Int): Int {
        return if (messages[int].isOutgoing) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_OUTGOING) {
            val binding = ItemOutgoingMessageBinding.inflate(inflater, parent, false)
            OutgoingMessageViewHolder(binding)
        } else {
            val binding = ItemIncomingMessageBinding.inflate(inflater, parent, false)
            IncomingMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder.itemViewType == VIEW_TYPE_OUTGOING) {
            (holder as OutgoingMessageViewHolder).bind(msg)
        } else {
            (holder as IncomingMessageViewHolder).bind(msg)
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateStatus(msgId: String, newStatus: String) {
        messages.indexOfFirst { it.id == msgId }.takeIf { it != -1 }?.let { idx ->
            messages[idx].status = newStatus
            notifyItemChanged(idx)
        }
    }
}