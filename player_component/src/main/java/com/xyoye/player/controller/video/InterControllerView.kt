package com.xyoye.player.controller.video

import android.graphics.Point
import android.view.View
import com.xyoye.data_component.enums.PlayState
import com.xyoye.data_component.enums.TrackType
import com.xyoye.player.wrapper.ControlWrapper

/**
 * Created by xyoye on 2020/11/1.
 */

interface InterControllerView {

    fun attach(controlWrapper: ControlWrapper)

    fun getView(): View

    fun onVisibilityChanged(isVisible: Boolean) {}

    fun onPlayStateChanged(playState: PlayState) {}

    fun onProgressChanged(duration: Long, position: Long) {}

    fun onLockStateChanged(isLocked: Boolean) {}

    fun onVideoSizeChanged(videoSize: Point) {}

    fun onPopupModeChanged(isPopup: Boolean) {}

    fun onTrackChanged(type: TrackType) {}

    /**
     * 当遥控器快进快退预览开始时调用
     * @param position 当前预览位置
     */
    fun onSeekPreviewStarted(position: Long) {}

    /**
     * 当遥控器快进快退预览位置变化时调用
     * @param position 新的预览位置
     * @param adjustmentText 调整提示文本（如"+30秒"）
     */
    fun onSeekPreviewChanged(position: Long, adjustmentText: String) {}

    /**
     * 当遥控器快进快退预览结束时调用
     * @param finalPosition 最终确认的位置，如果取消则为-1
     */
    fun onSeekPreviewFinished(finalPosition: Long) {}
}