package com.ailiveoverflow.overlay

import android.view.MotionEvent
import java.util.concurrent.CopyOnWriteArrayList

/** 手势识别器：单击 / 双击 / 长按 / 甩出 */
abstract class GestureHandler {
    private val tapTimes = mutableListOf<Long>()

    open fun onSingleTap(x: Float, y: Float) {}
    open fun onDoubleTap(x: Float, y: Float) {}
    open fun onLongPress(x: Float, y: Float) {}
    open fun onFling(vx: Float, vy: Float) {}

    companion object {
        private const val DOUBLE_TAP_THRESHOLD = 300L
        private const val LONG_PRESS_THRESHOLD = 800L
    }

    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f

    /** 调用方需要在 ACTION_DOWN / ACTION_UP 时传递 event */
    fun handleEvent(event: MotionEvent, vx: Float, vy: Float) {
        val isUp = event.actionMasked == MotionEvent.ACTION_UP

        if (!isUp) {
            downTime = System.currentTimeMillis()
            downX = event.rawX
            downY = event.rawY
            return
        }

        val elapsed = System.currentTimeMillis() - downTime
        val dx = event.rawX - downX
        val dy = event.rawY - downY

        if (elapsed > LONG_PRESS_THRESHOLD) {
            onLongPress(downX, downY)
        } else {
            val now = System.currentTimeMillis()
            tapTimes.add(now)
            tapTimes.removeAll { now - it > DOUBLE_TAP_THRESHOLD }

            when (tapTimes.size) {
                2 -> onDoubleTap(event.rawX, event.rawY)
                3 -> onSingleTap(event.rawX, event.rawY) // 连击 3 次 = 连续反应
                5 -> onSingleTap(event.rawX, event.rawY) // 连击 5 次
                8 -> onSingleTap(event.rawX, event.rawY) // 连击 8 次
                else -> onSingleTap(event.rawX, event.rawY)
            }
        }

        if (abs(vx) > 300f || abs(vy) > 300f) {
            onFling(vx, vy)
        }
    }

    private fun abs(v: Float): Float {
        return kotlin.math.abs(v)
    }
}