package com.triptolemus.hngchatbot.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.triptolemus.hngchatbot.room.entities.ChatEntity
import io.reactivex.Completable
import io.reactivex.Flowable

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChat(chatEntity: ChatEntity)


    @Query("DELETE FROM ChatEntity WHERE username = :username")
    fun deleteAllMyChats(username: String) : Completable

    @Query("SELECT * FROM ChatEntity WHERE username = :username ORDER BY id ASC")
    fun getAllChats(username : String) : Flowable<List<ChatEntity>>

}