package com.triptolemus.hngchatbot.activities

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.triptolemus.hngchatbot.R
import kotlinx.android.synthetic.main.activity_chat.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2.*
import com.triptolemus.hngchatbot.ChatAdapter
import com.triptolemus.hngchatbot.RequestBotAsyncTask
import com.triptolemus.hngchatbot.model.Model
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class ChatActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var sessionsClient: SessionsClient
    lateinit var sessions: SessionName

    private var chats = ArrayList<Model.ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        backButton.setOnClickListener(this)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        chatContainer.layoutManager = layoutManager
        chatContainer.adapter = ChatAdapter(chats)
        sendButton.setOnClickListener(this)
        initAI()

        val username = intent.getStringExtra("username")
        val message = "My name is $username"
        initChat(message)
    }

    private fun initChat(message: String){
        if (message.isNotEmpty()){
            val queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build()
            val performRequest = RequestBotAsyncTask(this, sessions, sessionsClient, queryInput)
            performRequest.execute()
        }
    }

    private fun initAI(){
        val stream: InputStream = resources.openRawResource(R.raw.test_agent_credentials)
        val credential: GoogleCredentials = GoogleCredentials.fromStream(stream)
        val projectId: String = (credential as ServiceAccountCredentials).projectId

        val settingsBuilder: SessionsSettings.Builder = SessionsSettings.newBuilder()
        val sessionsSettings: SessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credential)).build()
        sessionsClient = SessionsClient.create(sessionsSettings)
        sessions = SessionName.of(projectId, "123456")
    }

    override fun onClick(item: View?) {
        if (item!!.id == R.id.backButton){
            onBackPressed()
        } else if (item.id == R.id.sendButton){
            val message = messageEdittext.text.toString().trim()
            sendMessage(message)
            messageEdittext.setText("")
        }
    }

    private fun sendMessage(message: String){
        if (message.isNotEmpty()){
            lateinit var chat: Model.ChatMessage
            chat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val time = current.format(formatter)
                Model.ChatMessage("user", message, time)
            } else {
                Model.ChatMessage("user", message)
            }
            chats.add(chat)
            chatContainer.adapter!!.notifyItemInserted(chatContainer.adapter!!.itemCount)
            chatContainer.scrollToPosition(chatContainer.adapter!!.itemCount - 1)
            val queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build()
            val performRequest = RequestBotAsyncTask(this, sessions, sessionsClient, queryInput)
            performRequest.execute()
        }
    }

    fun receiveMessageFromBot(response: DetectIntentResponse){
        val botReply = response.queryResult.fulfillmentText
        lateinit var chat: Model.ChatMessage
        chat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val time = current.format(formatter)
            Model.ChatMessage("bot", botReply, time)
        } else{
            Model.ChatMessage("bot", botReply)
        }
        chats.add(chat)
        chatContainer.adapter!!.notifyItemInserted(chatContainer.adapter!!.itemCount)
        chatContainer.scrollToPosition(chatContainer.adapter!!.itemCount - 1)
    }
}