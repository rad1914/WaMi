package com.radwrld.wami.adapter;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\f\u0012\b\u0012\u00060\u0002R\u00020\u00000\u0001:\u0001\u0015BA\u0012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u0012\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\b0\u0007\u0012\u0018\u0010\t\u001a\u0014\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\b0\n\u00a2\u0006\u0002\u0010\fJ\b\u0010\r\u001a\u00020\u000bH\u0016J\u001c\u0010\u000e\u001a\u00020\b2\n\u0010\u000f\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u0010\u001a\u00020\u000bH\u0016J\u001c\u0010\u0011\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u000bH\u0016R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\t\u001a\u0014\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lcom/radwrld/wami/adapter/ConversationAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/radwrld/wami/adapter/ConversationAdapter$ConversationViewHolder;", "conversations", "", "Lcom/radwrld/wami/model/Message;", "onItemClicked", "Lkotlin/Function1;", "", "onItemLongClicked", "Lkotlin/Function2;", "", "(Ljava/util/List;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function2;)V", "getItemCount", "onBindViewHolder", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "ConversationViewHolder", "app_debug"})
public final class ConversationAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.radwrld.wami.adapter.ConversationAdapter.ConversationViewHolder> {
    private final java.util.List<com.radwrld.wami.model.Message> conversations = null;
    private final kotlin.jvm.functions.Function1<com.radwrld.wami.model.Message, kotlin.Unit> onItemClicked = null;
    private final kotlin.jvm.functions.Function2<com.radwrld.wami.model.Message, java.lang.Integer, kotlin.Unit> onItemLongClicked = null;
    
    public ConversationAdapter(@org.jetbrains.annotations.NotNull
    java.util.List<com.radwrld.wami.model.Message> conversations, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function1<? super com.radwrld.wami.model.Message, kotlin.Unit> onItemClicked, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function2<? super com.radwrld.wami.model.Message, ? super java.lang.Integer, kotlin.Unit> onItemLongClicked) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    @java.lang.Override
    public com.radwrld.wami.adapter.ConversationAdapter.ConversationViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull
    com.radwrld.wami.adapter.ConversationAdapter.ConversationViewHolder holder, int position) {
    }
    
    @java.lang.Override
    public int getItemCount() {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u000b"}, d2 = {"Lcom/radwrld/wami/adapter/ConversationAdapter$ConversationViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "binding", "Lcom/radwrld/wami/databinding/ItemConversationBinding;", "(Lcom/radwrld/wami/adapter/ConversationAdapter;Lcom/radwrld/wami/databinding/ItemConversationBinding;)V", "getBinding", "()Lcom/radwrld/wami/databinding/ItemConversationBinding;", "bind", "", "message", "Lcom/radwrld/wami/model/Message;", "app_debug"})
    public final class ConversationViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull
        private final com.radwrld.wami.databinding.ItemConversationBinding binding = null;
        
        public ConversationViewHolder(@org.jetbrains.annotations.NotNull
        com.radwrld.wami.databinding.ItemConversationBinding binding) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull
        public final com.radwrld.wami.databinding.ItemConversationBinding getBinding() {
            return null;
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull
        com.radwrld.wami.model.Message message) {
        }
    }
}