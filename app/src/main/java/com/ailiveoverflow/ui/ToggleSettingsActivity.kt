package com.ailiveoverflow.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ailiveoverflow.R
import com.ailiveoverflow.core.StateManager

/** 行为开关设置 */
class ToggleSettingsActivity : AppCompatActivity() {
    private lateinit var stateManager: StateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_toggle_settings)

        stateManager = StateManager(this)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val swChat = findViewById<Switch>(R.id.sw_auto_chat)
        val swNotify = findViewById<Switch>(R.id.sw_notify_chat)

        swChat.isChecked = stateManager.autoChatEnabled
        swNotify.isChecked = stateManager.notificationChatEnabled

        swChat.setOnCheckedChangeListener { _, isChecked ->
            stateManager.autoChatEnabled = isChecked
        }

        swNotify.setOnCheckedChangeListener { _, isChecked ->
            stateManager.notificationChatEnabled = isChecked
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}