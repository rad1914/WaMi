package com.radwrld.wami.api;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\'J\u000e\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003H\'J\u0018\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u00032\b\b\u0001\u0010\t\u001a\u00020\nH\'\u00a8\u0006\u000b"}, d2 = {"Lcom/radwrld/wami/api/WaApi;", "", "getQr", "Lretrofit2/Call;", "Lcom/radwrld/wami/api/QrResponse;", "getStatus", "Lcom/radwrld/wami/api/StatusResponse;", "sendMessage", "Ljava/lang/Void;", "req", "Lcom/radwrld/wami/api/SendMessageRequest;", "app_debug"})
public abstract interface WaApi {
    
    @org.jetbrains.annotations.NotNull
    @retrofit2.http.GET(value = "status")
    public abstract retrofit2.Call<com.radwrld.wami.api.StatusResponse> getStatus();
    
    @org.jetbrains.annotations.NotNull
    @retrofit2.http.GET(value = "qrcode")
    public abstract retrofit2.Call<com.radwrld.wami.api.QrResponse> getQr();
    
    @org.jetbrains.annotations.NotNull
    @retrofit2.http.POST(value = "send")
    @retrofit2.http.Headers(value = {"Content-Type: application/json"})
    public abstract retrofit2.Call<java.lang.Void> sendMessage(@org.jetbrains.annotations.NotNull
    @retrofit2.http.Body
    com.radwrld.wami.api.SendMessageRequest req);
}