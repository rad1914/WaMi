// @path: app/src/main/java/com/radwrld/wami/data/local/AppDatabase.kt
package com.radwrld.wami.data.local

import android.content.Context
import androidx.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
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
    val jid: String,
    val reactions: String
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats")
    fun getAll(): Flow<List<ChatEntity>>

    @Upsert
    suspend fun upsertAll(chats: List<ChatEntity>)

    @Query("DELETE FROM chats")
    suspend fun clear()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE jid = :jid ORDER BY timestamp DESC")
    fun getForJid(jid: String): Flow<List<MessageEntity>>

    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

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
        private const val DB_NAME = "wami_db"
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { INSTANCE = it }
            }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext app: Context): AppDatabase = AppDatabase.getInstance(app)

    @Provides
    @Singleton
    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()

    @Provides
    @Singleton
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()
}

fun ChatEntity.toChat(): com.radwrld.wami.data.Chat {
    return com.radwrld.wami.data.Chat(
        jid = this.jid,
        name = this.name
    )
}

fun com.radwrld.wami.data.Chat.toEntity(): ChatEntity {
    return ChatEntity(
        jid = this.jid,
        name = this.name
    )
}

fun MessageEntity.toMessage(): com.radwrld.wami.data.Message {
    return com.radwrld.wami.data.Message(
        id = this.id,
        fromMe = this.fromMe,
        text = this.text,
        timestamp = this.timestamp,
        jid = this.jid,
        reactions = this.reactions
    )
}

fun com.radwrld.wami.data.Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = this.id,
        fromMe = this.fromMe,
        text = this.text,
        timestamp = this.timestamp,
        jid = this.jid,
        reactions = this.reactions
    )
}
