package com.xyoye.storage_component.ui.fragment.storage_file

import androidx.core.view.children
import androidx.core.view.isVisible
import com.xyoye.common_component.base.BaseFragment
import com.xyoye.common_component.extension.setData
import com.xyoye.common_component.extension.vertical
import com.xyoye.common_component.utils.tv.TvFocusManager
import com.xyoye.common_component.storage.file.StorageFile
import com.xyoye.storage_component.BR
import com.xyoye.storage_component.R
import com.xyoye.storage_component.databinding.FragmentStorageFileBinding
import com.xyoye.storage_component.ui.activities.storage_file.StorageFileActivity

class StorageFileFragment :
    BaseFragment<StorageFileFragmentViewModel, FragmentStorageFileBinding>() {

    private val directory: StorageFile? by lazy { ownerActivity.directory }

    companion object {

        fun newInstance() = StorageFileFragment()
    }

    private val ownerActivity by lazy {
        requireActivity() as StorageFileActivity
    }

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            StorageFileFragmentViewModel::class.java
        )

    override fun getLayoutId() = R.layout.fragment_storage_file

    override fun initView() {
        initRecyclerView()

        viewModel.storage = ownerActivity.storage

        viewModel.fileLiveData.observe(this) {
            dataBinding.loading.isVisible = false
            dataBinding.refreshLayout.isVisible = true
            dataBinding.refreshLayout.isRefreshing = false
            ownerActivity.onDirectoryOpened(it)
            dataBinding.storageFileRv.setData(it)
            //延迟500毫秒，等待列表加载完成后，再请求焦点
            dataBinding.storageFileRv.postDelayed({ requestFocus() }, 500)
        }

        dataBinding.refreshLayout.setColorSchemeResources(R.color.theme)
        dataBinding.refreshLayout.setOnRefreshListener {
            viewModel.listFile(directory, refresh = true)
        }

        viewModel.listFile(directory)
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateHistory()
        setRecyclerViewItemFocusAble(true)

        // 恢复焦点状态
        val focusKey = "${this::class.java.simpleName}_${dataBinding.storageFileRv.id}"
        dataBinding.storageFileRv.post {
            TvFocusManager.restoreFocusState(focusKey, dataBinding.storageFileRv)
        }
    }

    override fun onPause() {
        super.onPause()
        setRecyclerViewItemFocusAble(false)

        // 保存焦点状态
        val focusKey = "${this::class.java.simpleName}_${dataBinding.storageFileRv.id}"
        TvFocusManager.saveFocusState(focusKey, dataBinding.storageFileRv)
    }

    private fun setRecyclerViewItemFocusAble(focusAble: Boolean) {
        dataBinding.storageFileRv.children.forEach {
            it.isFocusable = focusAble
        }
    }

    private fun initRecyclerView() {
        dataBinding.storageFileRv.apply {
            layoutManager = vertical()

            adapter = StorageFileAdapter(ownerActivity, viewModel).create()

            // 设置智能焦点恢复
            val focusKey = "${this@StorageFileFragment::class.java.simpleName}_${this.id}"
            TvFocusManager.setupSmartFocusRestore(focusKey, this)
        }
    }

    fun requestFocus(reversed: Boolean = false) {
        if (isDestroyed()) {
            return
        }
        val targetIndex = if (reversed) dataBinding.storageFileRv.childCount - 1 else 0
        dataBinding.storageFileRv.getChildAt(targetIndex)?.requestFocus()
    }

    /**
     * 搜索
     */
    fun search(text: String) {
        //存在搜索条件时，不允许下拉刷新
        dataBinding.refreshLayout.isEnabled = text.isEmpty()
        viewModel.searchByText(text)
    }

    /**
     * 修改文件排序
     */
    fun sort() {
        viewModel.changeSortOption()
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
            val fileList = viewModel.storageFileLiveData.value ?: emptyList()
            return TvKeyEventHelper.handleRecyclerViewKeyEvent(
                dataBinding.storageFileRv,
                keyCode,
                fileList
            )
        }

        return false
    }
}