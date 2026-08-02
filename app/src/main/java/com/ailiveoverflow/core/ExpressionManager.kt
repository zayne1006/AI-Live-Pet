package com.ailiveoverflow.core

import android.content.Context
import android.util.Log

/**
 * 表情与情绪管理器
 * 负责：当前状态、气泡、孤独递进、Heat、通知文案
 */
class ExpressionManager(private val context: Context) {
    companion object {
        private const val TAG = "ExpressionManager"
        private const val IDLE_BLINK_INTERVAL = 4000L
        private const val LONELINESS_SNEAK_MIN = 5
        private const val LONELINESS_SLEEP_MIN = 30
    }

    var currentState = "idle"
    private var lastInteraction = System.currentTimeMillis()
    private var heat = 0
    private var blinkTimer: java.util.TimerTask? = null

    // 自言自语词池
    private val chatPools = mapOf(
        "daily" to listOf(
            "今天也要加油哦", "在干嘛呢", "累不累呀", "记得休息",
            "想你了", "今天天气好", "要不要喝杯水", "嗯～"
        ),
        "clingy" to listOf(
            "看看我～", "理理我嘛", "不要只玩别的 app", "我在呢",
            "你理我一下？", "我超可爱的", "摸摸我"
        ),
        "chaos" to listOf(
            "🌀 精神错乱中", "我是谁我在哪", "啊——", "为什么",
            "不！不想工作！", "摸鱼万岁", "再戳一下试试？"
        ),
        "late" to listOf(
            "该睡了...", "眼睛会红的", "不要熬夜啊", "晚安...",
            "已经好晚了", "我在等你睡觉", "不睡我也要睡了"
        )
    )

    // 通知碎碎念词池
    private val notificationPools = mapOf(
        6 to "早呀～今天也要开心",
        8 to "起床了吗？别赖床哦",
        12 to "午饭时间啦，别饿着",
        15 to "下午茶时间，休息一下",
        18 to "下班/放学了！好开心",
        20 to "在做什么呢？",
        22 to "夜深了，该休息了吧",
        24 to "还没睡？..."
    )

    // 初始化默认眨眼循环
    fun startIdleLoop(handler: android.os.Handler) {
        blinkTimer = object : java.util.TimerTask() {
            override fun run() {
                if (currentState == "idle") {
                    handler.post {
                        // blink
                        val service = (context as? android.app.Service)?.let {
                            context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                        }
                        currentState = "blink"
                        handler.postDelayed({ currentState = "idle" }, 200)
                    }
                }
                scheduleAutoChat(handler)
            }
        }
        java.util.Timer().schedule(blinkTimer, IDLE_BLINK_INTERVAL, IDLE_BLINK_INTERVAL)
    }

    private fun scheduleAutoChat(handler: android.os.Handler) {
        if (!StateManager(context).autoChatEnabled) return
        val hour = android.text.format.Time(context.resources.configuration.locale).hour
        val pool = if (hour in 22..6) chatPools["late"]
            else if (Math.random() < 0.3) chatPools["clingy"]
            else chatPools["daily"]

        val msg = pool?.randomOrNull() ?: chatPools["daily"]?.randomOrNull()
        msg?.let { showBubble(it, "") }
    }

    // ---- 手势响应 ----
    fun onTap() {
        onInteraction()
        if (currentState == "sleep") {
            wakeUp()
            return
        }
        if (Math.random() < 0.5) {
            showBubble("？", "white")
        } else {
            currentState = "happy"
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                currentState = "idle"
            }, 1500)
        }
    }

    fun onDoubleTap() {
        onInteraction()
        if (currentState == "sleep") {
            wakeUp()
            return
        }
        currentState = "happy"
        showBubble("✨", "pink")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            currentState = "idle"
        }, 2000)
    }

    // ---- 交互回调 ----
    fun onInteraction() {
        lastInteraction = System.currentTimeMillis()
        heat = (heat + 10).coerceAtMost(100)
        // 清除孤独状态
        if (currentState == "sleep" || currentState == "snoring") {
            wakeUp()
        }
    }

    // ---- 孤独递进 ----
    fun updateLoneliness() {
        if (currentState != "idle") return
        val elapsedMin = (System.currentTimeMillis() - lastInteraction) / 60000
        when {
            elapsedMin >= LONELINESS_SLEEP_MIN -> {
                currentState = "sleep"
                showBubble("zZz...", "gray")
            }
            elapsedMin >= 20 -> {
                currentState = "idle"
                showBubble("要打瞌睡了...", "gray")
            }
            elapsedMin >= 15 -> {
                showBubble("好无聊啊", "gray")
            }
            elapsedMin >= 10 -> {
                showBubble("吹个泡泡～", "white")
            }
            elapsedMin >= 5 -> {
                showBubble("偷看～", "gray")
            }
        }
    }

    // ---- 充电状态 ----
    fun onCharging() {
        if (currentState != "sleep") {
            currentState = "charging"
            showBubble("充电中～", "green")
        }
    }

    fun onBatteryLow() {
        currentState = "sad"
        showBubble("电量不足...", "red")
    }

    // ---- 唤醒过渡 ----
    private fun wakeUp() {
        if (currentState == "sleep") {
            currentState = "wake"
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                currentState = "idle"
            }, 2000)
        }
    }

    // ---- 气泡 ----
    fun showBubble(text: String, style: String = "white") {
        // 回调到 service 发送 JS
        // ExpressionManager 不直接持有 WebView，通过外部调用
        // 这里只记录，由外部发送
        Log.d(TAG, "Bubble: $text ($style)")
    }

    // ---- Heat ----
    fun updateHeat() {
        if (heat > 0) {
            heat = (heat - 1).coerceAtLeast(0)
        }
    }

    fun getHeat(): Int = heat

    // ---- 通知文案 ----
    fun getNotificationText(): String {
        val hour = android.text.format.Time(context.resources.configuration.locale).hour
        return notificationPools[hour] ?: "AI 桌宠正在运行中"
    }

    private fun <T> List<T>.randomOrNull(): T? {
        return if (isEmpty()) null else random()
    }
}