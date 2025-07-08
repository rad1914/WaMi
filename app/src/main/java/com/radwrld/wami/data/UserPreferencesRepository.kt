// @path: app/src/main/java/com/radwrld/wami/data/UserPreferencesRepository.kt
package com.radwrld.wami.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val SESSION_ID = stringPreferencesKey("session_id")
    }

    val sessionIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SESSION_ID]
    }

    suspend fun saveSessionId(sessionId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SESSION_ID] = sessionId
        }
    }

    suspend fun clearSessionId() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SESSION_ID)
        }
    }
}
