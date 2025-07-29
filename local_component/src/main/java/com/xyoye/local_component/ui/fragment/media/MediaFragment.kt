package com.xyoye.local_component.ui.fragment.media

import android.view.KeyEvent
import androidx.core.view.isVisible
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.xyoye.common_component.adapter.addItem
import com.xyoye.common_component.adapter.buildAdapter
import com.xyoye.common_component.application.DanDanPlay
import com.xyoye.common_component.base.BaseFragment
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.extension.deletable
import com.xyoye.common_component.extension.setData
import com.xyoye.common_component.extension.vertical
import com.xyoye.common_component.services.ScreencastProvideService
import com.xyoye.common_component.utils.tv.TvFocusManager
import com.xyoye.common_component.utils.tv.TvKeyEventHelper
import com.xyoye.common_component.weight.BottomActionDialog
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.common_component.weight.dialog.CommonDialog
import com.xyoye.data_component.bean.SheetActionBean
import com.xyoye.data_component.entity.MediaLibraryEntity
import com.xyoye.data_component.enums.MediaType
import com.xyoye.local_component.BR
import com.xyoye.local_component.R
import com.xyoye.local_component.databinding.FragmentMediaBinding
import com.xyoye.local_component.databinding.ItemMediaLibraryBinding

/**
 * Created by xyoye on 2020/7/27.
 */

@Route(path = RouteTable.Local.MediaFragment)
class MediaFragment : BaseFragment<MediaViewModel, FragmentMediaBinding>() {

    @Autowired
    lateinit var provideService: ScreencastProvideService

    override fun initViewModel() = ViewModelInit(
        BR.viewModel,
        MediaViewModel::class.java
    )

    override fun getLayoutId() = R.layout.fragment_media

