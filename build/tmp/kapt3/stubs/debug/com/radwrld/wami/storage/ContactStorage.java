package com.radwrld.wami.storage;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0003\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fJ\u000e\u0010\r\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\fJ\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\f0\u0010J\u0014\u0010\u0011\u001a\u00020\n2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\f0\u0010R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/radwrld/wami/storage/ContactStorage;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "gson", "Lcom/google/gson/Gson;", "sharedPreferences", "Landroid/content/SharedPreferences;", "addContact", "", "newContact", "Lcom/radwrld/wami/model/Contact;", "deleteContact", "contactToDelete", "getContacts", "", "saveContacts", "contacts", "app_debug"})
public final class ContactStorage {
    private final android.content.SharedPreferences sharedPreferences = null;
    private final com.google.gson.Gson gson = null;
    
    public ContactStorage(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
        super();
    }
    
    public final void saveContacts(@org.jetbrains.annotations.NotNull
    java.util.List<com.radwrld.wami.model.Contact> contacts) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<com.radwrld.wami.model.Contact> getContacts() {
        return null;
    }
    
    public final void addContact(@org.jetbrains.annotations.NotNull
    com.radwrld.wami.model.Contact newContact) {
    }
    
    public final void deleteContact(@org.jetbrains.annotations.NotNull
    com.radwrld.wami.model.Contact contactToDelete) {
    }
}