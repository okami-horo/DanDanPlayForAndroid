package com.xyoye.player.controller.base

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.xyoye.common_component.utils.tv.TvKeyEventHelper
import com.xyoye.player.controller.video.InterGestureView
import com.xyoye.player.controller.tv.SeekPreviewManager

/**
 * Created by xyoye on 2021/5/30.
 */

abstract class TvVideoController(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseVideoController(context, attrs, defStyleAttr) {
    
    private val seekPreviewManager: SeekPreviewManager
    
    init {
        seekPreviewManager = SeekPreviewManager(
            mControlWrapper,
            { isPreviewing: Boolean, position: Long ->
                // 预览状态变化回调
                if (isPreviewing) {
                    showController(true)
                    for (entry in mControlComponents.entries) {
                        entry.key.onSeekPreviewStarted(position)
                    }
                } else {
                    for (entry in mControlComponents.entries) {
                        entry.key.onSeekPreviewFinished(position)
                    }
                }
            },
            { position: Long, adjustmentText: String ->
                // 预览位置变化回调
                for (entry in mControlComponents.entries) {
                    entry.key.onSeekPreviewChanged(position, adjustmentText)
                }
            }
        )
    }



    private fun onActionCenter(): Boolean {
        if (isLocked()) {
            showController(true)
            return true
        }
        if (mControlWrapper.isSettingViewShowing()) {
            return false
        }
        
        if (seekPreviewManager.isPreviewing()) {
            // 预览模式下确认跳转
            seekPreviewManager.exitPreviewMode(true)
            return true
        }
        
        if (isControllerShowing()) {
            showController(true)
            return false
        }
        togglePlay()
        return true
    }

    private fun onActionUp(): Boolean {
        if (mControlWrapper.isSettingViewShowing()) {
            return false
        }
        showController(true)
        return false
    }

    private fun onActionDown(): Boolean {
        if (mControlWrapper.isSettingViewShowing()) {
            return false
        }
        showController(true)
        return false
    }

    private fun onActionLeft(): Boolean {
        if (mControlWrapper.isSettingViewShowing()) {
            return false
        }
        if (isLocked()) {
            showController(true)
            return false
        }
        
        if (seekPreviewManager.isPreviewing()) {
            // 预览模式下调整位置
            seekPreviewManager.adjustPreviewPosition(-30 * 1000L)
            return true
        } else {
            // 进入预览模式
            seekPreviewManager.enterPreviewMode()
            return true
        }
    }

    private fun onActionRight(): Boolean {
        if (mControlWrapper.isSettingViewShowing()) {
            return false
        }
        if (isLocked()) {
            showController(true)
            return false
        }
        
        if (seekPreviewManager.isPreviewing()) {
            // 预览模式下调整位置
            seekPreviewManager.adjustPreviewPosition(30 * 1000L)
            return true
        } else {
            // 进入预览模式
            seekPreviewManager.enterPreviewMode()
            return true
        }
    }

    private fun changePosition(offset: Long) {
        val duration = mControlWrapper.getDuration()
        val currentPosition = mControlWrapper.getCurrentPosition()
        val newPosition = currentPosition + offset

        for (entry in mControlComponents.entries) {
            val view = entry.key
            if (view is InterGestureView) {
                view.onStartSlide()
                view.onPositionChange(newPosition, currentPosition, duration)
                view.onStopSlide()
            }
        }
        mControlWrapper.seekTo(newPosition)
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isPopupMode()) {
            return false
        }
        
        // 处理长按事件
        if (event?.repeatCount == 0 && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            if (seekPreviewManager.isPreviewing()) {
                val direction = if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -1L else 1L
                seekPreviewManager.startLongPressAdjust(direction)
                return true
            }
        }
        
        // 检查是否为TV端按键
        if (!TvKeyEventHelper.isTvDirectionKey(keyCode) && !TvKeyEventHelper.isTvConfirmKey(keyCode)) {
            return mControlWrapper.onKeyDown(keyCode, event)
        }
        
        val intercept = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER -> onActionCenter()
            KeyEvent.KEYCODE_DPAD_UP -> onActionUp()
            KeyEvent.KEYCODE_DPAD_DOWN -> onActionDown()
            KeyEvent.KEYCODE_DPAD_LEFT -> onActionLeft()
            KeyEvent.KEYCODE_DPAD_RIGHT -> onActionRight()
            KeyEvent.KEYCODE_BACK -> {
                if (seekPreviewManager.isPreviewing()) {
                    // 返回键取消预览
                    seekPreviewManager.exitPreviewMode(false)
                    true
                } else {
                    false
                }
            }
            else -> null
        }
        
        return if (intercept == null) {
            false
        } else if (intercept) {
            true
        } else {
            mControlWrapper.onKeyDown(keyCode, event)
        }
    }
    
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            seekPreviewManager.stopLongPressAdjust()
        }
        return super.onKeyUp(keyCode, event)
    }
}