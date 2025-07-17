package com.xyoye.anime_component.ui.fragment.search_anime

import android.view.KeyEvent
import androidx.core.view.isVisible
import com.xyoye.anime_component.BR
import com.xyoye.anime_component.R
import com.xyoye.anime_component.databinding.FragmentSearchAnimeBinding
import com.xyoye.anime_component.databinding.ItemCommonScreenBinding
import com.xyoye.anime_component.listener.SearchListener
import com.xyoye.anime_component.ui.activities.search.SearchActivity
import com.xyoye.anime_component.ui.adapter.AnimeAdapter
import com.xyoye.common_component.adapter.BaseAdapter
import com.xyoye.common_component.adapter.addItem
import com.xyoye.common_component.adapter.buildAdapter
import com.xyoye.common_component.base.BaseFragment
import com.xyoye.common_component.extension.grid
import com.xyoye.common_component.extension.gridEmpty
import com.xyoye.common_component.extension.setData
import com.xyoye.common_component.extension.setTextColorRes
import com.xyoye.common_component.extension.toResColor
import com.xyoye.common_component.utils.dp2px
import com.xyoye.common_component.utils.tv.TvFocusManager
import com.xyoye.common_component.utils.tv.TvKeyEventHelper
import com.xyoye.common_component.utils.view.ItemDecorationDrawable
import com.xyoye.common_component.utils.view.ItemDecorationSpace
import com.xyoye.data_component.data.CommonTypeData

class SearchAnimeFragment :
    BaseFragment<SearchAnimeFragmentViewModel, FragmentSearchAnimeBinding>(), SearchListener {
    private lateinit var animeAdapter: BaseAdapter

    companion object {
        fun newInstance(): SearchAnimeFragment {
            return SearchAnimeFragment()
        }
    }

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            SearchAnimeFragmentViewModel::class.java
        )

    override fun getLayoutId() = R.layout.fragment_search_anime

    override fun initView() {
        animeAdapter = AnimeAdapter.getAdapter(mAttachActivity)

        initObserver()

        initRv()

        dataBinding.historyLabelsView.setOnLabelClickListener { _, _, position ->
            viewModel.searchHistoryLiveData.value?.let {
                search(it[position])
            }
        }

        dataBinding.historyLabelsView.setOnLabelLongClickListener { _, _, position ->
            viewModel.searchHistoryLiveData.value?.let {
                viewModel.deleteSearchHistory(it[position])
                return@setOnLabelLongClickListener true
            }
            return@setOnLabelLongClickListener false
        }

        dataBinding.historyClearTv.setOnClickListener {
            viewModel.deleteAllSearchHistory()
        }

        viewModel.getAnimeType()
        viewModel.getAnimeSort()
    }

    override fun search(searchText: String) {
        dataBinding.historyCl.isVisible = false

        (mAttachActivity as SearchActivity).onSearch(searchText)

        viewModel.searchText.set(searchText)
        viewModel.search()
    }

    override fun onTextClear() {
        dataBinding.historyCl.isVisible = true
    }

    private fun initRv() {
        dataBinding.animeTypeRv.apply {
            itemAnimator = null

            layoutManager = grid(viewModel.screenSpanCount)

            addItemDecoration(ItemDecorationSpace(dp2px(2)))

            adapter = buildAdapter {
                addItem<CommonTypeData, ItemCommonScreenBinding>(R.layout.item_common_screen) {
                    initView { data, position, _ ->
                        itemBinding.apply {
                            typeNameTv.text = data.typeName
                            typeNameTv.setTextColorRes(
                                if (data.isChecked) R.color.text_screen_checked_color else R.color.text_black
                            )

                            itemLayout.isSelected = data.isChecked

                            itemLayout.setOnClickListener {
                                (mAttachActivity as SearchActivity).hideSearchKeyboard()
                                viewModel.checkType(position)
                            }
                        }
                    }
                }
            }

            // 设置TV端焦点管理
            val focusKey = "${this@SearchAnimeFragment::class.java.simpleName}_animeType_${this.id}"
            TvFocusManager.setupSmartFocusRestore(focusKey, this)
        }

        dataBinding.sortRv.apply {
            itemAnimator = null

            layoutManager = grid(viewModel.screenSpanCount)

            adapter = buildAdapter {

                addItemDecoration(ItemDecorationSpace(dp2px(2), 0))

                addItem<CommonTypeData, ItemCommonScreenBinding>(R.layout.item_common_screen) {
                    initView { data, position, _ ->
                        itemBinding.apply {
                            typeNameTv.text = data.typeName
                            typeNameTv.setTextColorRes(
                                if (data.isChecked) R.color.text_screen_checked_color else R.color.text_black
                            )
                            itemLayout.isSelected = data.isChecked

                            itemLayout.setOnClickListener {
                                (mAttachActivity as SearchActivity).hideSearchKeyboard()
                                viewModel.checkSort(position)
                            }
                        }
                    }
                }
            }

            // 设置TV端焦点管理
            val focusKey = "${this@SearchAnimeFragment::class.java.simpleName}_sort_${this.id}"
            TvFocusManager.setupSmartFocusRestore(focusKey, this)
        }

        dataBinding.animeRv.apply {
            layoutManager = gridEmpty(3)

            adapter = animeAdapter

            val pxValue = dp2px(10)
            addItemDecoration(
                ItemDecorationDrawable(
                    pxValue,
                    pxValue,
                    R.color.item_bg_color.toResColor(mAttachActivity)
                )
            )

            // 设置TV端焦点管理
            val focusKey = "${this@SearchAnimeFragment::class.java.simpleName}_anime_${this.id}"
            TvFocusManager.setupSmartFocusRestore(focusKey, this)
        }
    }

    private fun initObserver() {
        viewModel.animeTypeLiveData.observe(this) {
            dataBinding.animeTypeRv.setData(it)
        }

        viewModel.animeSortLiveData.observe(this) {
            dataBinding.sortRv.setData(it)
        }

        viewModel.animeLiveData.observe(this) {
            //保留recycler view位置，避免滚动
            val recyclerSaveState = dataBinding.animeRv.layoutManager?.onSaveInstanceState()
            dataBinding.animeRv.setData(it)
            dataBinding.animeRv.layoutManager?.onRestoreInstanceState(recyclerSaveState)
        }

        viewModel.searchHistoryLiveData.observe(this) {
            dataBinding.historyLabelsView.setLabels(it)
        }
    }
}