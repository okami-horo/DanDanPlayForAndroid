package com.xyoye.player.controller.setting

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.xyoye.common_component.adapter.addItem
import com.xyoye.common_component.adapter.buildAdapter
import com.xyoye.common_component.extension.nextItemIndexSafe
import com.xyoye.common_component.extension.previousItemIndexSafe
import com.xyoye.common_component.extension.requestIndexChildFocus
import com.xyoye.common_component.extension.requestIndexChildFocusSafe
import com.xyoye.common_component.utils.tv.TvKeyEventHelper
import com.xyoye.common_component.extension.setData
import com.xyoye.common_component.extension.vertical
import com.xyoye.common_component.utils.dp2px
import com.xyoye.common_component.utils.view.ItemDecorationOrientation
import com.xyoye.data_component.bean.VideoTrackBean
import com.xyoye.data_component.enums.SettingViewType
import com.xyoye.data_component.enums.TrackType
import com.xyoye.player_component.R
import com.xyoye.player_component.databinding.ItemSettingTracksBinding
import com.xyoye.player_component.databinding.LayoutSettingTracksBinding

/**
 * Created by xyoye on 2024/1/26
 */

class SettingTracksView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseSettingView<LayoutSettingTracksBinding>(
    context, attrs, defStyleAttr
) {

    private var mTrackType = TrackType.DANMU
    private val tracks = mutableListOf<VideoTrackBean>()

    private val title
        get() = when (mTrackType) {
            TrackType.AUDIO -> "音轨"
            TrackType.DANMU -> "弹幕轨"
            TrackType.SUBTITLE -> "字幕轨"
        }

    private val actionText
        get() = when (mTrackType) {
            TrackType.AUDIO -> "添加音轨"
            TrackType.DANMU -> "添加弹幕轨"
            TrackType.SUBTITLE -> "添加字幕轨"
        }

    init {
        initView()

        initListener()
    }

    private val decoration
        get() = ItemDecorationOrientation(
            dividerPx = dp2px(10),
            headerFooterPx = 0,
            orientation = RecyclerView.VERTICAL
        )

    override fun getLayoutId(): Int {
        return R.layout.layout_setting_tracks
    }

    override fun getSettingViewType(): SettingViewType {
        return SettingViewType.TRACKS
    }

    override fun onViewShow() {
        viewBinding.tvTitle.text = title
        viewBinding.tvAddTrack.text = actionText

        refreshTracks()
    }

    override fun onViewHide() {
        viewBinding.rvTrack.focusedChild?.clearFocus()
        viewBinding.rvTrack.clearFocus()
    }

    override fun onTrackChanged(type: TrackType) {
        if (isSettingShowing() && type == mTrackType) {
            postDelayed({ refreshTracks() }, 500)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isSettingShowing().not()) {
            return false
        }

        val handled = handleKeyCode(keyCode)
        if (handled) {
            return true
        }

        if (tracks.size > 0) {
            viewBinding.rvTrack.requestIndexChildFocus(0)
        }
        return true
    }

    private fun initView() {
        viewBinding.rvTrack.apply {
            itemAnimator = null

            layoutManager = vertical()

            adapter = buildAdapter {
                addItem<VideoTrackBean, ItemSettingTracksBinding>(R.layout.item_setting_tracks) {
                    initView { data, _, _ ->
                        itemBinding.tvName.text = data.name
                        itemBinding.tvName.isSelected = data.selected
                        itemBinding.tvName.setOnClickListener {
                            onClickTrack(data)
                        }
                    }
                }
            }

            addItemDecoration(decoration)
        }
    }

    private fun initListener() {
        viewBinding.tvAddTrack.setOnClickListener {
            mControlWrapper.showSettingView(SettingViewType.SWITCH_SOURCE, mTrackType)
            onSettingVisibilityChanged(false)
        }
    }

    private fun onClickTrack(track: VideoTrackBean) {
        if (track.selected) {
            return
        }

        if (track.disable) {
            mControlWrapper.deselectTrack(mTrackType)
        } else {
            mControlWrapper.selectTrack(track)
        }
    }

    /**
     * 处理KeyCode事件
     */
    private fun handleKeyCode(keyCode: Int): Boolean {
        // 使用新的TV按键事件处理器
        return TvKeyEventHelper.handleRecyclerViewKeyEvent(
            viewBinding.rvTrack,
            keyCode,
            tracks
        )
    }

    /**
     * 根据KeyCode与当前焦点位置，取得目标焦点位置
     * @deprecated 使用TvKeyEventHelper.handleRecyclerViewKeyEvent替代
     */
    private fun getTargetIndexByKeyCode(keyCode: Int, focusedIndex: Int): Int {
        return when (keyCode) {
            //左、上规则
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> {
                tracks.previousItemIndexSafe<VideoTrackBean>(focusedIndex)
            }
            //右、下规则
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> {
                tracks.nextItemIndexSafe<VideoTrackBean>(focusedIndex)
            }

            else -> {
                -1
            }
        }
    }

    /**
     * 刷新轨道列表
     */
    private fun refreshTracks() {
        val realTracks = mControlWrapper.getTracks(mTrackType)
        // 轨道列表不为空，但所有轨道都未选中，则视为禁用轨道
        val disabled = realTracks.isNotEmpty() && realTracks.all { it.selected.not() }

        tracks.clear()
        // 当轨道列表不为空时，添加禁用轨道
        if (realTracks.isNotEmpty()) {
            tracks.add(VideoTrackBean.disable(mTrackType, disabled))
        }
        tracks.addAll(mControlWrapper.getTracks(mTrackType))
        viewBinding.rvTrack.setData(tracks)

        viewBinding.tvEmptyTrack.isVisible = tracks.isEmpty()
    }

    fun setTrackType(trackType: TrackType) {
        this.mTrackType = trackType
    }
}