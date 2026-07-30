package com.operit.androidaiapp.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.*
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.webkit.WebView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.operit.androidaiapp.R

class FloatPetService : Service() {

    companion object {
        const val TAG = "FloatPetService"
        const val CHANNEL_ID = "pet_channel"
        const val NOTIF_ID = 1
        const val ACTION_STOP = "com.operit.androidaiapp.STOP"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var webView: WebView
    private val webViewParams: LayoutParams by lazy {
        LayoutParams(220, 220,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            x = 50
            y = 500
        }
    }

    // === 状态机 ===
    enum class PetState { IDLE, BLINK, SLEEP, SHY, ANGRY, EAT, TALK, LOVE, PEACE }
    private var currentState = PetState.IDLE

    // 情绪值
    var heat = 30 // 0-100
    private var lastInteractionTime = System.currentTimeMillis()

    // 触控
    private val onTouchListener = OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.x = event.rawX - webViewParams.x
                v.y = event.rawY - webViewParams.y
                updatePosition()
                heat = minOf(100, heat + 5)
                lastInteractionTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_MOVE -> {
                webViewParams.x = (event.rawX - v.x).toInt()
                webViewParams.y = (event.rawY - v.y).toInt()
                updatePosition()
            }
            MotionEvent.ACTION_UP -> {
                heat = minOf(100, heat + 10)
                toggleState()
            }
        }
        true
    }

    // === Handler 定时 ===
    private val handler = Handler(Looper.getMainLooper())
    private val idleTimer = object : Runnable {
        override fun run() {
            checkLoneliness()
            handler.postDelayed(this, 30000)
        }
    }
    private val heatTimer = object : Runnable {
        override fun run() {
            heat = maxOf(0, heat - 1)
            updateHeatVisual()
            handler.postDelayed(this, 30000)
        }
    }
    private val appChecker = object : Runnable {
        override fun run() {
            val app = getCurrentApp()
            if (app != null && app != currentApp) {
                currentApp = app
                onAppChanged(app)
                Log.d(TAG, "App changed to: $app")
            }
            handler.postDelayed(this, 3000)
        }
    }

    private var currentApp: String? = null

    // === 前台通知 ===
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "AI Pet", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "AI floating pet notification" }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Pet")
            .setContentText("Pet is watching you")
            .setSmallIcon(R.drawable.ic_pet)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)

        initWebView()
        handler.postDelayed(idleTimer, 30000)
        handler.postDelayed(heatTimer, 30000)
        handler.post(appChecker)

        return START_STICKY
    }

    private fun initWebView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            setBackgroundColor(0x00000000)
            settings.domStorageEnabled = true
            onTouchListener = onTouchListener
        }

        webView.loadUrl("file:///android_asset/pet.html")
        webViewParams.width = 220
        webViewParams.height = 220
        webViewParams.x = 50
        webViewParams.y = 500
        webViewParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        windowManager.addView(webView, webViewParams)

        Log.d(TAG, "WebView added to window manager")
    }

    private fun updatePosition() {
        try {
            windowManager.updateViewLayout(webView, webViewParams)
        } catch (e: Exception) {
            Log.e(TAG, "updatePosition error", e)
        }
    }

    private fun updateHeatVisual() {
        val heatColor = when {
            heat >= 70 -> "#FF0000"
            heat >= 40 -> "#FF8800"
            else -> "#880000"
        }
        webView.evaluateJavascript(
            "setHeat($heat, '$heatColor');", null
        )
    }

    private fun toggleState() {
        currentState = when (currentState) {
            PetState.IDLE -> {
                val states = listOf(PetState.BLINK, PetState.LOVE, PetState.EAT)
                states[(Math.random() * states.size).toInt()]
            }
            PetState.LOVE -> PetState.SHY
            else -> PetState.IDLE
        }
        webView.evaluateJavascript(
            "setExpression('${currentState.name}');", null
        )
        handler.postDelayed({
            if (currentState != PetState.IDLE) {
                currentState = PetState.IDLE
                webView.evaluateJavascript("setExpression('IDLE');", null)
            }
        }, 3000)
    }

    private fun checkLoneliness() {
        val minutesSinceInteraction = (System.currentTimeMillis() - lastInteractionTime) / 60000
        when {
            minutesSinceInteraction >= 30 -> {
                if (currentState != PetState.SLEEP) {
                    currentState = PetState.SLEEP
                    webView.evaluateJavascript("setExpression('SLEEP');", null)
                }
            }
            minutesSinceInteraction >= 20 -> {
                if (currentState != PetState.EAT) {
                    currentState = PetState.EAT
                    webView.evaluateJavascript("setExpression('EAT');", null)
                }
            }
            minutesSinceInteraction >= 15 -> {
                if (currentState != PetState.PEACE) {
                    currentState = PetState.PEACE
                    webView.evaluateJavascript("setExpression('PEACE');", null)
                }
            }
            minutesSinceInteraction >= 10 -> {
                if (currentState != PetState.SHY) {
                    currentState = PetState.SHY
                    webView.evaluateJavascript("setExpression('SHY');", null)
                }
            }
        }
    }

    private fun onAppChanged(app: String) {
        val response = when {
            app.contains("抖音", ignoreCase = true) -> "ANGRY"
            app.contains("微信", ignoreCase = true) -> "TALK"
            app.contains("学习", ignoreCase = true) -> "LOVE"
            app.contains("QQ", ignoreCase = true) -> "EAT"
            else -> "IDLE"
        }
        if (currentState != PetState.SLEEP) {
            currentState = PetState.valueOf(response)
            webView.evaluateJavascript(
                "setExpression('$response'); showBubble('$app');", null
            )
            handler.postDelayed({
                if (currentState != PetState.SLEEP && currentState == PetState.valueOf(response)) {
                    currentState = PetState.IDLE
                    webView.evaluateJavascript("setExpression('IDLE');", null)
                }
            }, 5000)
        }
    }

    private fun getCurrentApp(): String? {
        val usageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            } catch (e: Exception) {
                return null
            }
        } else return null

        val now = System.currentTimeMillis()
        val stats = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            usageStatsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_BEST, now - 10000, now
            )
        } else {
            return null
        }

        if (stats.isNotEmpty()) {
            var top = stats[0]
            for (s in stats) {
                if (s.lastTimeUsed > top.lastTimeUsed) top = s
            }
            return top.packageName
        }
        return null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::webView.isInitialized) {
            try { windowManager.removeView(webView) } catch (e: Exception) { }
        }
        super.onDestroy()
    }
}