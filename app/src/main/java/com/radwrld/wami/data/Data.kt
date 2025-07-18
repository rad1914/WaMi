// @path: app/src/main/java/com/radwrld/wami/data/Data.kt
package com.radwrld.wami.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.serialization.encodeToString

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

class ApiService @Inject constructor(
    private val client: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

 
    private inline fun <T> safeCall(block: () -> T): T? 
        = try { block() } catch (_: Exception) { null } 

    suspend fun createSession(): String?
        = withContext(Dispatchers.IO) { 
        safeCall {
            val req = Request.Builder()
                .url("${Constants.BASE_URL}/session/create")
                .post("".toRequestBody())
                .build()
            client.newCall(req).execute().use { resp ->
           
                if (!resp.isSuccessful) return@safeCall null
                json.decodeFromString<CreateSessionResponse>(resp.body!!.string()).sessionId
            }
        }
    }

    suspend fun getStatus(sessionId: String): StatusResponse?
        = withContext(Dispatchers.IO) { 
        safeCall {
            val req = Request.Builder()
                .url("${Constants.BASE_URL}/session/status")
                .header("Authorization", "Bearer $sessionId")
                .build()
            client.newCall(req).execute().use { resp ->
         
                if (!resp.isSuccessful) return@safeCall null
                json.decodeFromString(resp.body!!.string())
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
            .url("${Constants.BASE_URL}/messages?jid=${jid}")
  
            .header("Authorization", "Bearer $sessionId") 
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) emptyList()
            else json.decodeFromString(resp.body!!.string())
        }
    } ?: emptyList()

    suspend fun sendMessage(sessionId: String, jid: String, text: String): Boolean = safeCall {
        val 
        bodyJson = json.encodeToString(SendMessageRequest.serializer(), SendMessageRequest(jid, text)) 
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/message/send")
            .header("Authorization", "Bearer $sessionId")
            .post(bodyJson.toRequestBody())
            .build()
        client.newCall(req).execute().use { resp -> resp.isSuccessful }
    } ?: false
}

@Serializable
data class CreateSessionResponse(val sessionId: String)

@Serializable
data class StatusResponse(val connected: Boolean, val qr: String? = null) 

@Serializable
data class Chat(val jid: String, val name: String)

enum class MessageStatus {
    SENDING,
    FAILED,
    SENT
}

@Serializable
data class Message(
    val id: String,
    val fromMe: Boolean,
    val text: String?,
    val timestamp: Long,
    val jid: String,
    val reactions: String = "",
    val status: MessageStatus = MessageStatus.SENT
)

@Serializable
data class SendMessageRequest(val jid: String, val text: String)

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val SESSION_KEY = stringPreferencesKey("session_id") 

    val sessionIdFlow: Flow<String?> = context.dataStore.data
        .map { it[SESSION_KEY] }

    suspend fun getSessionId(): String?
        = sessionIdFlow.firstOrNull() 

    suspend fun saveSessionId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[SESSION_KEY] = id
        }
    }

    suspend fun clearSessionId() {
        context.dataStore.edit { prefs ->
            prefs.remove(SESSION_KEY)
        }
    }
}

@Singleton
class ChatRepository @Inject constructor(
    private val dao: ChatDao,
  
    private val api: ApiService, 
    private val prefs: UserPreferencesRepository
) {
    fun observeChats(): Flow<List<Chat>> =
        dao.getAll().map { list -> list.map { it.toChat() } }

    suspend fun refreshChats(): List<Chat> {
        val session = prefs.getSessionId() ?: return emptyList()
        val remote = api.fetchChats(session)
        dao.upsertAll(remote.map { it.toEntity() })
        return remote
    }

    suspend fun clearLocal() 
        = dao.clear() 
}

@Singleton
class MessageRepository @Inject constructor(
    private val dao: MessageDao,
    private val api: ApiService,
    private val prefs: UserPreferencesRepository
) {
    fun observeMessages(jid: String): Flow<List<Message>> =
        dao.getForJid(jid).map { list -> list.map { it.toMessage() } }

    suspend fun refreshMessages(jid: String): List<Message> {
        val session = prefs.getSessionId() ?: return emptyList()
        val remote = api.fetchMessages(session, jid)
        dao.upsertAll(remote.map { it.toEntity() })
    
        return remote 
    }

    suspend fun sendText(sessionId: String, jid: String, text: String): Boolean {
        return api.sendMessage(sessionId, jid, text)
    }

    suspend fun clearForJid(jid: String) = dao.clearForJid(jid)
}
