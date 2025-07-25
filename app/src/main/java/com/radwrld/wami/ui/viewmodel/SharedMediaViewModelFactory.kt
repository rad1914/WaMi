// @path: app/src/main/java/com/radwrld/wami/ui/viewmodel/SharedMediaViewModelFactory.kt
package com.radwrld.wami.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SharedMediaViewModelFactory(private val app: Application, private val jid: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedMediaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedMediaViewModel(app, jid) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