    override fun initView() {
        ARouter.getInstance().inject(this)

        viewModel.initLocalStorage()

        initRv()

        dataBinding.addMediaStorageBt.setOnClickListener {
            showAddStorageDialog()
        }

        // 设置加号按钮的焦点导航
        dataBinding.addMediaStorageBt.setOnKeyListener { _, keyCode, event ->
            if (event?.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> {
                    // 从加号按钮回到列表
                    val mediaLibList = viewModel.mediaLibWithStatusLiveData.value ?: emptyList()
                    if (mediaLibList.isNotEmpty()) {
                        dataBinding.mediaLibRv.requestFocus()
                        // 将焦点设置到列表的最后一项
                        val lastPosition = mediaLibList.size - 1
                        dataBinding.mediaLibRv.post {
                            TvFocusManager.restoreFocusState(
                                "${this::class.java.simpleName}_${dataBinding.mediaLibRv.id}",
                                dataBinding.mediaLibRv,
                                lastPosition
                            )
                        }
                        return@setOnKeyListener true
                    }
                    false
                }
                else -> false
            }
        }

        viewModel.mediaLibWithStatusLiveData.observe(this) {
            dataBinding.mediaLibRv.setData(it)
            
            // 当列表为空时，让加号按钮获得焦点
            if (it.isEmpty()) {
                dataBinding.addMediaStorageBt.post {
                    dataBinding.addMediaStorageBt.requestFocus()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 恢复TV端焦点状态
        val focusKey = "${this::class.java.simpleName}_${dataBinding.mediaLibRv.id}"
        dataBinding.mediaLibRv.post {
            TvFocusManager.restoreFocusState(focusKey, dataBinding.mediaLibRv)
        }
    }

    override fun onPause() {
        super.onPause()
        // 保存TV端焦点状态
        val focusKey = "${this::class.java.simpleName}_${dataBinding.mediaLibRv.id}"
        TvFocusManager.saveFocusState(focusKey, dataBinding.mediaLibRv)
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
            val mediaLibList = viewModel.mediaLibWithStatusLiveData.value ?: emptyList()
            
            // 处理向下键，当到达最后一项时移动到加号按钮
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                val layoutManager = dataBinding.mediaLibRv.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
                val itemCount = mediaLibList.size
                
                if (lastVisiblePosition == itemCount - 1 || itemCount == 0) {
                    // 已经到达最后一项或列表为空，移动到加号按钮
                    dataBinding.addMediaStorageBt.requestFocus()
                    return true
                }
            }
            
            return TvKeyEventHelper.handleRecyclerViewKeyEvent(
                dataBinding.mediaLibRv,
                keyCode,
                mediaLibList
            )
        }

        return false
    }

    private fun initRv() {
        dataBinding.mediaLibRv.apply {
            layoutManager = vertical()

            adapter = buildAdapter {
                addItem<MediaLibraryEntity, ItemMediaLibraryBinding>(R.layout.item_media_library) {
                    initView { data, _, _ ->
                        itemBinding.apply {
                            libraryNameTv.text = data.displayName
                            libraryUrlTv.text = data.disPlayDescribe
                            libraryCoverIv.setImageResource(data.mediaType.cover)

                            screencastStatusTv.isVisible =
                                data.mediaType == MediaType.SCREEN_CAST && data.running
                            screencastStatusTv.setOnClickListener {
                                showStopServiceDialog()
                            }

                            itemLayout.setOnClickListener {
                                DanDanPlay.permission.storage.request(this@MediaFragment) {
                                    onGranted {
                                        launchMediaStorage(data)
                                    }
                                    onDenied {
                                        ToastCenter.showError("获取文件读取权限失败，无法打开媒体库")
                                    }
                                }
                            }
                            itemLayout.setOnLongClickListener {
                                if (data.mediaType.deletable) {
                                    showManageStorageDialog(data)
                                }
                                true
                            }
                        }
                    }
                }
            }

            // 设置TV端焦点管理
            val focusKey = "${this@MediaFragment::class.java.simpleName}_${this.id}"
            TvFocusManager.setupSmartFocusRestore(focusKey, this)
        }
    }

    private fun launchMediaStorage(data: MediaLibraryEntity) {
        when (data.mediaType) {
            MediaType.STREAM_LINK, MediaType.MAGNET_LINK, MediaType.OTHER_STORAGE -> {
                ARouter.getInstance()
                    .build(RouteTable.Local.PlayHistory)
                    .withSerializable("typeValue", data.mediaType.value)
                    .navigation()
            }

            MediaType.SCREEN_CAST -> {
                viewModel.checkScreenDeviceRunning(data)
            }

            MediaType.LOCAL_STORAGE,
            MediaType.FTP_SERVER,
            MediaType.SMB_SERVER,
            MediaType.WEBDAV_SERVER,
            MediaType.REMOTE_STORAGE,
            MediaType.EXTERNAL_STORAGE,
            MediaType.ALSIT_STORAGE -> {
                ARouter.getInstance()
                    .build(RouteTable.Stream.StorageFile)
                    .withParcelable("storageLibrary", data)
                    .navigation()
            }
        }
    }

    private fun showAddStorageDialog() {
        val actionList = MediaType.values()
            .filter { it.deletable }
            .map { it.toAction() }

        BottomActionDialog(
            requireActivity(),
            actionList,
            "新增网络媒体库"
        ) {
            val mediaType = it.actionId as MediaType
            ARouter.getInstance()
                .build(RouteTable.Stream.StoragePlus)
                .withSerializable("mediaType", mediaType)
                .navigation()
            return@BottomActionDialog true
        }.show()
    }

    private fun showManageStorageDialog(data: MediaLibraryEntity) {
        val actions = mutableListOf<SheetActionBean>()
        actions.add(ManageStorage.Edit.toAction())
        actions.add(ManageStorage.Delete.toAction())

        BottomActionDialog(requireActivity(), actions) {
            if (it.actionId == ManageStorage.Edit) {
                ARouter.getInstance()
                    .build(RouteTable.Stream.StoragePlus)
                    .withSerializable("mediaType", data.mediaType)
                    .withParcelable("editData", data)
                    .navigation()
            } else if (it.actionId == ManageStorage.Delete) {
                showDeleteStorageDialog(data)
            }
            return@BottomActionDialog true
        }.show()
    }

    private fun showDeleteStorageDialog(data: MediaLibraryEntity) {
        CommonDialog.Builder(requireActivity())
            .apply {
                content = "确认删除以下媒体库?\n\n${data.displayName}"
                positiveText = "确认"
                addPositive { dialog ->
                    dialog.dismiss()
                    viewModel.deleteStorage(data)
                }
                addNegative()
            }.build().show()
    }

    private fun showStopServiceDialog() {
        CommonDialog.Builder(requireActivity())
            .apply {
                content = "确认停止投屏投送服务？"
                positiveText = "确认"
                addPositive { dialog ->
                    dialog.dismiss()
                    provideService.stopService(requireActivity())
                }
                addNegative()
            }.build().show()
    }

    private enum class ManageStorage(val title: String, val icon: Int) {
        Edit("编辑媒体库", R.drawable.ic_edit_storage),
        Delete("删除媒体库", R.drawable.ic_delete_storage);

        fun toAction() = SheetActionBean(this, title, icon)
    }
}