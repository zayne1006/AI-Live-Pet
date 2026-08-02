package com.ailiveoverflow.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ailiveoverflow.R
import com.ailiveoverflow.core.StateManager
import com.ailiveoverflow.skin.SkinManager

/** 主设置页 */
class MainActivity : AppCompatActivity() {
    private lateinit var stateManager: StateManager
    private lateinit var skinManager: SkinManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ui_main)

        stateManager = StateManager(this)
        skinManager = SkinManager(this)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<Button>(R.id.btn_skin).setOnClickListener {
            startActivity(Intent(this, SkinPickerActivity::class.java))
        }

        findViewById<Button>(R.id.btn_supabase).setOnClickListener {
            startActivity(Intent(this, SupabaseConfigActivity::class.java))
        }

        findViewById<Button>(R.id.btn_size).setOnClickListener {
            val sizeDialog = SizeDialogFragment(stateManager)
            sizeDialog.show(supportFragmentManager, "size_dialog")
        }

        findViewById<Button>(R.id.btn_switch_toggles).setOnClickListener {
            startActivity(Intent(this, ToggleSettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btn_about).setOnClickListener {
            android.widget.Toast.makeText(this, "AI 桌宠 v1.0 — 让 AI 住进你的屏幕", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (isTaskRoot) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}