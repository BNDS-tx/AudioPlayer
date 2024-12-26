package com.bnds.audioplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PlayerControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val serviceIntent = Intent(context, PlayerService::class.java)
        when (action) {
            "PAUSE_PLAY_ACTION" -> {
                serviceIntent.action = "TOGGLE_PLAY_PAUSE"
            }
            "PLAY_NEXT" -> {
                serviceIntent.action = "PLAY_NEXT"
            }
            "PLAY_PREVIOUS" -> {
                serviceIntent.action = "PLAY_PREVIOUS"
            }
        }
        if (action != null) {
            Log.d("PlayerControlReceiver", "Received action: $action")
        }
        context.startService(serviceIntent)
    }
}
