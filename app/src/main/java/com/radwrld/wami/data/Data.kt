package com.radwrld.wami.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.radwrld.wami.data.local.ChatDao // Added
import com.radwrld.wami.data.local.MessageDao // Added
import com.radwrld.wami.data.local.toChat // Added
import com.radwrld.wami.data.local.toEntity // Added
import com.radwrld.wami.data.local.toMessage // Added
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
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_URL = "http://22.ip.gl.ply.gg:18880"
private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

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

    suspend fun createSession(): String? = withContext(Dispatchers.IO) {
        safeCall {
            client.newCall(Request.Builder()
                .url("$BASE_URL/session/create")
                .post("".toRequestBody())
                .build()).execute().use {
                    if (it.isSuccessful) json.decodeFromString<CreateSessionResponse>(it.body!!.string()).sessionId
                    else null
                }
        }
    }

    suspend fun getStatus(sessionId: String): StatusResponse? = withContext(Dispatchers.IO) {
        safeCall {
            client.newCall(Request.Builder()
                .url("$BASE_URL/session/status")
                .header("Authorization", "Bearer $sessionId")
                .build()).execute().use {
                    if (it.isSuccessful) json.decodeFromString(it.body!!.string())
                    else null
                }
        }
    }

    suspend fun fetchChats(sessionId: String): List<Chat> = withContext(Dispatchers.IO) {
        safeCall {
            client.newCall(Request.Builder()
                .url("$BASE_URL/chats")
                .header("Authorization", "Bearer $sessionId")
                .build()).execute().use {
                    if (it.isSuccessful) json.decodeFromString(it.body?.string() ?: "")
                    else emptyList()
                }
        } ?: emptyList()
    }

    suspend fun fetchMessages(sessionId: String, jid: String): List<Message> = withContext(Dispatchers.IO) {
        safeCall {
            client.newCall(Request.Builder()
                .url("$BASE_URL/history/$jid")
                .header("Authorization", "Bearer $sessionId")
                .build()).execute().use {
                    if (it.isSuccessful) json.decodeFromString(it.body?.string() ?: "")
                    else emptyList()
                }
        } ?: emptyList()
    }

    suspend fun sendMessage(sessionId: String, jid: String, text: String, tempId: String): SendMessageResponse? = withContext(Dispatchers.IO) {
        safeCall {
            val body = json.encodeToString(SendMessageRequest(jid, text, tempId)).toRequestBody()
            client.newCall(Request.Builder()
                .url("$BASE_URL/message/send")
                .header("Authorization", "Bearer $sessionId")
                .post(body)
                .build()).execute().use {
                    if (it.isSuccessful) json.decodeFromString<SendMessageResponse>(it.body!!.string())
                    else null
                }
        }
    }

    suspend fun sendReaction(sessionId: String, jid: String, messageId: String, emoji: String): Boolean = withContext(Dispatchers.IO) {
        safeCall {
            val body = json.encodeToString(SendReactionRequest(jid, messageId, emoji)).toRequestBody()
            client.newCall(Request.Builder()
                .url("$BASE_URL/message/send/reaction")
                .header("Authorization", "Bearer $sessionId")
                .post(body)
                .build()).execute().isSuccessful
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

    suspend fun refreshMessages(jid: String): List<Message> {
        val session = prefs.getSessionId() ?: return emptyList()
        val remote = api.fetchMessages(session, jid)
        dao.upsertAll(remote.map { it.toEntity() })
        return remote
    }

    suspend fun sendText(sessionId: String, jid: String, text: String, tempId: String): SendMessageResponse? =
        api.sendMessage(sessionId, jid, text, tempId)

    suspend fun sendReaction(sessionId: String, jid: String, messageId: String, emoji: String): Boolean =
        api.sendReaction(sessionId, jid, messageId, emoji)

    suspend fun clearForJid(jid: String) = dao.clearForJid(jid)
}

@Serializable data class CreateSessionResponse(val sessionId: String)
@Serializable data class StatusResponse(val connected: Boolean, val qr: String? = null)
@Serializable data class Chat(val jid: String, val name: String?)
@Serializable data class SendMessageRequest(val jid: String, val text: String, val tempId: String)
@Serializable data class SendReactionRequest(val jid: String, val messageId: String, val emoji: String)
@Serializable data class SendMessageResponse(val messageId: String, val tempId: String, val timestamp: Long)
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