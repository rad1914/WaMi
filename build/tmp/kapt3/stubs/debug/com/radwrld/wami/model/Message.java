package com.radwrld.wami.model;

import java.lang.System;

/**
 * Represents a single chat message, either for the contact list (MainActivity)
 * or for the chat screen (ChatActivity).
 *
 * @param name       Display name of the sender.
 * @param text       Message body.
 * @param status     Delivery/read status (e.g. "sent", "delivered", "read").
 * @param id         Unique message identifier.
 * @param jid        Jabber ID (or other internal ID) of the sender.
 * @param isOutgoing True if this message was sent by the local user.
 * @param timestamp  Unix‐epoch milliseconds when this message was created.
 * @param lastMessage The last message for the contact list (MainActivity only).
 * @param avatarUrl  Avatar URL for the contact (MainActivity only).
 * @param phoneNumber The phone number of the sender (MainActivity only).
 * @param isOnline   Indicates if the contact is online (MainActivity only).
 */
@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\t\n\u0002\b\"\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001Bq\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0007\u001a\u00020\u0003\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u0012\b\b\u0002\u0010\n\u001a\u00020\u000b\u0012\b\b\u0002\u0010\f\u001a\u00020\u0003\u0012\b\b\u0002\u0010\r\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u000f\u001a\u00020\t\u00a2\u0006\u0002\u0010\u0010J\t\u0010\u001f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010 \u001a\u00020\u0003H\u00c6\u0003J\t\u0010!\u001a\u00020\tH\u00c6\u0003J\t\u0010\"\u001a\u00020\u0003H\u00c6\u0003J\t\u0010#\u001a\u00020\u0003H\u00c6\u0003J\t\u0010$\u001a\u00020\u0003H\u00c6\u0003J\t\u0010%\u001a\u00020\u0003H\u00c6\u0003J\t\u0010&\u001a\u00020\tH\u00c6\u0003J\t\u0010\'\u001a\u00020\u000bH\u00c6\u0003J\t\u0010(\u001a\u00020\u0003H\u00c6\u0003J\t\u0010)\u001a\u00020\u0003H\u00c6\u0003Jw\u0010*\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00032\b\b\u0002\u0010\u0007\u001a\u00020\u00032\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u000b2\b\b\u0002\u0010\f\u001a\u00020\u00032\b\b\u0002\u0010\r\u001a\u00020\u00032\b\b\u0002\u0010\u000e\u001a\u00020\u00032\b\b\u0002\u0010\u000f\u001a\u00020\tH\u00c6\u0001J\u0013\u0010+\u001a\u00020\t2\b\u0010,\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010-\u001a\u00020.H\u00d6\u0001J\t\u0010/\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\r\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0006\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0012R\u0011\u0010\u000f\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0014R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0014R\u0011\u0010\u0007\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0012R\u0011\u0010\f\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0012R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0012R\u0011\u0010\u000e\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0012R\u001a\u0010\u0005\u001a\u00020\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0019\u0010\u0012\"\u0004\b\u001a\u0010\u001bR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0012R\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001e\u00a8\u00060"}, d2 = {"Lcom/radwrld/wami/model/Message;", "", "name", "", "text", "status", "id", "jid", "isOutgoing", "", "timestamp", "", "lastMessage", "avatarUrl", "phoneNumber", "isOnline", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V", "getAvatarUrl", "()Ljava/lang/String;", "getId", "()Z", "getJid", "getLastMessage", "getName", "getPhoneNumber", "getStatus", "setStatus", "(Ljava/lang/String;)V", "getText", "getTimestamp", "()J", "component1", "component10", "component11", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "", "toString", "app_debug"})
public final class Message {
    @org.jetbrains.annotations.NotNull
    private final java.lang.String name = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String text = null;
    @org.jetbrains.annotations.NotNull
    private java.lang.String status;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String id = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String jid = null;
    private final boolean isOutgoing = false;
    private final long timestamp = 0L;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String lastMessage = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String avatarUrl = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String phoneNumber = null;
    private final boolean isOnline = false;
    
    /**
     * Represents a single chat message, either for the contact list (MainActivity)
     * or for the chat screen (ChatActivity).
     *
     * @param name       Display name of the sender.
     * @param text       Message body.
     * @param status     Delivery/read status (e.g. "sent", "delivered", "read").
     * @param id         Unique message identifier.
     * @param jid        Jabber ID (or other internal ID) of the sender.
     * @param isOutgoing True if this message was sent by the local user.
     * @param timestamp  Unix‐epoch milliseconds when this message was created.
     * @param lastMessage The last message for the contact list (MainActivity only).
     * @param avatarUrl  Avatar URL for the contact (MainActivity only).
     * @param phoneNumber The phone number of the sender (MainActivity only).
     * @param isOnline   Indicates if the contact is online (MainActivity only).
     */
    @org.jetbrains.annotations.NotNull
    public final com.radwrld.wami.model.Message copy(@org.jetbrains.annotations.NotNull
    java.lang.String name, @org.jetbrains.annotations.NotNull
    java.lang.String text, @org.jetbrains.annotations.NotNull
    java.lang.String status, @org.jetbrains.annotations.NotNull
    java.lang.String id, @org.jetbrains.annotations.NotNull
    java.lang.String jid, boolean isOutgoing, long timestamp, @org.jetbrains.annotations.NotNull
    java.lang.String lastMessage, @org.jetbrains.annotations.NotNull
    java.lang.String avatarUrl, @org.jetbrains.annotations.NotNull
    java.lang.String phoneNumber, boolean isOnline) {
        return null;
    }
    
    /**
     * Represents a single chat message, either for the contact list (MainActivity)
     * or for the chat screen (ChatActivity).
     *
     * @param name       Display name of the sender.
     * @param text       Message body.
     * @param status     Delivery/read status (e.g. "sent", "delivered", "read").
     * @param id         Unique message identifier.
     * @param jid        Jabber ID (or other internal ID) of the sender.
     * @param isOutgoing True if this message was sent by the local user.
     * @param timestamp  Unix‐epoch milliseconds when this message was created.
     * @param lastMessage The last message for the contact list (MainActivity only).
     * @param avatarUrl  Avatar URL for the contact (MainActivity only).
     * @param phoneNumber The phone number of the sender (MainActivity only).
     * @param isOnline   Indicates if the contact is online (MainActivity only).
     */
    @java.lang.Override
    public boolean equals(@org.jetbrains.annotations.Nullable
    java.lang.Object other) {
        return false;
    }
    
    /**
     * Represents a single chat message, either for the contact list (MainActivity)
     * or for the chat screen (ChatActivity).
     *
     * @param name       Display name of the sender.
     * @param text       Message body.
     * @param status     Delivery/read status (e.g. "sent", "delivered", "read").
     * @param id         Unique message identifier.
     * @param jid        Jabber ID (or other internal ID) of the sender.
     * @param isOutgoing True if this message was sent by the local user.
     * @param timestamp  Unix‐epoch milliseconds when this message was created.
     * @param lastMessage The last message for the contact list (MainActivity only).
     * @param avatarUrl  Avatar URL for the contact (MainActivity only).
     * @param phoneNumber The phone number of the sender (MainActivity only).
     * @param isOnline   Indicates if the contact is online (MainActivity only).
     */
    @java.lang.Override
    public int hashCode() {
        return 0;
    }
    
    /**
     * Represents a single chat message, either for the contact list (MainActivity)
     * or for the chat screen (ChatActivity).
     *
     * @param name       Display name of the sender.
     * @param text       Message body.
     * @param status     Delivery/read status (e.g. "sent", "delivered", "read").
     * @param id         Unique message identifier.
     * @param jid        Jabber ID (or other internal ID) of the sender.
     * @param isOutgoing True if this message was sent by the local user.
     * @param timestamp  Unix‐epoch milliseconds when this message was created.
     * @param lastMessage The last message for the contact list (MainActivity only).
     * @param avatarUrl  Avatar URL for the contact (MainActivity only).
     * @param phoneNumber The phone number of the sender (MainActivity only).
     * @param isOnline   Indicates if the contact is online (MainActivity only).
     */
    @org.jetbrains.annotations.NotNull
    @java.lang.Override
    public java.lang.String toString() {
        return null;
    }
    
    public Message(@org.jetbrains.annotations.NotNull
    java.lang.String name, @org.jetbrains.annotations.NotNull
    java.lang.String text, @org.jetbrains.annotations.NotNull
    java.lang.String status, @org.jetbrains.annotations.NotNull
    java.lang.String id, @org.jetbrains.annotations.NotNull
    java.lang.String jid, boolean isOutgoing, long timestamp, @org.jetbrains.annotations.NotNull
    java.lang.String lastMessage, @org.jetbrains.annotations.NotNull
    java.lang.String avatarUrl, @org.jetbrains.annotations.NotNull
    java.lang.String phoneNumber, boolean isOnline) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getText() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getStatus() {
        return null;
    }
    
    public final void setStatus(@org.jetbrains.annotations.NotNull
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getJid() {
        return null;
    }
    
    public final boolean component6() {
        return false;
    }
    
    public final boolean isOutgoing() {
        return false;
    }
    
    public final long component7() {
        return 0L;
    }
    
    public final long getTimestamp() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component8() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getLastMessage() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getAvatarUrl() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component10() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getPhoneNumber() {
        return null;
    }
    
    public final boolean component11() {
        return false;
    }
    
    public final boolean isOnline() {
        return false;
    }
}