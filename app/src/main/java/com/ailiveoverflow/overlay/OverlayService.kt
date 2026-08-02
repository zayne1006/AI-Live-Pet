package com.ailiveoverflow.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import com.ailiveoverflow.App
import com.ailiveoverflow.R
import com.ailiveoverflow.core.ExpressionManager
import com.ailiveoverflow.core.StateManager
import com.ailiveoverflow.core.SensorManager
import com.ailiveoverflow.skin.SkinManager
import kotlin.math.abs

class OverlayService : Service() {
    companion object {
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var wm: WindowManager
    private var webView: WebView? = null
    private lateinit var wmParams: WindowManager.LayoutParams

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var skinManager: SkinManager
    private lateinit var stateManager: StateManager
    private lateinit var expressionManager: ExpressionManager
    private lateinit var sensorManager: SensorManager
    private lateinit var gestureHandler: GestureHandler

    private var isDragging = false
    private var lastX = 0f
    private var lastY = 0f

    private var flingThreshold = 300f  // px/s

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        skinManager = SkinManager(this)
        stateManager = StateManager(this)
        expressionManager = ExpressionManager(this)
        sensorManager = SensorManager(this, mainHandler)

        gestureHandler = object : GestureHandler {
            override fun onSingleTap(x: Float, y: Float) {
                expressionManager.onTap()
                uploadGesture("tap", x, y)
            }

            override fun onDoubleTap(x: Float, y: Float) {
                expressionManager.onDoubleTap()
                expressionManager.showBubble("❤️", "pink")
                uploadGesture("double_tap", x, y)
            }

            override fun onLongPress(x: Float, y: Float) {
                showSkinPickerPopup(x, y)
                uploadGesture("long_press", x, y)
            }

            override fun onFling(vx: Float, vy: Float) {
                if (abs(vx) > flingThreshold || abs(vy) > flingThreshold) {
                    expressionManager.showBubble("你把我扔出去啦！", "gray")
                    startBringBackAnimation()
                }
            }
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createOverlay()
        sensorManager.start()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        webView?.let {
            if (it.isAttachedToWindow) wm.removeView(it)
            it.destroy()
            webView = null
        }
        sensorManager.stop()
        super.onDestroy()
    }

    // ---- Overlay ----
    private fun createOverlay() {
        val w = stateManager.overlayWidth
        val h = stateManager.overlayHeight
        val x = stateManager.overlayX
        val y = stateManager.overlayY

        webView = WebView(this).apply {
            setBackgroundColor(0x00000000)
            settings.javaScriptEnabled = true
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            settings.domStorageEnabled = true

            webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    // inject current state
                    val frame = expressionManager.currentState
                    view.evaluateJavascript(
                        "if (typeof setFrame === 'function') setFrame('$frame');",
                        null
                    )
                }
            }

            setOnTouchListener { _, event ->
                handleTouch(event)
                true
            }

            setWebViewClient(this@apply)
        }

