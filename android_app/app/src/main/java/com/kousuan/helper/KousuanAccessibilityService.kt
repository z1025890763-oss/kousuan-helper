package com.kousuan.helper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class KousuanAccessibilityService : AccessibilityService() {

    companion object {
        var instance: KousuanAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 无需监听额外系统事件以减少开销
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * 极速绘制大于号 '>' (耗时约 20ms)
     */
    fun drawGreaterThan(cx: Float, cy: Float, size: Float, strokeDurationMs: Long = 20) {
        val path = Path().apply {
            moveTo(cx - size, cy - size)
            lineTo(cx + size, cy)
            lineTo(cx - size, cy + size)
        }
        dispatchStroke(path, strokeDurationMs)
    }

    /**
     * 极速绘制小于号 '<' (耗时约 20ms)
     */
    fun drawLessThan(cx: Float, cy: Float, size: Float, strokeDurationMs: Long = 20) {
        val path = Path().apply {
            moveTo(cx + size, cy - size)
            lineTo(cx - size, cy)
            lineTo(cx + size, cy + size)
        }
        dispatchStroke(path, strokeDurationMs)
    }

    /**
     * 极速绘制等于号 '=' (双横线，耗时约 15ms)
     */
    fun drawEqual(cx: Float, cy: Float, width: Float, height: Float, strokeDurationMs: Long = 15) {
        val path1 = Path().apply {
            moveTo(cx - width, cy - height)
            lineTo(cx + width, cy - height)
        }
        val path2 = Path().apply {
            moveTo(cx - width, cy + height)
            lineTo(cx + width, cy + height)
        }

        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path1, 0, strokeDurationMs))
        builder.addStroke(GestureDescription.StrokeDescription(path2, strokeDurationMs + 5, strokeDurationMs))
        dispatchGesture(builder.build(), null, null)
    }

    private fun dispatchStroke(path: Path, durationMs: Long) {
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }
}
