package com.example.organizeme.Restriction

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import com.example.organizeme.DB.Dao
import com.example.organizeme.DB.Pickup_Database
import com.example.organizeme.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AlertDay(private val context: Context, wManager: WindowManager) {
    private var windowManager: WindowManager? = wManager
    private var overlayView: View? = null
    private var isShowing = false

    fun show(db: Pickup_Database, dao: Dao, scope: CoroutineScope, dayID: String) {
        Log.i("alert", "begin Showing alert day")
        if(Settings.canDrawOverlays(context)) {
            if (overlayView == null) {
                Log.i("alert", "day overlay is null")
                // Create overlay view
                overlayView = LayoutInflater.from(context).inflate(R.layout.alert_window, null)
                // Create layout parameters for the overlay view
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSPARENT
                )
                params.gravity = Gravity.CENTER
                // Get WindowManager
                if(windowManager == null) windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                //TODO

                val dismissButton: Button = overlayView!!.findViewById(R.id.dismissButton)
                dismissButton.setOnClickListener {
                    scope.launch {dao.updateDayAA(dayID)}
                    dismiss()
                }


                val AAasync = scope.async {  dao.getDayAA(dayID) }
                var AA = 0L
                runBlocking { AA = AAasync.await() }
                Log.i("alert", "day AA is $AA")
                if (AA == 0L) {
                    // Add the view to the WindowManager
                    windowManager?.addView(overlayView, params)
                    isShowing = true
//                    overlayView = null
                    Log.i("alert", "Showing alert day")
                    Log.i("alert", "Showing alert day overlay: $overlayView")
                    Log.i("alert", "Showing alert day manager: $windowManager")
                }else {
                    isShowing = false
                }
            }
            else{
                Log.i("alert", "day overlay is not null")
                if (!isShowing){
                    Log.i("alert", "alert day isShowing $isShowing")
                    overlayView = null
                }
            }
        }
    }

    fun dismiss() {
        try {
            if (overlayView != null && windowManager != null) {
                // Remove the overlay view from the WindowManager
                windowManager?.removeView(overlayView)
                isShowing = false
                overlayView = null
//                windowManager = null
            }
        }
        catch (e: Exception) {
            Log.i("AlertDay","Error is ${e.stackTrace}")
        }
    }

    fun isDayAlertShowing(): Boolean {
        return isShowing
    }

}