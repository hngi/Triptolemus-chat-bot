package com.triptolemus.hngchatbot.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.triptolemus.hngchatbot.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_onboarding.*

class OnBoardingActivity : AppCompatActivity(), View.OnClickListener{

    private var isConnected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        startButton.setOnClickListener(this)
    }

    private fun navigateToChatActivity(username: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("username", username)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onClick(item: View?) {
        val username: String = usernameEdittext.text.toString().trim()
        if (item!!.id == R.id.startButton){
            if (username.isNotEmpty() && isConnected) {
                navigateToChatActivity(username)
                usernameEdittext.setText("")
            } else if (username.isNotEmpty() && !isConnected){
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.network_failure))
                    .setMessage(getString(R.string.continue_prompt))
                    .setPositiveButton(getString(R.string.open_network_settings)) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                        startActivity(intent)
                    }
                    .setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
            else{
                usernameEdittext.error
                Toast.makeText(this, getString(R.string.enter_username), Toast.LENGTH_LONG).show()
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
    }

    private fun connected() {
        isConnected = true
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(broadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onStop() {
        unregisterReceiver(broadcastReceiver)
        super.onStop()
    }
}
