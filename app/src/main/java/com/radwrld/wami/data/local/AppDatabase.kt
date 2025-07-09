// @path: app/src/main/java/com/radwrld/wami/data/local/AppDatabase.kt
package com.radwrld.wami.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.radwrld.wami.data.Chat
import com.radwrld.wami.data.Message
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val jid: String,
    val name: String
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val fromMe: Boolean,
    val text: String?,
    val timestamp: Long,
    val jid: String
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats")
    fun getAll(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chats: List<ChatEntity>)

    @Query("DELETE FROM chats")
    suspend fun clear()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE jid = :jid ORDER BY timestamp DESC")
    fun getMessagesForJid(jid: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE jid = :jid")
    suspend fun clearForJid(jid: String)

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}

@Database(
    entities = [ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {

        @Volatile private var INSTANCE: AppDatabase? = null

        
        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wami_db"
                ).build().also { INSTANCE = it }
            }
    }
}

fun ChatEntity.toChat() = Chat(jid = jid, name = name)

fun Chat.toEntity() = ChatEntity(jid = jid, name = name)

fun MessageEntity.toMessage() =
    Message(id = id, fromMe = fromMe, text = text, timestamp = timestamp)

fun Message.toEntity(jid: String) =
    MessageEntity(id = id, fromMe = fromMe, text = text, timestamp = timestamp, jid = jid)
