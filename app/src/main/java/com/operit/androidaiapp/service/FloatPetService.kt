package com.operit.androidaiapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.*
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat

class FloatPetService : Service() {
    companion object {
        const val TAG = "FloatPetService"
        const val CHANNEL_ID = "pet_channel"
        const val NOTIF_ID = 1
        const val ACTION_STOP = "com.operit.androidaiapp.STOP"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var webView: WebView
    private lateinit var layoutParams: LayoutParams

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        layoutParams = LayoutParams(
            220, 220,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { x = 50; y =  500 }

        createNotificationChannel()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            webViewChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/pet.html")
        }
        webView.setOnTouchListener(OnTouchListener { _, event ->
            layoutParams.x = (event.rawX - webView.x).toInt()
            layoutParams.y = (event.rawY - webView.y).toInt()
            windowManager.updateViewLayout(webView, layoutParams)
            true
        })
        windowManager.addView(webView, layoutParams)
        Log.d(TAG, "Service started")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Pet Service", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            onDestroy()
            return START_NOT_STICKY
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Pet")
            .setContentText("Running")
            .setSmallIcon(android.R.drawable.star_on)
            .build()
        startForeground(NOTIF_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.stopLoading()
        webView.destroy()
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
