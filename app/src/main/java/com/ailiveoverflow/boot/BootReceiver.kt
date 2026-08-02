package com.ailiveoverflow.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ailiveoverflow.overlay.OverlayService
import com.ailiveoverflow.core.StateManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val sm = StateManager(context)
            if (sm.hasOverlayPermission()) {
                context.startForegroundService(Intent(context, OverlayService::class.java))
            }
        }
    }
}