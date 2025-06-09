package com.radwrld.wami;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\r\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u001b\u0012\u0014\u0010\u0002\u001a\u0010\u0012\u0006\u0012\u0004\u0018\u00010\u0004\u0012\u0004\u0012\u00020\u00050\u0003\u00a2\u0006\u0002\u0010\u0006J\u0012\u0010\u0007\u001a\u00020\u00052\b\u0010\b\u001a\u0004\u0018\u00010\tH\u0016J*\u0010\n\u001a\u00020\u00052\b\u0010\b\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\f2\u0006\u0010\u000e\u001a\u00020\fH\u0016J*\u0010\u0002\u001a\u00020\u00052\b\u0010\b\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\u000f\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\fH\u0016R\u001c\u0010\u0002\u001a\u0010\u0012\u0006\u0012\u0004\u0018\u00010\u0004\u0012\u0004\u0012\u00020\u00050\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/radwrld/wami/SimpleTextWatcher;", "Landroid/text/TextWatcher;", "onTextChanged", "Lkotlin/Function1;", "", "", "(Lkotlin/jvm/functions/Function1;)V", "afterTextChanged", "s", "Landroid/text/Editable;", "beforeTextChanged", "start", "", "count", "after", "before", "app_debug"})
public final class SimpleTextWatcher implements android.text.TextWatcher {
    private final kotlin.jvm.functions.Function1<java.lang.CharSequence, kotlin.Unit> onTextChanged = null;
    
    public SimpleTextWatcher(@org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function1<? super java.lang.CharSequence, kotlin.Unit> onTextChanged) {
        super();
    }
    
    @java.lang.Override
    public void beforeTextChanged(@org.jetbrains.annotations.Nullable
    java.lang.CharSequence s, int start, int count, int after) {
    }
    
    @java.lang.Override
    public void onTextChanged(@org.jetbrains.annotations.Nullable
    java.lang.CharSequence s, int start, int before, int count) {
    }
    
    @java.lang.Override
    public void afterTextChanged(@org.jetbrains.annotations.Nullable
    android.text.Editable s) {
    }
}