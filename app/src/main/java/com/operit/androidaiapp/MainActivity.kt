package com.operit.androidaiapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.operit.androidaiapp.service.FloatPetService

class MainActivity : ComponentActivity() {
    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { startService() }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        val permissionOk by remember { mutableStateOf(checkAllPermissions()) }
        LaunchedEffect(permissionOk) {
            if (permissionOk) startService()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("*", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "AI Pet",
                fontSize = 28.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF7C3AED)
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (permissionOk) {
                Text("Starting...", fontSize = 16.sp, color = Color(0xFF10B981))
            } else {
                Button(
                    onClick = { requestOverlayPermission() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Text("Enable Overlay", color = Color.White)
                }
            }
        }
    }

    private fun checkAllPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) return false
        }
        return true
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            overlayLauncher.launch(intent)
        } else {
            startService()
        }
    }

    private fun startService() {
        val svc = Intent(this, FloatPetService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainScreen() }
    }
}
