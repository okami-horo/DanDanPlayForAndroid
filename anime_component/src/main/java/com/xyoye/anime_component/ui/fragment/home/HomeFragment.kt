package com.xyoye.anime_component.ui.fragment.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.xyoye.anime_component.BR
import com.xyoye.anime_component.R
import com.xyoye.anime_component.databinding.FragmentHomeBinding
import android.view.KeyEvent
import android.view.View
import com.xyoye.anime_component.ui.adapter.HomeBannerAdapter
import com.xyoye.anime_component.ui.fragment.home_page.HomePageFragment
import com.xyoye.common_component.base.BaseFragment
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.utils.dp2px
import com.xyoye.common_component.utils.tv.TvFocusManager
import com.youth.banner.config.BannerConfig
import com.youth.banner.config.IndicatorConfig
import com.youth.banner.indicator.CircleIndicator
import java.util.*

/**
 * Created by xyoye on 2020/7/28.
 */

@Route(path = RouteTable.Anime.HomeFragment)
class HomeFragment : BaseFragment<HomeFragmentViewModel, FragmentHomeBinding>() {

    override fun initViewModel() = ViewModelInit(
        BR.viewModel,
        HomeFragmentViewModel::class.java
    )

    override fun getLayoutId() = R.layout.fragment_home

    override fun initView() {
        dataBinding.tabLayout.setupWithViewPager(dataBinding.viewpager)

        dataBinding.searchLl.setOnClickListener {
            ARouter.getInstance()
                .build(RouteTable.Anime.Search)
                .navigation()
        }
        dataBinding.seasonLl.setOnClickListener {
            ARouter.getInstance()
                .build(RouteTable.Anime.AnimeSeason)
                .navigation()
        }

        initViewModelObserve()
        viewModel.getBanners()
        viewModel.getWeeklyAnime()
        
        // 设置TV端焦点管理
        setupTvFocusManagement()
    }

    private fun initViewModelObserve() {
        viewModel.bannersLiveData.observe(this) {
            dataBinding.banner.apply {
                setAdapter(HomeBannerAdapter(it.banners))
                indicator = CircleIndicator(mAttachActivity)
                setIndicatorGravity(IndicatorConfig.Direction.RIGHT)
                setIndicatorMargins(
                    IndicatorConfig.Margins(
                        0, 0, BannerConfig.INDICATOR_MARGIN, dp2px(12)
                    )
                )
            }
        }

        viewModel.weeklyAnimeLiveData.observe(this) {
            dataBinding.viewpager.apply {
                adapter = HomePageAdapter(childFragmentManager)
                offscreenPageLimit = 2
                currentItem = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
            }
        }
    }

    override fun onResume() {
        super.onResume()
        restoreTvFocus()
    }

    override fun onPause() {
        super.onPause()
        saveTvFocus()
    }

    fun handleKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // 在HomeFragment中，左右键用于横幅轮播，但主要焦点应该在垂直导航上
                // 让系统处理横幅的左右滑动，但阻止左右键导致焦点跳跃
                return false
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                // 允许上下键正常导航
                return false
            }
        }
        return false
    }

    private fun setupTvFocusManagement() {
        // 设置垂直焦点顺序：banner -> search/season -> tab_layout -> viewpager
        dataBinding.banner.apply {
            nextFocusDownId = dataBinding.searchLl.id
        }
        
        dataBinding.searchLl.apply {
            nextFocusUpId = dataBinding.banner.id
            nextFocusDownId = dataBinding.tabLayout.id
            nextFocusLeftId = dataBinding.searchLl.id
            nextFocusRightId = dataBinding.seasonLl.id
        }
        
        dataBinding.seasonLl.apply {
            nextFocusUpId = dataBinding.banner.id
            nextFocusDownId = dataBinding.tabLayout.id
            nextFocusLeftId = dataBinding.searchLl.id
            nextFocusRightId = dataBinding.seasonLl.id
        }
        
        dataBinding.tabLayout.apply {
            nextFocusUpId = dataBinding.searchLl.id
            nextFocusDownId = dataBinding.viewpager.id
        }
        
        dataBinding.viewpager.apply {
            nextFocusUpId = dataBinding.tabLayout.id
        }
    }

    private fun saveTvFocus() {
        // HomeFragment没有RecyclerView，保存当前焦点的View ID
        val focusedView = view?.findFocus()
        if (focusedView != null) {
            // 保存当前焦点的View ID到SharedPreferences
            requireContext().getSharedPreferences("tv_focus", 0)
                .edit()
                .putInt("home_fragment_focus", focusedView.id)
                .apply()
        }
    }

    private fun restoreTvFocus() {
        val savedFocusId = requireContext().getSharedPreferences("tv_focus", 0)
            .getInt("home_fragment_focus", dataBinding.banner.id)
        
        view?.findViewById<View>(savedFocusId)?.requestFocus() ?: run {
            // 默认焦点到banner
            dataBinding.banner.requestFocus()
        }
    }

    inner class HomePageAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(
        fragmentManager,
        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {
        override fun getItem(position: Int): Fragment {
            return HomePageFragment.newInstance(
                viewModel.weeklyAnimeLiveData.value!![position]
            )
        }

        override fun getCount(): Int {
            return viewModel.weeklyAnimeLiveData.value?.size ?: 0
        }

        override fun getPageTitle(position: Int): CharSequence {
            return if (position < viewModel.tabTitles.size)
                viewModel.tabTitles[position]
            else
                ""
        }
    }
}