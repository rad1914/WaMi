package com.radwrld.wami.network;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0005J!\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00070\u00032\b\b\u0001\u0010\b\u001a\u00020\tH\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\nJ\u001b\u0010\u000b\u001a\u00020\f2\b\b\u0001\u0010\r\u001a\u00020\u000eH\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u000f\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u0010"}, d2 = {"Lcom/radwrld/wami/network/WhatsAppApi;", "", "getChats", "", "Lcom/radwrld/wami/network/Conversation;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getHistory", "Lcom/radwrld/wami/network/MessageHistoryItem;", "jid", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendMessage", "Lcom/radwrld/wami/network/SendResponse;", "request", "Lcom/radwrld/wami/network/SendRequest;", "(Lcom/radwrld/wami/network/SendRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface WhatsAppApi {
    
    @org.jetbrains.annotations.Nullable
    @retrofit2.http.GET(value = "history/{jid}")
    public abstract java.lang.Object getHistory(@org.jetbrains.annotations.NotNull
    @retrofit2.http.Path(value = "jid")
    java.lang.String jid, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.util.List<com.radwrld.wami.network.MessageHistoryItem>> continuation);
    
    @org.jetbrains.annotations.Nullable
    @retrofit2.http.POST(value = "send")
    public abstract java.lang.Object sendMessage(@org.jetbrains.annotations.NotNull
    @retrofit2.http.Body
    com.radwrld.wami.network.SendRequest request, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.radwrld.wami.network.SendResponse> continuation);
    
    @org.jetbrains.annotations.Nullable
    @retrofit2.http.GET(value = "chats")
    public abstract java.lang.Object getChats(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.util.List<com.radwrld.wami.network.Conversation>> continuation);
}