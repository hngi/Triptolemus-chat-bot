package com.triptolemus.hngchatbot.model

object Model {
    data class ChatMessage(val msgUser: String,
                           val msgText: String,
                           val msgTime: String){
        constructor(msgUser: String, msgText: String) : this(msgUser, msgText, " ")
    }
}