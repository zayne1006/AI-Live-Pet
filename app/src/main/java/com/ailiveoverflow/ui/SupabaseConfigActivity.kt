package com.ailiveoverflow.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.ailiveoverflow.R
import com.ailiveoverflow.core.StateManager

/** Supabase 配置页 */
class SupabaseConfigActivity : AppCompatActivity() {
    private lateinit var stateManager: StateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supabase_config)

        stateManager = StateManager(this)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etUrl = findViewById<EditText>(R.id.et_supabase_url)
        val etKey = findViewById<EditText>(R.id.et_supabase_key)

        etUrl.setText(stateManager.supabaseUrl ?: "")
        etKey.setText(stateManager.supabaseKey ?: "")

        findViewById<Button>(R.id.btn_save_supabase).setOnClickListener {
            stateManager.supabaseUrl = etUrl.text.toString().trim().takeIf { it.isNotEmpty() } ?: ""
            stateManager.supabaseKey = etKey.text.toString().trim().takeIf { it.isNotEmpty() } ?: ""
            android.widget.Toast.makeText(this, "Supabase 配置已保存", android.widget.Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}