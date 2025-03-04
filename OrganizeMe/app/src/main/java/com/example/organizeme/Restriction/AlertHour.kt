package com.example.organizeme.Restriction

import android.content.Context
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.organizeme.DB.Dao
import com.example.organizeme.DB.Pickup_Database
import com.example.organizeme.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AlertHour(private val context: Context, wManager: WindowManager) {
    private var windowManager: WindowManager? = wManager
    private var overlayView: View? = null
    private var countTimer: countDownTimer? = null
    private var isShowing = false

    fun show(dayWindow: AlertDay ,focus: Long, db: Pickup_Database, dao: Dao, scope: CoroutineScope, dayID: String, hourId: Int) {
        Log.i("alert", "begin Showing alert hour")
        if(Settings.canDrawOverlays(context)) {
            if (overlayView == null && !dayWindow.isDayAlertShowing()) {
                // Create overlay view
                Log.i("alert", "alert hour layout is null $overlayView")
                overlayView = LayoutInflater.from(context).inflate(R.layout.alert_hour_window, null)
                // Create layout parameters for the overlay view
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    0,
                    170,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSPARENT
                )
                params.gravity = Gravity.BOTTOM

                if(windowManager == null) windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                //TODO

                val dismissButton: Button = overlayView!!.findViewById(R.id.hourAAButton)
                dismissButton.setOnClickListener {
                    scope.launch {dao.updateHourAA(dayID, hourId)}
                    dismiss()
                }


                val AAasync = scope.async {  dao.getHourAA(dayID, hourId) }
                var AA = 0L
                runBlocking { AA = AAasync.await() }
                Log.i("alert", "after alert hour AA is:  $AA")
                if (AA == 0L) {
                    val hourAA_tv: TextView? = overlayView?.findViewById(R.id.hourAA_tv)
                    scope.launch(Dispatchers.Main) {
                        windowManager?.addView(overlayView, params)
                        isShowing = true
                        countTimer = countDownTimer(focus, 1000, hourAA_tv)
                        countTimer?.start()
                    }
                    Log.i("alert", "Showing alert hour")
                    Log.i("alert", "Showing alert hour overlay: $overlayView")
                    Log.i("alert", "Showing alert hour manager: $windowManager")
                }
            }
            else {
                Log.i("alert", "alert hour overlay not null $overlayView")
                if( countTimer==null || !isShowing){
                    overlayView = null
                    Log.i("alert", "hour overlay not null and isShowing $isShowing")
                    Log.i("alert", "hour overlay not null and dayAlertShowing ${dayWindow.isDayAlertShowing()}")
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
                countTimer?.cancel()
                countTimer = null
                overlayView = null

//                windowManager = null
            }
        }
        catch (e: Exception) {
            Log.i("AlertHour","Error is ${e.stackTrace}")
        }
    }

    private inner class countDownTimer(millisInFuture: Long, countDownInterval: Long,v: TextView?) : CountDownTimer(millisInFuture, countDownInterval){
        val hourAA_tv: TextView? = v
        override fun onTick(millisUntilFinished: Long) {
            hourAA_tv?.text = (millisUntilFinished / 1000).toString() + " Seconds Remaining"
//            Log.i("alert", "alert hour timer is:  $millisUntilFinished")
        }
        override fun onFinish() {
               dismiss()
        }

    }
}