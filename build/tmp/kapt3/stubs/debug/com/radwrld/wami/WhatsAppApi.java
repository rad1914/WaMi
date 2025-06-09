package com.radwrld.wami;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J+\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0003\u0010\u0007\u001a\u00020\bH\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\tJ\u001b\u0010\n\u001a\u00020\u000b2\b\b\u0001\u0010\f\u001a\u00020\rH\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u000e\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u000f"}, d2 = {"Lcom/radwrld/wami/WhatsAppApi;", "", "getHistory", "", "Lcom/radwrld/wami/ChatMessageDto;", "jid", "", "limit", "", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendMessage", "Lcom/radwrld/wami/SendResponse;", "req", "Lcom/radwrld/wami/SendRequest;", "(Lcom/radwrld/wami/SendRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface WhatsAppApi {
    
    @org.jetbrains.annotations.Nullable
    @retrofit2.http.GET(value = "history/{jid}")
    public abstract java.lang.Object getHistory(@org.jetbrains.annotations.NotNull
    @retrofit2.http.Path(value = "jid")
    java.lang.String jid, @retrofit2.http.Query(value = "limit")
    int limit, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.util.List<com.radwrld.wami.ChatMessageDto>> continuation);
    
    @org.jetbrains.annotations.Nullable
    @retrofit2.http.POST(value = "send")
    public abstract java.lang.Object sendMessage(@org.jetbrains.annotations.NotNull
    @retrofit2.http.Body
    com.radwrld.wami.SendRequest req, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.radwrld.wami.SendResponse> continuation);
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 3)
    public final class DefaultImpls {
    }
}