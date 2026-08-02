package com.ailiveoverflow

import android.app.AppOpsManager
import android.content.Context
import android.provider.Settings

class StateManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "ailiveoverflow_prefs"

        // Overlay position
        const val KEY_X = "overlay_x"
        const val KEY_Y = "overlay_y"

        // Skin
        const val KEY_CURRENT_SKIN = "current_skin"

        // Size
        const val KEY_WIDTH = "overlay_width"
        const val KEY_HEIGHT = "overlay_height"

        // Behaviour
        const val KEY_AUTO_CHAT = "auto_chat"
        const val KEY_NOTIFICATION_CHAT = "notification_chat"

        // Supabase
        const val KEY_SUPABASE_URL = "supabase_url"
        const val KEY_SUPABASE_KEY = "supabase_key"
    }

    fun getSharedPreferences(): android.content.SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ---- Permissions ----
    fun hasOverlayPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 23) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun hasUsagePermission(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // ---- Position ----
    var overlayX: Float
        get() = getSharedPreferences().getFloat(KEY_X, 0f)
        set(v) = getSharedPreferences().edit().putFloat(KEY_X, v).apply()

    var overlayY: Float
        get() = getSharedPreferences().getFloat(KEY_Y, 0f)
        set(v) = getSharedPreferences().edit().putFloat(KEY_Y, v).apply()

    // ---- Skin ----
    var currentSkin: String
        get() = getSharedPreferences().getString(KEY_CURRENT_SKIN, "default") ?: "default"
        set(v) = getSharedPreferences().edit().putString(KEY_CURRENT_SKIN, v).apply()

    // ---- Size ----
    var overlayWidth: Int
        get() = getSharedPreferences().getInt(KEY_WIDTH, 220)
        set(v) = getSharedPreferences().edit().putInt(KEY_WIDTH, v).apply()

    var overlayHeight: Int
        get() = getSharedPreferences().getInt(KEY_HEIGHT, 220)
        set(v) = getSharedPreferences().edit().putInt(KEY_HEIGHT, v).apply()

    // ---- Behaviour toggles ----
    var autoChatEnabled: Boolean
        get() = getSharedPreferences().getBoolean(KEY_AUTO_CHAT, true)
        set(v) = getSharedPreferences().edit().putBoolean(KEY_AUTO_CHAT, v).apply()

    var notificationChatEnabled: Boolean
        get() = getSharedPreferences().getBoolean(KEY_NOTIFICATION_CHAT, true)
        set(v) = getSharedPreferences().edit().putBoolean(KEY_NOTIFICATION_CHAT, v).apply()

    // ---- Supabase ----
    var supabaseUrl: String?
        get() = getSharedPreferences().getString(KEY_SUPABASE_URL, "")
        set(v) = getSharedPreferences().edit().putString(KEY_SUPABASE_URL, v ?: "").apply()

    var supabaseKey: String?
        get() = getSharedPreferences().getString(KEY_SUPABASE_KEY, "")
        set(v) = getSharedPreferences().edit().putString(KEY_SUPABASE_KEY, v ?: "").apply()
}