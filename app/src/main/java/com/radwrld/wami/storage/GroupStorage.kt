// @path: app/src/main/java/com/radwrld/wami/storage/GroupStorage.kt
package com.radwrld.wami.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radwrld.wami.network.GroupInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GroupStorage(context: Context) {
    private val prefs = context.getSharedPreferences("wami_groups_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _groupInfoFlow = MutableStateFlow<Map<String, GroupInfo>>(emptyMap())
    val groupInfoFlow: StateFlow<Map<String, GroupInfo>> = _groupInfoFlow.asStateFlow()

    init {
        _groupInfoFlow.value = loadAllGroupsFromPrefs()
    }

    
    fun saveGroupInfo(groupInfo: GroupInfo) {
        val currentGroups = _groupInfoFlow.value.toMutableMap()
        currentGroups[groupInfo.id] = groupInfo
        saveGroupsToPrefs(currentGroups)
    }

    private fun saveGroupsToPrefs(groups: Map<String, GroupInfo>) {
        val json = gson.toJson(groups)
        prefs.edit().putString("group_info_map", json).apply()
        _groupInfoFlow.value = groups
    }

    private fun loadAllGroupsFromPrefs(): Map<String, GroupInfo> {
        return try {
            val json = prefs.getString("group_info_map", null)
            if (json != null) {
                val type = object : TypeToken<Map<String, GroupInfo>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
