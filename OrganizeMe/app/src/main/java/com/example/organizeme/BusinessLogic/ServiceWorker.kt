package com.example.organizeme.BusinessLogic

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startForegroundService
import androidx.work.Worker
import androidx.work.WorkerParameters

class ServiceWorker(private val appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {

        // Do the work here--in this case
        if (!appContext.isServiceRunning(ScreenTimeService::class.java)) {
            try {
                val intent1 = Intent(applicationContext, ScreenTimeService::class.java)
                startForegroundService(applicationContext, intent1);
                Log.i("MyWorker", "worker created Service")
            } catch (e: Exception) {
                Log.i("MyWorker", e.message.toString())
            }
        } else {
            Log.i("MyWorker", "Background task is running")
        }
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }

    @Suppress("DEPRECATION")
    fun <T> Context.isServiceRunning(service: Class<T>) =
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any {
                it.service.className == service.name
            }
}
