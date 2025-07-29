package com.xyoye.player.controller.video

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.xyoye.common_component.config.UserConfig
import com.xyoye.common_component.extension.toResColor
import com.xyoye.common_component.extension.toResDrawable
import com.xyoye.common_component.extension.toResString
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.bean.SendDanmuBean
import com.xyoye.data_component.enums.PlayState
import com.xyoye.data_component.enums.SettingViewType
import com.xyoye.player.utils.formatDuration
import com.xyoye.player.wrapper.ControlWrapper
import com.xyoye.player_component.R
import com.xyoye.player_component.databinding.LayoutPlayerBottomBinding

/**
 * Created by xyoye on 2020/11/3.
 */

class PlayerBottomView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), InterControllerView, OnSeekBarChangeListener {

    private var mIsDragging = false
    private lateinit var mControlWrapper: ControlWrapper

    private var sendDanmuBlock: ((SendDanmuBean) -> Unit)? = null

    private var switchVideoSourceBlock: ((Int) -> Unit)? = null

    private val viewBinding = DataBindingUtil.inflate<LayoutPlayerBottomBinding>(
        LayoutInflater.from(context),
        R.layout.layout_player_bottom,
        this,
        true
    )

    init {

        viewBinding.playIv.setOnClickListener {
            mControlWrapper.togglePlay()
        }

        viewBinding.danmuControlIv.setOnClickListener {
            mControlWrapper.toggleDanmuVisible()
            viewBinding.danmuControlIv.isSelected = !viewBinding.danmuControlIv.isSelected
        }

        viewBinding.sendDanmuTv.setOnClickListener {
            if (!UserConfig.isUserLoggedIn()) {
                ToastCenter.showWarning(R.string.tips_login_required.toResString())
                return@setOnClickListener
            }

            if (!mControlWrapper.allowSendDanmu()) {
                ToastCenter.showOriginalToast("当前弹幕不支持发送弹幕")
                return@setOnClickListener
            }

            mControlWrapper.hideController()
            mControlWrapper.pause()
            SendDanmuDialog(mControlWrapper.getCurrentPosition(), context) {
                //添加弹幕到view
                mControlWrapper.addDanmuToView(it)

                //添加弹幕到文件和服务器
                sendDanmuBlock?.invoke(it)
            }.show()
        }

        viewBinding.ivNextSource.setOnClickListener {
            val videoSource = mControlWrapper.getVideoSource()
            if (videoSource.hasNextSource()) {
                switchVideoSourceBlock?.invoke(videoSource.getGroupIndex() + 1)
            }
        }

        viewBinding.ivPreviousSource.setOnClickListener {
            val videoSource = mControlWrapper.getVideoSource()
            if (videoSource.hasPreviousSource()) {
                switchVideoSourceBlock?.invoke(videoSource.getGroupIndex() - 1)
            }
        }

        viewBinding.videoListIv.setOnClickListener {
            mControlWrapper.showSettingView(SettingViewType.SWITCH_VIDEO_SOURCE)
        }

        viewBinding.playSeekBar.setOnSeekBarChangeListener(this)

    }

    override fun attach(controlWrapper: ControlWrapper) {
        mControlWrapper = controlWrapper
    }

    override fun getView() = this

