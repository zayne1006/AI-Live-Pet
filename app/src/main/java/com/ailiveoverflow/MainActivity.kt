package com.ailiveoverflow

import android.content.Intent
import android.os.Bundle
import com.ailiveoverflow.overlay.OverlayService
import com.ailiveoverflow.core.StateManager
import com.ailiveoverflow.core.SkinInitializer
import com.ailiveoverflow.ui.MainActivity as AppMainActivity

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化内置皮肤
        SkinInitializer(this).init()

        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btn_start).setOnClickListener {
            if (StateManager(this).hasOverlayPermission()) {
                startForegroundService(Intent(this, OverlayService::class.java))
                finish()
            } else {
                // request permission
                val intent = android.provider.Settings.Intent("ACTION_MANAGE_OVERLAY_PERMISSION")
                startActivityForResult(intent, 1001)
            }
        }

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btn_usage).setOnClickListener {
            if (!StateManager(this).hasUsagePermission()) {
                val intent = android.provider.Settings.Intent("ACTION_USAGE_ACCESS_SETTINGS")
                startActivity(intent)
            }
        }

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, AppMainActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (StateManager(this).hasOverlayPermission()) {
                startForegroundService(Intent(this, OverlayService::class.java))
                finish()
            } else {
                android.widget.Toast.makeText(this, "悬浮窗权限未授权", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}