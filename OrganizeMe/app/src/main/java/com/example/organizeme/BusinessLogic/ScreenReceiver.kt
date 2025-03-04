package com.example.organizeme.BusinessLogic

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

class ScreenReceiver : BroadcastReceiver() {
    private val TAG = "ScreenReceiver"


    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action || Intent.ACTION_LOCKED_BOOT_COMPLETED == intent.action) {
            Toast.makeText(context, "BOOT_COMPLETED is received", Toast.LENGTH_LONG).show()
            Log.d("hhh", "Boot")
            if(!context.isServiceRunning(ScreenTimeService::class.java)) startService(context);
        }
    }


    private fun startService(context: Context) {
        val serviceIntent = Intent(context, ScreenTimeService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }


    @Suppress("DEPRECATION")
    fun <T> Context.isServiceRunning(service: Class<T>) =
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any {
                it.service.className == service.name
            }
}