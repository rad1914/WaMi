package com.radwrld.wami.adapter;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\f\u0012\b\u0012\u00060\u0002R\u00020\u00000\u0001:\u0001\u0017B\u0013\u0012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0005J\b\u0010\n\u001a\u00020\u000bH\u0016J\u001c\u0010\f\u001a\u00020\b2\n\u0010\r\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u000e\u001a\u00020\u000bH\u0016J\u001c\u0010\u000f\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u000bH\u0016J\u0016\u0010\u0013\u001a\u00020\b2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0015R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lcom/radwrld/wami/adapter/ChatAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/radwrld/wami/adapter/ChatAdapter$MessageViewHolder;", "messages", "", "Lcom/radwrld/wami/model/Message;", "(Ljava/util/List;)V", "addMessage", "", "message", "getItemCount", "", "onBindViewHolder", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "updateStatus", "msgId", "", "newStatus", "MessageViewHolder", "app_debug"})
public final class ChatAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.radwrld.wami.adapter.ChatAdapter.MessageViewHolder> {
    private final java.util.List<com.radwrld.wami.model.Message> messages = null;
    
    public ChatAdapter(@org.jetbrains.annotations.NotNull
    java.util.List<com.radwrld.wami.model.Message> messages) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    @java.lang.Override
    public com.radwrld.wami.adapter.ChatAdapter.MessageViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull
    com.radwrld.wami.adapter.ChatAdapter.MessageViewHolder holder, int position) {
    }
    
    @java.lang.Override
    public int getItemCount() {
        return 0;
    }
    
    public final void addMessage(@org.jetbrains.annotations.NotNull
    com.radwrld.wami.model.Message message) {
    }
    
    public final void updateStatus(@org.jetbrains.annotations.NotNull
    java.lang.String msgId, @org.jetbrains.annotations.NotNull
    java.lang.String newStatus) {
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/radwrld/wami/adapter/ChatAdapter$MessageViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "binding", "Lcom/radwrld/wami/databinding/ItemChatMessageBinding;", "(Lcom/radwrld/wami/adapter/ChatAdapter;Lcom/radwrld/wami/databinding/ItemChatMessageBinding;)V", "getBinding", "()Lcom/radwrld/wami/databinding/ItemChatMessageBinding;", "app_debug"})
    public final class MessageViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull
        private final com.radwrld.wami.databinding.ItemChatMessageBinding binding = null;
        
        public MessageViewHolder(@org.jetbrains.annotations.NotNull
        com.radwrld.wami.databinding.ItemChatMessageBinding binding) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull
        public final com.radwrld.wami.databinding.ItemChatMessageBinding getBinding() {
            return null;
        }
    }
}