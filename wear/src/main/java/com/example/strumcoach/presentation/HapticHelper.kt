package com.example.strumcoach.presentation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager

class HapticHelper(context: Context) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        vibrator
    }

    fun vibrateBeat(amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        if (amplitude == 0) return
        vibrator.vibrate(VibrationEffect.createOneShot(50, amplitude.coerceIn(1, 255)))
    }

    fun vibrateSuccess(amplitude: Int = 255) {
        if (amplitude == 0) return
        val timings = longArrayOf(0, 100, 50, 100, 50, 200)
        val peak = amplitude.coerceIn(1, 255)
        val amplitudes = intArrayOf(0, peak, 0, peak, 0, peak)
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    fun stop() {
        vibrator.cancel()
    }
}
