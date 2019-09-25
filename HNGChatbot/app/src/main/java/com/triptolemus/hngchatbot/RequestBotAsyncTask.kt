package com.triptolemus.hngchatbot

import android.app.Activity
import android.os.AsyncTask
import com.google.cloud.dialogflow.v2.*
import com.triptolemus.hngchatbot.activities.ChatActivity
import java.lang.Exception

class RequestBotAsyncTask(private val activity: Activity,
                          private val session: SessionName,
                          private val sessionsClient: SessionsClient,
                          private val queryInput: QueryInput): AsyncTask<Void, Void, DetectIntentResponse>() {

    override fun doInBackground(vararg p0: Void?): DetectIntentResponse? {
        try {
            val detectIntentRequest: DetectIntentRequest = DetectIntentRequest.newBuilder()
                .setSession(session.toString())
                .setQueryInput(queryInput)
                .build()
            return sessionsClient.detectIntent(detectIntentRequest)
        } catch (err: Exception){
            err.printStackTrace()
        }
        return null
    }

    override fun onPostExecute(result: DetectIntentResponse?) {
        (activity as ChatActivity).receiveMessageFromBot(result!!)
    }
}