        wmParams = WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = x.toInt()
            y = y.toInt()
        }

        val html = skinManager.renderSkinToHtml(stateManager.currentSkin)
        webView?.loadData(html, "text/html; charset=utf-8", "UTF-8")

        try {
            wm.addView(webView, wmParams)
            Log.d(TAG, "Overlay added at ($x, $y) size(${w}x${h})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay", e)
        }
    }

    private fun handleTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                lastX = event.rawX
                lastY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastX
                val dy = event.rawY - lastY
                if (abs(dx) > 4f || abs(dy) > 4f) {
                    isDragging = true
                    wmParams.x = (event.rawX - webView!!.width / 2).toInt()
                    wmParams.y = (event.rawY - webView!!.height / 2).toInt()
                    wm.updateViewLayout(webView, wmParams)
                    lastX = event.rawX
                    lastY = event.rawY
                }
            }
            MotionEvent.ACTION_UP -> {
                wmParams.x = webView!!.x.toInt()
                wmParams.y = webView!!.y.toInt()
                stateManager.overlayX = wmParams.x.toFloat()
                stateManager.overlayY = wmParams.y.toFloat()

                if (!isDragging) {
                    gestureHandler.handleEvent(event, 0f, 0f)
                } else {
                    gestureHandler.handleEvent(event,
                        (event.rawX - lastX) / 0.1f,
                        (event.rawY - lastY) / 0.1f
                    )
                }
            }
        }
    }

    private fun startBringBackAnimation() {
        // Fly back to center of screen
        val screenWidth = (wm.defaultDisplay.width / 2 - stateManager.overlayWidth / 2)
        val screenHeight = (wm.defaultDisplay.height / 2 - stateManager.overlayHeight / 2)

        val startX = wmParams.x.toFloat()
        val startY = wmParams.y.toFloat()
        val animStart = System.currentTimeMillis()
        val duration = 800L

        mainHandler.post(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - animStart
                val t = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                val eased = 1f - (1f - t) * (1f - t)

                wmParams.x = (startX + (screenWidth - startX) * eased).toInt()
                wmParams.y = (startY + (screenHeight - startY) * eased).toInt()
                wm.updateViewLayout(webView, wmParams)

                if (t < 1f) {
                    mainHandler.postDelayed(this, 16)
                } else {
                    stateManager.overlayX = screenWidth.toFloat()
                    stateManager.overlayY = screenHeight.toFloat()
                }
            }
        })
    }

    // ---- Skin Picker Popup (Quick Action) ----
    private fun showSkinPickerPopup(x: Float, y: Float) {
        val skins = skinManager.listSkins()
        if (skins.isEmpty()) {
            // no skins, show toast
            android.widget.Toast.makeText(this, "尚无皮肤，请先在设置中导入", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // Use a simple PopupWindow for skin quick switch
        val popupView = android.widget.PopupWindow(
            android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0xEEFFFFFF.toInt()))
                setPadding(8, 8, 8, 8)
                skins.forEach { skinName ->
                    val isActive = skinName == stateManager.currentSkin
                    addView(android.widget.Button(this@OverlayService).apply {
                        text = "◉ ${skinName}".replace("◉", "○")
                        if (isActive) {
                            text = "◉ $skinName"
                            setTextColor(android.graphics.Color.parseColor("#4A6FA5"))
                        }
                        setOnClickListener {
                            switchSkin(skinName)
                            this@PopupWindow.dismiss()
                        }
                    })
                }
            },
            200,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
            elevation = 8f
            showAtLocation(
                webView,
                Gravity.NO_GRAVITY,
                x.toInt(),
                y.toInt()
            )
        }

        // Also show settings button
        expressionManager.showBubble("长按已弹出皮肤菜单", "gray")
    }

    fun switchSkin(name: String) {
        stateManager.currentSkin = name
        expressionManager.showBubble("已切换到 [$name] 皮肤", "pink")
        // Reload WebView with new skin
        webView?.let { wv ->
            val html = skinManager.renderSkinToHtml(name)
            wv.loadData(html, "text/html; charset=utf-8", "UTF-8")
        }
        uploadGesture("switch_skin", stateManager.currentSkin.toFloat(), 0f)
    }

    // ---- WebView Commands ----
    fun sendFrame(state: String) {
        webView?.evaluateJavascript(
            "if (typeof setFrame === 'function') setFrame('$state');",
            null
        )
    }

    fun sendBubble(text: String, style: String = "white") {
        webView?.evaluateJavascript(
            "if (typeof showBubble === 'function') showBubble('$text', '$style');",
            null
        )
    }

    fun sendHeat(level: Int) {
        webView?.evaluateJavascript(
            "if (typeof setHeat === 'function') setHeat($level);",
            null
        )
    }

    // ---- Gesture Upload ----
    private fun uploadGesture(type: String, x: Float, y: Float) {
        expressionManager.onInteraction()
    }

    // ---- Notification ----
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI 桌宠",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val intent = Intent(this, com.ailiveoverflow.MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = android.app.Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AI 桌宠运行中")
            .setContentText(expressionManager.getNotificationText())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .setCategory(android.app.Notification.CATEGORY_SERVICE)

        return builder.build()
    }
}