package com.drawing.paint

import android.content.Context
import android.net.ConnectivityManager


class CheckInternet {
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetwork
        //should check null because in airplane mode it will be null
        return networkInfo != null
    }
}