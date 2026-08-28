package com.kousuan.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams

    private var isRunning = AtomicBoolean(false)
    private var solvedCount = 0
    private var strokeSpeedMs = 20L
    private var lastQuestionText = ""

    private val mainHandler = Handler(Looper.getMainLooper())
    private var loopThread: Thread? = null

    companion object {
        var captureManager: ScreenCaptureManager? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        initFloatingWindow()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "kousuan_floating_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "口算助手前台服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("口算秒答助手正在运行")
            .setContentText("悬浮窗已就绪，点击开始PK")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        startForeground(1001, notification)
    }

    private fun initFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating_window, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        windowManager.addView(floatingView, params)
        setupInteractions()
    }

    private fun setupInteractions() {
        val dragBar = floatingView.findViewById<View>(R.id.layoutDragBar)
        val btnToggle = floatingView.findViewById<Button>(R.id.btnToggle)
        val btnSpeed = floatingView.findViewById<Button>(R.id.btnSpeed)
        val btnClose = floatingView.findViewById<Button>(R.id.btnClose)
        val tvSolvedBadge = floatingView.findViewById<TextView>(R.id.tvSolvedBadge)
        val tvStatus = floatingView.findViewById<TextView>(R.id.tvStatus)

        // 拖动悬浮窗
        dragBar.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })

        // 开始/暂停
        btnToggle.setOnClickListener {
            val running = isRunning.get()
            isRunning.set(!running)
            if (!running) {
                btnToggle.text = "⏸ 暂停"
                btnToggle.setBackgroundColor(0xFFEAB308.toInt())
                tvStatus.text = "⚡ 极速答题中..."
                startSolveLoop(tvSolvedBadge, tvStatus)
            } else {
                btnToggle.text = "▶ 开始"
                btnToggle.setBackgroundColor(0xFF22C55E.toInt())
                tvStatus.text = "已暂停"
            }
        }

        // 速度档位切换
        var speedIndex = 0
        val speeds = arrayOf("⚡ 极限", "🚀 极速", "🎯 稳健")
        val strokeTimes = arrayOf(15L, 35L, 80L)

        btnSpeed.setOnClickListener {
            speedIndex = (speedIndex + 1) % speeds.size
            btnSpeed.text = speeds[speedIndex]
            strokeSpeedMs = strokeTimes[speedIndex]
            tvStatus.text = "切换为: ${speeds[speedIndex]}"
        }

        // 关闭
        btnClose.setOnClickListener {
            stopSelf()
        }
    }

    private fun startSolveLoop(tvSolvedBadge: TextView, tvStatus: TextView) {
        loopThread?.interrupt()
        loopThread = Thread {
            val dm = resources.displayMetrics
            val cx = dm.widthPixels * 0.50f
            val cy = dm.heightPixels * 0.70f
            val size = dm.widthPixels * 0.12f

            while (isRunning.get() && !Thread.currentThread().isInterrupted) {
                val bitmap = captureManager?.captureQuestionBitmap()
                if (bitmap != null) {
                    captureManager?.recognizeText(bitmap) { text ->
                        if (text.isNotBlank() && text != lastQuestionText) {
                            val answer = KousuanSolver.solve(text)
                            if (answer != null) {
                                lastQuestionText = text
                                solvedCount++

                                val accService = KousuanAccessibilityService.instance
                                if (accService != null) {
                                    when (answer) {
                                        ">" -> accService.drawGreaterThan(cx, cy, size, strokeSpeedMs)
                                        "<" -> accService.drawLessThan(cx, cy, size, strokeSpeedMs)
                                        "=" -> accService.drawEqual(cx, cy, size, size * 0.4f, strokeSpeedMs)
                                    }
                                }

                                mainHandler.post {
                                    tvSolvedBadge.text = " [$solvedCount 题]"
                                    tvStatus.text = "答: $text -> $answer"
                                }
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(15) // 极速轮询间隔
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.apply { start() }
    }

    override fun onDestroy() {
        isRunning.set(false)
        loopThread?.interrupt()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
        captureManager?.release()
        super.onDestroy()
    }
}
