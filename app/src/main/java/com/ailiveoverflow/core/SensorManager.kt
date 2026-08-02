package com.ailiveoverflow.core

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.FileObserver
import android.util.Log
import java.io.File

/**
 * 传感器管理器：前台 App 检测、截图检测、电池感知、时段感知
 */
class SensorManager(private val context: Context, private val handler: android.os.Handler) {
    companion object {
        private const val TAG = "SensorManager"
        private const val CHECK_INTERVAL_MS = 3000L
    }

    private lateinit var statsManager: UsageStatsManager
    private var lastApp: String = ""
    private val appChangeCallbacks = mutableListOf<(appName: String) -> Unit>()
    private val screenshotCallbacks = mutableListOf<() -> Unit>()
    private val chargeCallbacks = mutableListOf<(isCharging: Boolean, isLow: Boolean) -> Unit>()
    private var checker: Runnable? = null
    private var screenshotObserver: FileObserver? = null

    fun start() {
        statsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // 前台 App 检测
        checker = object : Runnable {
            override fun run() {
                checkForegroundApp()
                handler.postDelayed(this, CHECK_INTERVAL_MS)
            }
        }
        handler.post(checker!!)

        // 截图检测
        startScreenshotObserver()

        // 电池广播
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(batteryReceiver, filter)
    }

    fun stop() {
        checker?.let { handler.removeCallbacks(it) }
        screenshotObserver?.stopWatching()
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }

    // ---- 前台 App ----
    private fun checkForegroundApp() {
        if (!StateManager(context).hasUsagePermission()) return
        try {
            val now = System.currentTimeMillis()
            val stats = statsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                now - 1000,
                now
            )
            var top: UsageStats? = null
            for (stat in stats) {
                if (stat.lastTimeUsed > (top?.lastTimeUsed ?: 0)) {
                    top = stat
                }
            }
            top?.let {
                val packageName = it.packageName
                val label = getAppLabel(packageName)
                if (packageName != lastApp) {
                    lastApp = packageName
                    onAppChanged(packageName, label)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkForegroundApp failed", e)
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun onAppChanged(packageName: String, label: String) {
        Log.d(TAG, "App changed: $label ($packageName)")
        // 触发 App 反应映射
        for (cb in appChangeCallbacks) cb(label)

        // 内置 App 反应
        when {
            packageName.contains("tiktok") || packageName.contains("com.ss") -> {
                // 抖音 - 吃醋
            }
            packageName.contains("taobao") || packageName.contains("tmall") -> {
                // 淘宝 - 戴金链子
            }
            packageName.contains("learning") || packageName.contains("学习") -> {
                // 学习通 - 搬书
            }
        }
    }

    // ---- 截图检测 ----
    private fun startScreenshotObserver() {
        val screenshotDirs = listOf(
            "/sdcard/Pictures/Screenshots",
            "/sdcard/DCIM/Screenshots",
            "/sdcard/Screenshots"
        )

        for (dir in screenshotDirs) {
            val file = File(dir)
            if (file.exists() && file.isDirectory) {
                screenshotObserver = object : FileObserver(dir, FileObserver.CLOSE_WRITE) {
                    override fun onEvent(event: Int, path: String?) {
                        if (event == FileObserver.CLOSE_WRITE && path != null) {
                            handler.post {
                                for (cb in screenshotCallbacks) cb()
                            }
                        }
                    }
                }
                screenshotObserver?.startWatching()
                break
            }
        }
    }

    // ---- 电池 ----
    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
            val isLow = scale > 0 && level > 0 && level / scale < 0.2

            for (cb in chargeCallbacks) cb(isCharging, isLow)
        }
    }

    // ---- 注册回调 ----
    fun onAppChange(callback: (String) -> Unit) {
        appChangeCallbacks.add(callback)
    }

    fun onScreenshot(callback: () -> Unit) {
        screenshotCallbacks.add(callback)
    }

    fun onCharge(callback: (isCharging: Boolean, isLow: Boolean) -> Unit) {
        chargeCallbacks.add(callback)
    }
}