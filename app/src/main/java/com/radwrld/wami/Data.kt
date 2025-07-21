// @path: app/src/main/java/com/radwrld/wami/data/Data.kt
package com.radwrld.wami.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.radwrld.wami.WamiApp
import com.radwrld.wami.data.local.ChatDao
import com.radwrld.wami.data.local.MessageDao
import com.radwrld.wami.data.local.toChat
import com.radwrld.wami.data.local.toEntity
import com.radwrld.wami.data.local.toMessage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
private val JSON = "application/json; charset=utf-8".toMediaType()

private val Context.dataStore by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
}

@Singleton
class ApiService @Inject constructor(
    private val client: OkHttpClient
) {
    private inline fun <T> safeCall(block: () -> T): T? = try { block() } catch (_: Exception) { null }

    suspend fun createSession(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        safeCall {
            val body = json.encodeToString(mapOf("sessionId" to sessionId)).toRequestBody(JSON)
            client.newCall(
                Request.Builder()
                    .url("${WamiApp.Constants.BASE_URL}/sessions")
                    .post(body)
                    .build()
            ).execute().isSuccessful
        } ?: false
    }

    suspend fun getSessions(): List<String> = withContext(Dispatchers.IO) {
        safeCall {
            client.newCall(
                Request.Builder()
                    .url("${WamiApp.Constants.BASE_URL}/sessions")
                    .get()
                    .build()
            ).execute().use {
                if (it.isSuccessful) json.decodeFromString(it.body?.string() ?: "[]")
                else emptyList()
            }
        } ?: emptyList()
    }

    suspend fun fetchChats(sessionId: String): List<Chat> = withContext(Dispatchers.IO) {
        safeCall {
            client.newCall(
                Request.Builder()
                    .url("${WamiApp.Constants.BASE_URL}/chats/$sessionId")
                    .build()
            ).execute().use {
                if (it.isSuccessful) json.decodeFromString(it.body?.string() ?: "[]")
                else emptyList()
            }
        } ?: emptyList()
    }

    suspend fun sendMessage(sessionId: String, jid: String, text: String): Boolean = withContext(Dispatchers.IO) {
        safeCall {
            val body = json.encodeToString(SendMessageRequest(jid, text)).toRequestBody(JSON)
            client.newCall(
                Request.Builder()
                    .url("${WamiApp.Constants.BASE_URL}/messages/$sessionId/send")
                    .post(body)
                    .build()
            ).execute().isSuccessful
        } ?: false
    }
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val SESSION_KEY = stringPreferencesKey("session_id")

    val sessionIdFlow: Flow<String?> = context.dataStore.data.map { it[SESSION_KEY] }

    suspend fun getSessionId(): String? = sessionIdFlow.firstOrNull()

    suspend fun saveSessionId(id: String) {
        context.dataStore.edit { it[SESSION_KEY] = id }
    }

    suspend fun clearSessionId() {
        context.dataStore.edit { it.remove(SESSION_KEY) }
    }
}

@Singleton
class ChatRepository @Inject constructor(
    private val dao: ChatDao,
    private val api: ApiService,
    private val prefs: UserPreferencesRepository
) {
    fun observeChats(): Flow<List<Chat>> = dao.getAll().map { list -> list.map { it.toChat() } }

    suspend fun refreshChats(): List<Chat> {
        val session = prefs.getSessionId() ?: return emptyList()
        val remote = api.fetchChats(session)
        dao.upsertAll(remote.map { it.toEntity() })
        return remote
    }

    suspend fun clearLocal() = dao.clear()
}

@Singleton
class MessageRepository @Inject constructor(
    private val dao: MessageDao,
    private val api: ApiService,
    private val prefs: UserPreferencesRepository
) {
    fun observeMessages(jid: String): Flow<List<Message>> = dao.getForJid(jid).map { list -> list.map { it.toMessage() } }
    
    // History fetching is no longer supported by the backend
    suspend fun refreshMessages(jid: String): List<Message> {
        return emptyList()
    }

    suspend fun sendText(sessionId: String, jid: String, text: String): Boolean =
        api.sendMessage(sessionId, jid, text)

    suspend fun clearForJid(jid: String) = dao.clearForJid(jid)
}

// Renamed 'id' to 'jid' to match Baileys' terminology used in backend
@Serializable data class Chat(val id: String, val name: String?) {
    val jid: String
        get() = id
}
// The 'to' field is for the backend, 'jid' is for the frontend data models.
@Serializable data class SendMessageRequest(val to: String, val text: String)

@Serializable data class Message(
    val id: String,
    val isOutgoing: Boolean,
    val text: String?,
    val timestamp: Long,
    val jid: String,
    val reactions: Map<String, Int> = emptyMap(),
    var status: MessageStatus? = null
)

enum class MessageStatus { SENDING, FAILED, SENT }