    override fun onVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            ViewCompat.animate(viewBinding.playerBottomLl).translationY(0f).setDuration(300).start()
        } else {
            val height = viewBinding.playerBottomLl.height.toFloat()
            ViewCompat.animate(viewBinding.playerBottomLl).translationY(height)
                .setDuration(300)
                .start()
        }
    }

    override fun onPlayStateChanged(playState: PlayState) {
        when (playState) {
            PlayState.STATE_IDLE -> {
                viewBinding.playSeekBar.progress = 0
                viewBinding.playSeekBar.secondaryProgress = 0
            }

            PlayState.STATE_PREPARING -> {
                updateSourceAction()
                viewBinding.playIv.isSelected = false
            }

            PlayState.STATE_START_ABORT,
            PlayState.STATE_PREPARED,
            PlayState.STATE_PAUSED,
            PlayState.STATE_ERROR -> {
                viewBinding.playIv.isSelected = false
                mControlWrapper.stopProgress()
            }

            PlayState.STATE_PLAYING -> {
                viewBinding.playIv.isSelected = true
                mControlWrapper.startProgress()
            }

            PlayState.STATE_BUFFERING_PAUSED,
            PlayState.STATE_BUFFERING_PLAYING -> {
                viewBinding.playIv.isSelected = mControlWrapper.isPlaying()
            }

            PlayState.STATE_COMPLETED -> {
                mControlWrapper.stopProgress()
                viewBinding.playIv.isSelected = mControlWrapper.isPlaying()
            }
        }
    }

    override fun onProgressChanged(duration: Long, position: Long) {
        if (mIsDragging)
            return

        if (duration > 0) {
            viewBinding.playSeekBar.isEnabled = true
            viewBinding.playSeekBar.progress =
                (position.toFloat() / duration * viewBinding.playSeekBar.max).toInt()
        } else {
            viewBinding.playSeekBar.isEnabled = false
        }

        var bufferedPercent = mControlWrapper.getBufferedPercentage()
        if (bufferedPercent > 95)
            bufferedPercent = 100
        viewBinding.playSeekBar.secondaryProgress = bufferedPercent

        viewBinding.durationTv.text = formatDuration(duration)
        viewBinding.currentPositionTv.text =
            formatDuration(position)
    }

    override fun onLockStateChanged(isLocked: Boolean) {
        //显示状态与锁定状态相反
        onVisibilityChanged(!isLocked)
    }

    override fun onVideoSizeChanged(videoSize: Point) {

    }

    override fun onPopupModeChanged(isPopup: Boolean) {

    }

    override fun onSeekPreviewStarted(position: Long) {
        // 显示进度条预览状态
        viewBinding.playSeekBar.isEnabled = false
        viewBinding.playSeekBar.requestFocus()
        
        // 高亮显示进度条
        val highlightColor = R.color.text_theme.toResColor()
        viewBinding.playSeekBar.progressDrawable.colorFilter = 
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                highlightColor, BlendModeCompat.SRC_IN
            )
        
        // 更新预览位置显示
        val duration = mControlWrapper.getDuration()
        if (duration > 0) {
            val progress = (position.toFloat() / duration * viewBinding.playSeekBar.max).toInt()
            viewBinding.playSeekBar.progress = progress
            viewBinding.currentPositionTv.text = formatDuration(position)
        }
    }

    override fun onSeekPreviewChanged(position: Long, adjustmentText: String) {
        // 更新预览进度条位置
        val duration = mControlWrapper.getDuration()
        if (duration > 0) {
            val progress = (position.toFloat() / duration * viewBinding.playSeekBar.max).toInt()
            viewBinding.playSeekBar.progress = progress
            viewBinding.currentPositionTv.text = formatDuration(position)
        }
        
        // 显示调整提示
        ToastCenter.showOriginalToast(adjustmentText)
    }

    override fun onSeekPreviewFinished(finalPosition: Long) {
        // 恢复进度条正常状态
        viewBinding.playSeekBar.isEnabled = true
        
        // 恢复进度条颜色
        val normalColor = R.color.text_gray.toResColor()
        viewBinding.playSeekBar.progressDrawable.colorFilter = 
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                normalColor, BlendModeCompat.SRC_IN
            )
        
        if (finalPosition >= 0) {
            // 确认跳转
            val duration = mControlWrapper.getDuration()
            if (duration > 0) {
                val progress = (finalPosition.toFloat() / duration * viewBinding.playSeekBar.max).toInt()
                viewBinding.playSeekBar.progress = progress
                viewBinding.currentPositionTv.text = formatDuration(finalPosition)
            }
        } else {
            // 取消预览，恢复当前位置
            val currentPosition = mControlWrapper.getCurrentPosition()
            val duration = mControlWrapper.getDuration()
            if (duration > 0) {
                val progress = (currentPosition.toFloat() / duration * viewBinding.playSeekBar.max).toInt()
                viewBinding.playSeekBar.progress = progress
                viewBinding.currentPositionTv.text = formatDuration(currentPosition)
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser)
            return
        val duration = mControlWrapper.getDuration()
        val newPosition = (duration * progress) / viewBinding.playSeekBar.max
        viewBinding.currentPositionTv.text =
            formatDuration(newPosition)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        mIsDragging = true
        mControlWrapper.stopProgress()
        mControlWrapper.stopFadeOut()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        mIsDragging = false
        val duration = mControlWrapper.getDuration()
        val newPosition =
            (duration * viewBinding.playSeekBar.progress) / viewBinding.playSeekBar.max
        mControlWrapper.seekTo(newPosition)
        mControlWrapper.startFadeOut()
    }

    fun setSendDanmuBlock(block: (SendDanmuBean) -> Unit) {
        sendDanmuBlock = block
    }

    fun setSwitchVideoSourceBlock(block: (Int) -> Unit) {
        switchVideoSourceBlock = block
    }

    private fun updateSourceAction() {
        val videoSource = mControlWrapper.getVideoSource()
        viewBinding.ivNextSource.isVisible = videoSource.hasNextSource()
        viewBinding.ivPreviousSource.isVisible = videoSource.hasPreviousSource()
        viewBinding.videoListIv.isVisible = videoSource.getGroupSize() > 1

        //下一个视频资源是否可用
        val hasNextSource = videoSource.hasNextSource()
        viewBinding.ivNextSource.isEnabled = hasNextSource
        val nextIcon = R.drawable.ic_video_next.toResDrawable()
        if (hasNextSource.not() && nextIcon != null) {
            nextIcon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                R.color.gray_60.toResColor(), BlendModeCompat.SRC_IN
            )
        }
        viewBinding.ivNextSource.setImageDrawable(nextIcon)

        //上一个视频资源是否可用
        val hasPreviousSource = videoSource.hasPreviousSource()
        viewBinding.ivPreviousSource.isEnabled = hasPreviousSource
        val previousIcon = R.drawable.ic_video_previous.toResDrawable()
        if (hasPreviousSource.not() && previousIcon != null) {
            previousIcon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                R.color.gray_60.toResColor(), BlendModeCompat.SRC_IN
            )
        }
        viewBinding.ivPreviousSource.setImageDrawable(previousIcon)
    }
}