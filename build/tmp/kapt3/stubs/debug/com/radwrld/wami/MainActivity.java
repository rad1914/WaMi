package com.radwrld.wami;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0011H\u0014J\b\u0010\u0012\u001a\u00020\u000fH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/radwrld/wami/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/radwrld/wami/databinding/ActivityMainBinding;", "contactStorage", "Lcom/radwrld/wami/storage/ContactStorage;", "conversationAdapter", "Lcom/radwrld/wami/adapter/ConversationAdapter;", "conversations", "", "Lcom/radwrld/wami/model/Message;", "serverConfigStorage", "Lcom/radwrld/wami/storage/ServerConfigStorage;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "showSetServersDialog", "app_debug"})
public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.radwrld.wami.databinding.ActivityMainBinding binding;
    private com.radwrld.wami.adapter.ConversationAdapter conversationAdapter;
    private com.radwrld.wami.storage.ContactStorage contactStorage;
    private com.radwrld.wami.storage.ServerConfigStorage serverConfigStorage;
    private final java.util.List<com.radwrld.wami.model.Message> conversations = null;
    
    public MainActivity() {
        super();
    }
    
    @java.lang.Override
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    private final void showSetServersDialog() {
    }
}