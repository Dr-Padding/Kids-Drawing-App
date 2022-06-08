package com.drawing.paint

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NetworkChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        val status = CheckInternet().isOnline(context)
        if (status) {
             //?????
        } else {
            Toast.makeText(
                context,
                "Please connect to the Internet to access more colors, brushes and erasers!",
                Toast.LENGTH_LONG
            ).show()
        }

    }
}