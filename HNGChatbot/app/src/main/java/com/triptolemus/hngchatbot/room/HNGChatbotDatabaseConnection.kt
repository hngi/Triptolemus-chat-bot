package com.triptolemus.hngchatbot.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.triptolemus.hngchatbot.room.dao.ChatDao
import com.triptolemus.hngchatbot.room.entities.ChatEntity

@Database(
    entities = [ChatEntity::class],
    version = 1
)
abstract class HNGChatbotDatabaseConnection : RoomDatabase() {

    abstract fun getChatsDao(): ChatDao

    companion object {
        @Volatile
        private var instance: HNGChatbotDatabaseConnection? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance
            ?: synchronized(LOCK) {
                instance
                    ?: buildDatabase(context).also {
                        instance = it
                    }
            }


        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            HNGChatbotDatabaseConnection::class.java,
            "hngchatbotdatabase"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}