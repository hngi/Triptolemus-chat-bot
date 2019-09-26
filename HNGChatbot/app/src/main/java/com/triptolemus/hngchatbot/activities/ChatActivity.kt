package com.triptolemus.hngchatbot.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2.*
import com.triptolemus.hngchatbot.ChatAdapter
import com.triptolemus.hngchatbot.R
import com.triptolemus.hngchatbot.RequestBotAsyncTask
import com.triptolemus.hngchatbot.model.Model
import com.triptolemus.hngchatbot.room.HNGChatbotDatabaseConnection
import com.triptolemus.hngchatbot.room.entities.ChatEntity
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableObserver
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_chat.*
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class ChatActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var sessionsClient: SessionsClient
    private lateinit var sessions: SessionName
    private lateinit var database: HNGChatbotDatabaseConnection
    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var adapter: ChatAdapter

    private var chats = ArrayList<Model.ChatMessage>()
    private var username: String? = null
    private var isConnected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        database = HNGChatbotDatabaseConnection.invoke(this.applicationContext)
        compositeDisposable = CompositeDisposable()

        backButton.setOnClickListener(this)
        clearChats.setOnClickListener(this)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        chatContainer.layoutManager = layoutManager
        adapter = ChatAdapter(chats)
        chatContainer.adapter = adapter
        sendButton.setOnClickListener(this)
        initAI()
        username = intent.getStringExtra("username")
        getAllChatsInit()
    }

    private fun initChat(message: String) {
        if (message.isNotEmpty()) {
            val queryInput = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US"))
                .build()
            val performRequest = RequestBotAsyncTask(this, sessions, sessionsClient, queryInput)
            performRequest.execute()
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
        when {
            item!!.id == R.id.backButton -> {
                onBackPressed()
                finish()
            }
            item.id == R.id.sendButton -> {
                val message = messageEdittext.text.toString().trim()
                sendMessage(message)
                messageEdittext.setText("")
            }
            item.id == R.id.clearChats -> {
                AlertDialog.Builder(this)
                    .setMessage("Do you want to clear all your chats?")
                    .setPositiveButton("Yes") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                        username?.let {
                            database.getChatsDao().deleteAllMyChats(it)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(object : DisposableCompletableObserver() {
                                    override fun onComplete() {
                                        Toast.makeText(
                                            this@ChatActivity,
                                            "cleared",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    override fun onError(e: Throwable) {
                                        Toast.makeText(
                                            this@ChatActivity,
                                            e.message.toString(),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                })
                        }
                    }
                    .setNegativeButton("No") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun sendMessage(message: String) {
        if (isConnected) {
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

                username?.let {
                    val chatDb = ChatEntity(
                        it,
                        chat.msgUser,
                        chat.msgText,
                        chat.msgTime
                    )
                    insertMessageToDb(chatDb) { thisBool ->
                        if (thisBool) {
                            val queryInput = QueryInput.newBuilder()
                                .setText(
                                    TextInput.newBuilder().setText(message).setLanguageCode(
                                        "en-US"
                                    )
                                )
                                .build()
                            val performRequest =
                                RequestBotAsyncTask(this, sessions, sessionsClient, queryInput)
                            performRequest.execute()
                        }
                    }
                }
            }
        }
    }


    private fun getAllChatsInit() {
        val chatLocal = ArrayList<Model.ChatMessage>()
        username?.let { usernameLocal ->
            val allChatsDisposable = database.getChatsDao().getAllChats(usernameLocal)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError {
                    Toast.makeText(this, "an error has occurred", Toast.LENGTH_LONG).show()
                }
                .subscribe { thisIt ->
                    Log.d("HNGData", thisIt.size.toString())
                    if (thisIt.isNotEmpty()) {
                        thisIt.toObservable()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(object : DisposableObserver<ChatEntity>() {
                                override fun onComplete() {
                                    chats.clear()
                                    chats.addAll(chatLocal)
                                    adapter.notifyDataSetChanged()
                                    chatContainer.scrollToPosition(chatContainer.adapter!!.itemCount - 1)
                                    Log.d("HNGData", "chtLocal ==> " + chatLocal.size.toString())
                                    chatLocal.clear()
                                }

                                override fun onNext(it: ChatEntity) {
                                    Log.d("HNGData", "data ==> " + it.toString())
                                    chatLocal.add(
                                        Model.ChatMessage(it.msgUser, it.msgText, it.msgTime)
                                    )
                                }

                                override fun onError(e: Throwable) {}

                            })
                    } else {
                        val message = "My name is $username"
                        initChat(message)
                    }
                }
            allChatsDisposable.addTo(compositeDisposable)
        }
    }

    private fun insertMessageToDb(chatDb: ChatEntity, complete: (Boolean) -> Unit) {
        Completable
            .fromAction {
                database.getChatsDao().insertChat(chatDb)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : DisposableCompletableObserver() {
                override fun onComplete() {
                    complete(true)
                }

                override fun onError(e: Throwable) {
                    complete(false)
                }
            })
    }

    fun receiveMessageFromBot(response: DetectIntentResponse) {
        if (isConnected) {
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

            username?.let {
                val chatDb = ChatEntity(
                    it,
                    chat.msgUser,
                    chat.msgText,
                    chat.msgTime
                )
                insertMessageToDb(chatDb) {

                }
            }
        }
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val notConnected =
                intent!!.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)
            if (notConnected) {
                disconnected()
            } else {
                connected()
            }
        }
    }

    private fun disconnected() {
        isConnected = false
        messageEdittext.isEnabled = false
        Toast.makeText(this, getString(R.string.network_failure), Toast.LENGTH_LONG).show()
    }

    private fun connected() {
        isConnected = true
        messageEdittext.isEnabled = true
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(broadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onStop() {
        unregisterReceiver(broadcastReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }
}