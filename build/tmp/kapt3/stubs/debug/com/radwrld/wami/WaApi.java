package com.radwrld.wami;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0011\u0010\u0002\u001a\u00020\u0003H\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0004J\u0011\u0010\u0005\u001a\u00020\u0006H\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0004J\u001b\u0010\u0007\u001a\u00020\b2\b\b\u0001\u0010\t\u001a\u00020\nH\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u000b\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\f"}, d2 = {"Lcom/radwrld/wami/WaApi;", "", "getQr", "Lcom/radwrld/wami/QrResponse;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getStatus", "Lcom/radwrld/wami/StatusResponse;", "sendMessage", "", "req", "Lcom/radwrld/wami/SendRequest;", "(Lcom/radwrld/wami/SendRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface WaApi {
    
    @org.jetbrains.annotations.Nullable
    @retrofit2.http.GET(value = "status")
    public abstract java.lang.Object getStatus(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.radwrld.wami.StatusResponse> continuation);
    
    @org.jetbrains.annotations.Nullable
    @retrofit2.http.GET(value = "qrcode")
    public abstract java.lang.Object getQr(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.radwrld.wami.QrResponse> continuation);
    
    @org.jetbrains.annotations.Nullable
    @retrofit2.http.POST(value = "send")
    @retrofit2.http.Headers(value = {"Content-Type: application/json"})
    public abstract java.lang.Object sendMessage(@org.jetbrains.annotations.NotNull
    @retrofit2.http.Body
    com.radwrld.wami.SendRequest req, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.lang.String> continuation);
}