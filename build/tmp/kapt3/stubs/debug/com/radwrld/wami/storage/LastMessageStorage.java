package com.radwrld.wami.storage;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\t\u001a\u0004\u0018\u00010\n2\u0006\u0010\u000b\u001a\u00020\fJ\u0016\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\u000f\u001a\u00020\nR\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/radwrld/wami/storage/LastMessageStorage;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "gson", "Lcom/google/gson/Gson;", "prefs", "Landroid/content/SharedPreferences;", "getLastMessage", "Lcom/radwrld/wami/model/Message;", "jid", "", "saveLastMessage", "", "message", "app_debug"})
public final class LastMessageStorage {
    private final android.content.SharedPreferences prefs = null;
    private final com.google.gson.Gson gson = null;
    
    public LastMessageStorage(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
        super();
    }
    
    /**
     * Saves the last message for a given JID.
     * @param jid The Jabber ID of the chat.
     * @param message The message object to save.
     */
    public final void saveLastMessage(@org.jetbrains.annotations.NotNull
    java.lang.String jid, @org.jetbrains.annotations.NotNull
    com.radwrld.wami.model.Message message) {
    }
    
    /**
     * Retrieves the last saved message for a given JID.
     * @param jid The Jabber ID of the chat.
     * @return The last saved Message object, or null if none exists.
     */
    @org.jetbrains.annotations.Nullable
    public final com.radwrld.wami.model.Message getLastMessage(@org.jetbrains.annotations.NotNull
    java.lang.String jid) {
        return null;
    }
}