package com.radwrld.wami;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\r2\u0006\u0010\u001b\u001a\u00020\rH\u0002J\u0010\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u000e\u001a\u00020\rH\u0002J\b\u0010\u001e\u001a\u00020\u0019H\u0002J\u0012\u0010\u001f\u001a\u00020\u00192\b\u0010 \u001a\u0004\u0018\u00010!H\u0014J\b\u0010\"\u001a\u00020\u0019H\u0014J\u0018\u0010#\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\r2\u0006\u0010\u001b\u001a\u00020\rH\u0002J\b\u0010$\u001a\u00020\u0019H\u0002J\u0010\u0010%\u001a\u00020\u00192\u0006\u0010&\u001a\u00020\rH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\u000bR\u000e\u0010\f\u001a\u00020\rX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\rX\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00110\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\rX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0017X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\'"}, d2 = {"Lcom/radwrld/wami/ChatActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "_binding", "Lcom/radwrld/wami/databinding/ActivityChatBinding;", "adapter", "Lcom/radwrld/wami/adapter/ChatAdapter;", "api", "Lcom/radwrld/wami/network/WhatsAppApi;", "binding", "getBinding", "()Lcom/radwrld/wami/databinding/ActivityChatBinding;", "contactName", "", "jid", "messages", "", "Lcom/radwrld/wami/model/Message;", "onMessageStatusUpdate", "Lio/socket/emitter/Emitter$Listener;", "onNewMessage", "serverUrl", "socket", "Lio/socket/client/Socket;", "addMessageToUI", "", "text", "tempId", "isValidJid", "", "loadChatHistory", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "sendMessageToServer", "setupSocket", "showToast", "msg", "app_debug"})
public final class ChatActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.radwrld.wami.databinding.ActivityChatBinding _binding;
    private com.radwrld.wami.adapter.ChatAdapter adapter;
    private final java.util.List<com.radwrld.wami.model.Message> messages = null;
    private com.radwrld.wami.network.WhatsAppApi api;
    private io.socket.client.Socket socket;
    private java.lang.String jid;
    private java.lang.String contactName;
    private final java.lang.String serverUrl = "http://22.ip.gl.ply.gg:18880/";
    private final io.socket.emitter.Emitter.Listener onNewMessage = null;
    private final io.socket.emitter.Emitter.Listener onMessageStatusUpdate = null;
    
    public ChatActivity() {
        super();
    }
    
    private final com.radwrld.wami.databinding.ActivityChatBinding getBinding() {
        return null;
    }
    
    @java.lang.Override
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    private final void addMessageToUI(java.lang.String text, java.lang.String tempId) {
    }
    
    private final void sendMessageToServer(java.lang.String text, java.lang.String tempId) {
    }
    
    private final void loadChatHistory() {
    }
    
    private final void setupSocket() {
    }
    
    private final boolean isValidJid(java.lang.String jid) {
        return false;
    }
    
    private final void showToast(java.lang.String msg) {
    }
    
    @java.lang.Override
    protected void onDestroy() {
    }
}