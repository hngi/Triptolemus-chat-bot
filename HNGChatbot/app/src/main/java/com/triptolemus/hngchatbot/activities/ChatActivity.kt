package com.triptolemus.hngchatbot.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2.*
import com.triptolemus.hngchatbot.ChatAdapter
import com.triptolemus.hngchatbot.R
import com.triptolemus.hngchatbot.RequestBotAsyncTask
import com.triptolemus.hngchatbot.model.Model
import kotlinx.android.synthetic.main.activity_chat.*
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class ChatActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var sessionsClient: SessionsClient
    lateinit var sessions: SessionName

    private var chats = ArrayList<Model.ChatMessage>()
    private var isConnected = false

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

    private fun initChat(message: String) {
        if(isConnected) {
            if (message.isNotEmpty()) {
                val queryInput = QueryInput.newBuilder()
                    .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US"))
                    .build()
                val performRequest = RequestBotAsyncTask(this, sessions, sessionsClient, queryInput)
                performRequest.execute()
            }
        }
    }

    private fun initAI() {
        val stream: InputStream = resources.openRawResource(R.raw.test_agent_credentials)
        val credential: GoogleCredentials = GoogleCredentials.fromStream(stream)
        val projectId: String = (credential as ServiceAccountCredentials).projectId

        val settingsBuilder: SessionsSettings.Builder = SessionsSettings.newBuilder()
        val sessionsSettings: SessionsSettings =
            settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credential))
                .build()
        sessionsClient = SessionsClient.create(sessionsSettings)
        val sessionId = UUID.randomUUID().toString()
        sessions = SessionName.of(projectId, sessionId)
    }

    override fun onClick(item: View?) {
        if (item!!.id == R.id.backButton) {
            onBackPressed()
        } else if (item.id == R.id.sendButton) {
            val message = messageEdittext.text.toString().trim()
            sendMessage(message)
            messageEdittext.setText("")
        }
    }

    private fun sendMessage(message: String) {
        if (message.isNotEmpty()) {
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
            if (isConnected) {
                val queryInput = QueryInput.newBuilder()
                    .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US"))
                    .build()
                val performRequest = RequestBotAsyncTask(this, sessions, sessionsClient, queryInput)
                performRequest.execute()
            }
        }
    }

    fun receiveMessageFromBot(response: DetectIntentResponse) {
        val botReply = response.queryResult.fulfillmentText
        lateinit var chat: Model.ChatMessage
        chat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val time = current.format(formatter)
            Model.ChatMessage("bot", botReply, time)
        } else {
            Model.ChatMessage("bot", botReply)
        }
        chats.add(chat)
        chatContainer.adapter!!.notifyItemInserted(chatContainer.adapter!!.itemCount)
        chatContainer.scrollToPosition(chatContainer.adapter!!.itemCount - 1)
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val notConnected = intent!!.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)
            if (notConnected){
                disconnected()
            } else{
                connected()
            }
        }
    }

    private fun disconnected(){
        internetNotice.visibility = View.VISIBLE
        chatContainer.visibility = View.GONE
        isConnected = false
    }

    private fun connected(){
        internetNotice.visibility = View.GONE
        chatContainer.visibility = View.VISIBLE
        isConnected = true
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onPause() {
        unregisterReceiver(broadcastReceiver)
        super.onPause()
    }
}