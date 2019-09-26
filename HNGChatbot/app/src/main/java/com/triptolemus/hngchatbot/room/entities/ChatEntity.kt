package com.triptolemus.hngchatbot.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ChatEntity(
    val username : String,
    val msgUser: String,
    val msgText: String,
    val msgTime: String
) {
    @PrimaryKey(autoGenerate = true)
    var id : Int = 0
}