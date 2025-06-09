package com.radwrld.wami;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u000f\u001a\u00020\u0010H\u0002J\u0010\u0010\u0011\u001a\u00020\u00102\u0006\u0010\u0012\u001a\u00020\u0013H\u0002J\b\u0010\u0014\u001a\u00020\u0010H\u0002J\u0012\u0010\u0015\u001a\u00020\u00102\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u0014J\b\u0010\u0018\u001a\u00020\u0010H\u0014J\b\u0010\u0019\u001a\u00020\u0010H\u0002J\u0010\u0010\u001a\u001a\u00020\u00102\u0006\u0010\u001b\u001a\u00020\u0013H\u0002J\b\u0010\u001c\u001a\u00020\u0010H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001d"}, d2 = {"Lcom/radwrld/wami/LoginActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "SWITCH_SERVER_DELAY_MS", "", "config", "Lcom/radwrld/wami/storage/ServerConfigStorage;", "handler", "Landroid/os/Handler;", "qrImage", "Landroid/widget/ImageView;", "statusText", "Landroid/widget/TextView;", "waApi", "Lcom/radwrld/wami/api/WaApi;", "checkStatus", "", "initApi", "serverIp", "", "launchMain", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "pollForQr", "renderQr", "text", "retryOrPollQr", "app_debug"})
public final class LoginActivity extends androidx.appcompat.app.AppCompatActivity {
    private android.widget.ImageView qrImage;
    private android.widget.TextView statusText;
    private com.radwrld.wami.api.WaApi waApi;
    private com.radwrld.wami.storage.ServerConfigStorage config;
    private final android.os.Handler handler = null;
    private final long SWITCH_SERVER_DELAY_MS = 12000L;
    
    public LoginActivity() {
        super();
    }
    
    @java.lang.Override
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    private final void initApi(java.lang.String serverIp) {
    }
    
    private final void checkStatus() {
    }
    
    private final void retryOrPollQr() {
    }
    
    private final void pollForQr() {
    }
    
    private final void renderQr(java.lang.String text) {
    }
    
    private final void launchMain() {
    }
    
    @java.lang.Override
    protected void onDestroy() {
    }
}