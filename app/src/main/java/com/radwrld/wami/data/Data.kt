package com.radwrld.wami.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.radwrld.wami.data.local.*
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
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

object Constants {
    const val BASE_URL = "http://22.ip.gl.ply.gg:18880"
}

object Http {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = Http.client
}

class ApiService @Inject constructor(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    private inline fun <T> safeCall(block: () -> T): T? =
        try { block() } catch (_: Exception) { null }

    suspend fun createSession(): String? = withContext(Dispatchers.IO) {
        safeCall {
            val req = Request.Builder()
                .url("${Constants.BASE_URL}/session/create")
                .post("".toRequestBody())
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null
                else json.decodeFromString<CreateSessionResponse>(resp.body!!.string()).sessionId
            }
        }
    }

    suspend fun getStatus(sessionId: String): StatusResponse? = withContext(Dispatchers.IO) {
        safeCall {
            val req = Request.Builder()
                .url("${Constants.BASE_URL}/session/status")
                .header("Authorization", "Bearer $sessionId")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null
                else json.decodeFromString(resp.body!!.string())
            }
        }
    }

    suspend fun fetchChats(sessionId: String): List<Chat> = safeCall {
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/chats")
            .header("Authorization", "Bearer $sessionId")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) emptyList()
            else json.decodeFromString(resp.body!!.string())
        }
    } ?: emptyList()

    suspend fun fetchMessages(sessionId: String, jid: String): List<Message> = safeCall {
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/messages?jid=$jid")
            .header("Authorization", "Bearer $sessionId")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) emptyList()
            else json.decodeFromString(resp.body!!.string())
        }
    } ?: emptyList()

    suspend fun sendMessage(sessionId: String, jid: String, text: String): Boolean = safeCall {
        val body = json.encodeToString(SendMessageRequest(jid, text)).toRequestBody()
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/message/send")
            .header("Authorization", "Bearer $sessionId")
            .post(body)
            .build()
        client.newCall(req).execute().use { it.isSuccessful }
    } ?: false
}

@Serializable data class CreateSessionResponse(val sessionId: String)
@Serializable data class StatusResponse(val connected: Boolean, val qr: String? = null)
@Serializable data class Chat(val jid: String, val name: String)
@Serializable data class SendMessageRequest(val jid: String, val text: String)
@Serializable data class Message(
    val id: String,
    val fromMe: Boolean,
    val text: String?,
    val timestamp: Long,
    val jid: String,
    val reactions: String = "",
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageStatus { SENDING, FAILED, SENT }

private val Context.dataStore by preferencesDataStore(name = "settings")

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
    fun observeChats(): Flow<List<Chat>> =
        dao.getAll().map { it.map { it.toChat() } }

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
    fun observeMessages(jid: String): Flow<List<Message>> =
        dao.getForJid(jid).map { it.map { it.toMessage() } }

    suspend fun refreshMessages(jid: String): List<Message> {
        val session = prefs.getSessionId() ?: return emptyList()
        val remote = api.fetchMessages(session, jid)
        dao.upsertAll(remote.map { it.toEntity() })
        return remote
    }

    suspend fun sendText(sessionId: String, jid: String, text: String): Boolean =
        api.sendMessage(sessionId, jid, text)

    suspend fun clearForJid(jid: String) = dao.clearForJid(jid)
}
