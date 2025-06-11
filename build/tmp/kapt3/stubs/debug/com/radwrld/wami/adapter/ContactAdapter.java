package com.radwrld.wami.adapter;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\f\u0012\b\u0012\u00060\u0002R\u00020\u00000\u0001:\u0001\u0013B\'\u0012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u0012\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\u0002\u0010\tJ\b\u0010\n\u001a\u00020\u000bH\u0016J\u001c\u0010\f\u001a\u00020\b2\n\u0010\r\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u000e\u001a\u00020\u000bH\u0016J\u001c\u0010\u000f\u001a\u00060\u0002R\u00020\u00002\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u000bH\u0016R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/radwrld/wami/adapter/ContactAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/radwrld/wami/adapter/ContactAdapter$ContactViewHolder;", "contacts", "", "Lcom/radwrld/wami/model/Contact;", "onContactClick", "Lkotlin/Function1;", "", "(Ljava/util/List;Lkotlin/jvm/functions/Function1;)V", "getItemCount", "", "onBindViewHolder", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "ContactViewHolder", "app_debug"})
public final class ContactAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.radwrld.wami.adapter.ContactAdapter.ContactViewHolder> {
    private final java.util.List<com.radwrld.wami.model.Contact> contacts = null;
    private final kotlin.jvm.functions.Function1<com.radwrld.wami.model.Contact, kotlin.Unit> onContactClick = null;
    
    public ContactAdapter(@org.jetbrains.annotations.NotNull
    java.util.List<com.radwrld.wami.model.Contact> contacts, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function1<? super com.radwrld.wami.model.Contact, kotlin.Unit> onContactClick) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    @java.lang.Override
    public com.radwrld.wami.adapter.ContactAdapter.ContactViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull
    com.radwrld.wami.adapter.ContactAdapter.ContactViewHolder holder, int position) {
    }
    
    @java.lang.Override
    public int getItemCount() {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u000b"}, d2 = {"Lcom/radwrld/wami/adapter/ContactAdapter$ContactViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "binding", "Lcom/radwrld/wami/databinding/ItemContactBinding;", "(Lcom/radwrld/wami/adapter/ContactAdapter;Lcom/radwrld/wami/databinding/ItemContactBinding;)V", "getBinding", "()Lcom/radwrld/wami/databinding/ItemContactBinding;", "bind", "", "contact", "Lcom/radwrld/wami/model/Contact;", "app_debug"})
    public final class ContactViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull
        private final com.radwrld.wami.databinding.ItemContactBinding binding = null;
        
        public ContactViewHolder(@org.jetbrains.annotations.NotNull
        com.radwrld.wami.databinding.ItemContactBinding binding) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull
        public final com.radwrld.wami.databinding.ItemContactBinding getBinding() {
            return null;
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull
        com.radwrld.wami.model.Contact contact) {
        }
    }
}