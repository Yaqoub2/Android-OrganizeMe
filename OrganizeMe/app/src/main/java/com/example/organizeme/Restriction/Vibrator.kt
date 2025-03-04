package com.example.organizeme.Restriction

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.content.ContextCompat.getSystemService

class Vibrator {
    fun vibratePhone(context: Context, milliseconds: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, 100))
        }

    }}