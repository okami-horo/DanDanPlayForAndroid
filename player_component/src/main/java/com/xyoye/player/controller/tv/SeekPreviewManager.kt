package com.xyoye.player.controller.tv

import android.os.Handler
import android.os.Looper
import com.xyoye.player.wrapper.ControlWrapper

/**
 * TV端遥控器快进快退预览管理器
 * 实现预览-确认模式的交互逻辑
 */
class SeekPreviewManager(
    private val controlWrapper: ControlWrapper,
    private val onPreviewStateChanged: (isPreviewing: Boolean, previewPosition: Long) -> Unit,
    private val onPreviewPositionChanged: (previewPosition: Long, adjustmentText: String) -> Unit
) {
    companion object {
        private const val CLICK_ADJUSTMENT = 30 * 1000L // 30秒
        private const val LONG_PRESS_DELAY = 1000L // 长按检测延迟
        private const val AUTO_ADJUST_INTERVAL = 200L // 自动调整间隔
        
        // 调整幅度序列（毫秒）
        private val ADJUSTMENT_STEPS = longArrayOf(
            30 * 1000L,    // 30秒
            60 * 1000L,    // 1分钟
            3 * 60 * 1000L, // 3分钟
            5 * 60 * 1000L, // 5分钟
            10 * 60 * 1000L  // 10分钟
        )
    }

    enum class PreviewState {
        IDLE,
        PREVIEW,
        ADJUSTING
    }

    private var state: PreviewState = PreviewState.IDLE
    private var originalPosition: Long = 0
    private var previewPosition: Long = 0
    private var currentStepIndex: Int = 0
    private var isLongPressing: Boolean = false
    
    private val handler = Handler(Looper.getMainLooper())
    private var autoAdjustRunnable: Runnable? = null

    /**
     * 进入预览模式
     */
    fun enterPreviewMode() {
        if (state == PreviewState.IDLE) {
            state = PreviewState.PREVIEW
            originalPosition = controlWrapper.getCurrentPosition()
            previewPosition = originalPosition
            currentStepIndex = 0
            
            onPreviewStateChanged(true, previewPosition)
        }
    }

    /**
     * 退出预览模式
     */
    fun exitPreviewMode(confirm: Boolean) {
        if (state != PreviewState.IDLE) {
            stopAutoAdjust()
            state = PreviewState.IDLE
            
            if (confirm && previewPosition != originalPosition) {
                controlWrapper.seekTo(previewPosition)
            }
            
            onPreviewStateChanged(false, if (confirm) previewPosition else originalPosition)
        }
    }

    /**
     * 调整预览位置（单击）
     */
    fun adjustPreviewPosition(offset: Long) {
        if (state == PreviewState.IDLE) {
            enterPreviewMode()
        }
        
        val duration = controlWrapper.getDuration()
        previewPosition = (previewPosition + offset).coerceIn(0, duration)
        currentStepIndex = 0
        
        val adjustmentText = formatAdjustmentText(offset)
        onPreviewPositionChanged(previewPosition, adjustmentText)
    }

    /**
     * 开始长按调整
     */
    fun startLongPressAdjust(direction: Long) {
        if (state != PreviewState.IDLE) {
            isLongPressing = true
            currentStepIndex = 0
            startAutoAdjust(direction)
        }
    }

    /**
     * 停止长按调整
     */
    fun stopLongPressAdjust() {
        isLongPressing = false
        stopAutoAdjust()
    }

    /**
     * 开始自动调整
     */
    private fun startAutoAdjust(direction: Long) {
        stopAutoAdjust()
        
        autoAdjustRunnable = object : Runnable {
            override fun run() {
                if (isLongPressing && state != PreviewState.IDLE) {
                    val step = ADJUSTMENT_STEPS[currentStepIndex.coerceIn(0, ADJUSTMENT_STEPS.size - 1)]
                    val duration = controlWrapper.getDuration()
                    
                    previewPosition = (previewPosition + direction * step).coerceIn(0, duration)
                    
                    // 递增调整幅度
                    if (currentStepIndex < ADJUSTMENT_STEPS.size - 1) {
                        currentStepIndex++
                    }
                    
                    val adjustmentText = formatAdjustmentText(direction * step)
                    onPreviewPositionChanged(previewPosition, adjustmentText)
                    
                    handler.postDelayed(this, AUTO_ADJUST_INTERVAL)
                }
            }
        }
        
        handler.postDelayed(autoAdjustRunnable!!, LONG_PRESS_DELAY)
    }

    /**
     * 停止自动调整
     */
    private fun stopAutoAdjust() {
        autoAdjustRunnable?.let { handler.removeCallbacks(it) }
        autoAdjustRunnable = null
    }

    /**
     * 格式化调整文本
     */
    private fun formatAdjustmentText(offset: Long): String {
        val absOffset = kotlin.math.abs(offset)
        val direction = if (offset > 0) "快进" else "快退"
        
        val timeText = when {
            absOffset < 60 * 1000 -> "${absOffset / 1000}秒"
            absOffset < 60 * 60 * 1000 -> "${absOffset / (60 * 1000)}分钟"
            else -> "${absOffset / (60 * 60 * 1000)}小时${(absOffset % (60 * 60 * 1000)) / (60 * 1000)}分钟"
        }
        
        return "$direction $timeText"
    }

    /**
     * 获取当前预览状态
     */
    fun isPreviewing(): Boolean = state != PreviewState.IDLE

    /**
     * 获取当前预览位置
     */
    fun getPreviewPosition(): Long = previewPosition

    /**
     * 获取原始位置
     */
    fun getOriginalPosition(): Long = originalPosition
}

/**
 * 时间格式化工具类
 */
object TimeFormatter {
    fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}