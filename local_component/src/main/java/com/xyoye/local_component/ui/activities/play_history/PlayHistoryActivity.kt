package com.xyoye.local_component.ui.activities.play_history

import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.xyoye.common_component.base.BaseActivity
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.extension.setData
import com.xyoye.common_component.extension.vertical
import com.xyoye.common_component.extension.requestIndexChildFocus
import com.xyoye.common_component.utils.tv.TvFocusManager
import com.xyoye.common_component.utils.tv.TvKeyEventHelper
import com.xyoye.data_component.enums.MediaType
import com.xyoye.local_component.BR
import com.xyoye.local_component.R
import com.xyoye.local_component.databinding.ActivityPlayHistoryBinding
import com.xyoye.local_component.ui.dialog.MagnetPlayDialog
import com.xyoye.local_component.ui.dialog.StreamLinkDialog
import com.xyoye.local_component.ui.weight.PlayHistoryMenus

@Route(path = RouteTable.Local.PlayHistory)
class PlayHistoryActivity : BaseActivity<PlayHistoryViewModel, ActivityPlayHistoryBinding>() {

    @Autowired
    @JvmField
    var typeValue: String = MediaType.LOCAL_STORAGE.value

    private lateinit var mediaType: MediaType

    // 标题栏菜单管理器
    private lateinit var mMenus: PlayHistoryMenus

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            PlayHistoryViewModel::class.java
        )

    override fun getLayoutId() = R.layout.activity_play_history

    override fun initView() {
        ARouter.getInstance().inject(this)

        mediaType = MediaType.fromValue(typeValue)
        viewModel.mediaType = mediaType

        title = when (mediaType) {
            MediaType.MAGNET_LINK -> "磁链播放"
            MediaType.STREAM_LINK -> "串流播放"
            else -> "播放历史"
        }
        dataBinding.addLinkBt.isVisible = mediaType == MediaType.MAGNET_LINK || mediaType == MediaType.STREAM_LINK

        initRv()

        initListener()
    }

    override fun onResume() {
        super.onResume()

        viewModel.updatePlayHistory()
        
        // 恢复TV端焦点状态
        val focusKey = "${this::class.java.simpleName}_${dataBinding.playHistoryRv.id}"
        dataBinding.playHistoryRv.post {
            TvFocusManager.restoreFocusState(focusKey, dataBinding.playHistoryRv)
        }
    }

    override fun onPause() {
        super.onPause()
        
        // 保存TV端焦点状态
        val focusKey = "${this::class.java.simpleName}_${dataBinding.playHistoryRv.id}"
        TvFocusManager.saveFocusState(focusKey, dataBinding.playHistoryRv)
    }

    private fun initListener() {
        dataBinding.addLinkBt.setOnClickListener {
            if (mediaType == MediaType.STREAM_LINK) {
                showStreamDialog()
            } else if (mediaType == MediaType.MAGNET_LINK) {
                showMagnetDialog()
            }
        }
        
        // 设置加号按钮的按键监听器，处理从按钮回到列表的焦点导航
        dataBinding.addLinkBt.setOnKeyListener { _, keyCode, event ->
            if (event?.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> {
                    // 从加号按钮回到列表
                    val historyList = viewModel.historyLiveData.value ?: emptyList()
                    if (historyList.isNotEmpty()) {
                        dataBinding.playHistoryRv.requestFocus()
                        // 将焦点设置到列表的最后一项
                        val lastPosition = historyList.size - 1
                        dataBinding.playHistoryRv.post {
                            dataBinding.playHistoryRv.requestIndexChildFocus(lastPosition)
                        }
                        return@setOnKeyListener true
                    }
                    false
                }
                else -> false
            }
        }
        
        viewModel.historyLiveData.observe(this) {
            dataBinding.playHistoryRv.setData(it)
            
            // 当列表为空时，让加号按钮获得焦点
            if (it.isEmpty() && dataBinding.addLinkBt.isVisible) {
                dataBinding.addLinkBt.post {
                    dataBinding.addLinkBt.requestFocus()
                }
            }
        }
        viewModel.playLiveData.observe(this) {
            ARouter.getInstance()
                .build(RouteTable.Player.Player)
                .navigation()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mMenus = PlayHistoryMenus.inflater(this, menu)
        mMenus.onClearHistory { viewModel.clearHistory() }
        mMenus.onSortTypeChanged { viewModel.changeSortOption(it) }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        mMenus.onOptionsItemSelected(item)
        return super.onOptionsItemSelected(item)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return handleKeyEvent(event.keyCode, event) || super.dispatchKeyEvent(event)
    }

    /**
     * 处理TV端按键事件
     */
    fun handleKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action != KeyEvent.ACTION_DOWN) {
            return false
        }

        // 处理RecyclerView的方向键导航
        if (TvKeyEventHelper.isTvDirectionKey(keyCode)) {
            val historyList = viewModel.historyLiveData.value ?: emptyList()
            
            // 处理向下键，当到达最后一项时移动到加号按钮
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                val layoutManager = dataBinding.playHistoryRv.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
                val itemCount = historyList.size
                
                if (lastVisiblePosition == itemCount - 1 || itemCount == 0) {
                    // 已经到达最后一项或列表为空，移动到加号按钮
                    if (dataBinding.addLinkBt.isVisible) {
                        dataBinding.addLinkBt.requestFocus()
                        return true
                    }
                }
            }
            
            // 如果列表为空，让加号按钮获得焦点
            if (historyList.isEmpty() && dataBinding.addLinkBt.isVisible) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    dataBinding.addLinkBt.requestFocus()
                    return true
                }
            }
            
            return TvKeyEventHelper.handleRecyclerViewKeyEvent(
                dataBinding.playHistoryRv,
                keyCode,
                historyList
            )
        }

        return false
    }

    private fun initRv() {
        dataBinding.playHistoryRv.apply {
            layoutManager = vertical()

            adapter = PlayHistoryAdapter(
                this@PlayHistoryActivity,
                viewModel
            ).createAdapter()

            // 设置TV端焦点管理
            val focusKey = "${this@PlayHistoryActivity::class.java.simpleName}_${this.id}"
            TvFocusManager.setupSmartFocusRestore(focusKey, this)
        }
    }

    private fun showStreamDialog() {
        StreamLinkDialog(this) { link, header ->
            viewModel.openStreamLink(link, header)
        }.show()
    }

    private fun showMagnetDialog() {
        MagnetPlayDialog(this).show()
    }
}