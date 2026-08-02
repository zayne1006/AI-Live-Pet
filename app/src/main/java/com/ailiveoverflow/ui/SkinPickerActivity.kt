package com.ailiveoverflow.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ailiveoverflow.R
import com.ailiveoverflow.core.StateManager
import com.ailiveoverflow.skin.SkinManager
import com.ailiveoverflow.overlay.OverlayService

/** 皮肤选择器：查看 / 切换 / 删除 / 导入 */
class SkinPickerActivity : AppCompatActivity() {
    private lateinit var skinManager: SkinManager
    private lateinit var stateManager: StateManager
    private lateinit var skinList: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skin_picker)

        skinManager = SkinManager(this)
        stateManager = StateManager(this)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<Button>(R.id.btn_import).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/zip"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, 1002)
        }

        findViewById<Button>(R.id.btn_refresh).setOnClickListener {
            loadSkinList()
        }

        findViewById<Button>(R.id.btn_switch).setOnClickListener {
            val spinner = findViewById<android.widget.Spinner>(R.id.sp_skin)
            val selected = spinner.selectedItem as String
            stateManager.currentSkin = selected
            android.widget.Toast.makeText(this, "已切换为 [$selected]", android.widget.Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_delete).setOnClickListener {
            val spinner = findViewById<android.widget.Spinner>(R.id.sp_skin)
            val selected = spinner.selectedItem as String
            if (skinManager.deleteSkin(selected)) {
                loadSkinList()
                android.widget.Toast.makeText(this, "已删除 [$selected]", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        loadSkinList()
    }

    private fun loadSkinList() {
        skinList = skinManager.listSkins().toMutableList()
        if (skinList.isEmpty()) {
            findViewById<TextView>(R.id.tv_empty).visibility = View.VISIBLE
            findViewById<android.widget.AdapterView<*>>(R.id.sp_skin).visibility = View.GONE
            findViewById<Button>(R.id.btn_switch).visibility = View.GONE
            findViewById<Button>(R.id.btn_delete).visibility = View.GONE
            return
        }
        findViewById<TextView>(R.id.tv_empty).visibility = View.GONE
        findViewById<android.widget.AdapterView<*>>(R.id.sp_skin).visibility = View.VISIBLE
        findViewById<Button>(R.id.btn_switch).visibility = View.VISIBLE
        findViewById<Button>(R.id.btn_delete).visibility = View.VISIBLE

        val spinner = findViewById<Spinner>(R.id.sp_skin)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, skinList)
        (spinner.adapter as ArrayAdapter<String>).dropDownViewResource = android.R.layout.simple_spinner_dropdown_item

        val currentPos = skinList.indexOf(stateManager.currentSkin)
        spinner.setSelection(if (currentPos >= 0) currentPos else 0)
    }

    override fun onResume() {
        super.onResume()
        loadSkinList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val name = uri.lastPathSegment?.substringBeforeLast(".") ?: "custom"
                val isOk = skinManager.importSkin(uri, name)
                if (isOk) {
                    loadSkinList()
                    android.widget.Toast.makeText(this, "皮肤导入成功！", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(this, "导入失败，请检查文件格式", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}