package com.kousuan.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnPermOverlay: Button
    private lateinit var btnPermAcc: Button
    private lateinit var btnStartHelper: Button

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = mpManager.getMediaProjection(result.resultCode, result.data!!)
            
            val captureManager = ScreenCaptureManager(this)
            captureManager.initProjection(projection)
            FloatingWindowService.captureManager = captureManager

            // 启动悬浮窗前台服务
            val serviceIntent = Intent(this, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            Toast.makeText(this, "⚡ 秒答悬浮窗已开启！请进入小猿口算", Toast.LENGTH_LONG).show()
            finish() // 最小化回到桌面
        } else {
            Toast.makeText(this, "需要授予截屏权限才能识别题目！", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPermOverlay = findViewById(R.id.btnPermOverlay)
        btnPermAcc = findViewById(R.id.btnPermAcc)
        btnStartHelper = findViewById(R.id.btnStartHelper)

        btnPermOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        btnPermAcc.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnStartHelper.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先开启【悬浮窗权限】", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (KousuanAccessibilityService.instance == null) {
                Toast.makeText(this, "请先开启【无障碍服务】", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 请求截屏权限
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            screenCaptureLauncher.launch(mpManager.createScreenCaptureIntent())
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionButtons()
    }

    private fun updatePermissionButtons() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            btnPermOverlay.text = "已开启 ✓"
            btnPermOverlay.isEnabled = false
            btnPermOverlay.setBackgroundColor(0xFF22C55E.toInt())
        }
        if (KousuanAccessibilityService.instance != null) {
            btnPermAcc.text = "已开启 ✓"
            btnPermAcc.isEnabled = false
            btnPermAcc.setBackgroundColor(0xFF22C55E.toInt())
        }
    }
}
