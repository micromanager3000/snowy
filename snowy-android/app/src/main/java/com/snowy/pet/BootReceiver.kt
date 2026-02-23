package com.snowy.pet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Boot completed — starting Snowy")
            val serviceIntent = Intent(context, ZeroClawService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
