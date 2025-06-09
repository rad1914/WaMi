package com.radwrld.wami.storage;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u0010\u001a\u00020\u0006J\u0006\u0010\u0011\u001a\u00020\u0006J\u0006\u0010\u0012\u001a\u00020\u0013J\u0016\u0010\u0014\u001a\u00020\u00132\u0006\u0010\u0015\u001a\u00020\u00062\u0006\u0010\u0016\u001a\u00020\u0006R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u0011\u0010\t\u001a\u00020\u00068F\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\f\u001a\u00020\u00068F\u00a2\u0006\u0006\u001a\u0004\b\r\u0010\u000bR\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/radwrld/wami/storage/ServerConfigStorage;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "FALLBACK_KEY", "", "INDEX_KEY", "PRIMARY_KEY", "fallbackServer", "getFallbackServer", "()Ljava/lang/String;", "primaryServer", "getPrimaryServer", "sharedPreferences", "Landroid/content/SharedPreferences;", "getCurrentServer", "moveToNextServer", "resetToPrimary", "", "saveServers", "primary", "fallback", "app_debug"})
public final class ServerConfigStorage {
    private final android.content.SharedPreferences sharedPreferences = null;
    private final java.lang.String PRIMARY_KEY = "server_primary";
    private final java.lang.String FALLBACK_KEY = "server_fallback";
    private final java.lang.String INDEX_KEY = "server_index";
    
    public ServerConfigStorage(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getPrimaryServer() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getFallbackServer() {
        return null;
    }
    
    public final void saveServers(@org.jetbrains.annotations.NotNull
    java.lang.String primary, @org.jetbrains.annotations.NotNull
    java.lang.String fallback) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getCurrentServer() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String moveToNextServer() {
        return null;
    }
    
    public final void resetToPrimary() {
    }
}