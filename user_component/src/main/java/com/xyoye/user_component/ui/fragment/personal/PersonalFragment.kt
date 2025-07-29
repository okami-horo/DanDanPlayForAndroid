package com.xyoye.user_component.ui.fragment.personal

import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.alibaba.sdk.android.feedback.impl.FeedbackAPI
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import com.xyoye.common_component.base.BaseFragment
import com.xyoye.common_component.bridge.LoginObserver
import com.xyoye.common_component.bridge.ServiceLifecycleBridge
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.config.UserConfig
import com.xyoye.common_component.extension.setTextColorRes
import com.xyoye.common_component.utils.UserInfoHelper
import com.xyoye.data_component.data.LoginData
import com.xyoye.user_component.BR
import com.xyoye.user_component.R
import com.xyoye.user_component.databinding.FragmentPersonalBinding
import com.xyoye.user_component.ui.dialog.UserCoverDialog
import com.xyoye.common_component.utils.tv.TvFocusManager

/**
 * Created by xyoye on 2020/7/28.
 */

@Route(path = RouteTable.User.PersonalFragment)
class PersonalFragment : BaseFragment<PersonalFragmentViewModel, FragmentPersonalBinding>() {

    override fun initViewModel() = ViewModelInit(
        BR.viewModel,
        PersonalFragmentViewModel::class.java
    )

    override fun getLayoutId() = R.layout.fragment_personal

    override fun initView() {
        dataBinding.userCoverIv.setImageResource(getDefaultCoverResId())

        initClick()

        applyLoginData(null)

        viewModel.relationLiveData.observe(this) {
            dataBinding.followAnimeTv.text = it.first.toString()
            dataBinding.followAnimeTv.setTextColorRes(R.color.text_pink)
            dataBinding.cloudHistoryTv.text = it.second.toString()
        }

        UserInfoHelper.loginLiveData.observe(this) {
            applyLoginData(it)
        }

        ServiceLifecycleBridge.getScreencastReceiveObserver().observe(this) {
            dataBinding.screencastStatusTv.isVisible = it
        }

        if (mAttachActivity is LoginObserver) {
            (mAttachActivity as LoginObserver).getLoginLiveData().observe(this) {
                applyLoginData(it)
            }
        }

        // 设置TV端焦点管理
        setupTvFocusManagement()
    }

    private fun applyLoginData(loginData: LoginData?) {
        if (loginData != null) {
            dataBinding.userNameTv.text = loginData.screenName
            dataBinding.tipsLoginBt.isVisible = false
            viewModel.getUserRelationInfo()
        } else {
            dataBinding.userNameTv.text = "登录账号"
            dataBinding.tipsLoginBt.isVisible = true
            dataBinding.followAnimeTv.text = resources.getText(R.string.text_default_count)
            dataBinding.followAnimeTv.setTextColorRes(R.color.text_black)
            dataBinding.cloudHistoryTv.text = resources.getText(R.string.text_default_count)
        }
    }

    private fun getDefaultCoverResId(): Int {
        val coverArray = resources.getIntArray(R.array.cover)
        var coverIndex = UserConfig.getUserCoverIndex()
        if (coverIndex == -1) {
            coverIndex = coverArray.indices.random()
            UserConfig.putUserCoverIndex(coverIndex)
        }
        val typedArray = resources.obtainTypedArray(R.array.cover)
        val coverResId = typedArray.getResourceId(coverIndex, 0)
        typedArray.recycle()
        return coverResId
    }

    private fun initClick() {

        dataBinding.userCoverIv.setOnClickListener {
            UserCoverDialog(requireActivity()) {
                val typedArray = resources.obtainTypedArray(R.array.cover)
                val coverResId = typedArray.getResourceId(it, 0)
                typedArray.recycle()
                UserConfig.putUserCoverIndex(it)
                dataBinding.userCoverIv.setImageResource(coverResId)
            }.show()
        }

        dataBinding.userAccountCl.setOnClickListener {
            if (!checkLoggedIn())
                return@setOnClickListener

            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                mAttachActivity, dataBinding.userCoverIv, dataBinding.userCoverIv.transitionName
            )

            ARouter.getInstance()
                .build(RouteTable.User.UserInfo)
                .withOptionsCompat(options)
                .navigation(mAttachActivity)
        }

        dataBinding.followAnimeLl.setOnClickListener {
            if (!checkLoggedIn())
                return@setOnClickListener

            ARouter.getInstance()
                .build(RouteTable.Anime.AnimeFollow)
                .withParcelable("followData", viewModel.followData)
                .navigation()
        }

        dataBinding.cloudHistoryLl.setOnClickListener {
            if (!checkLoggedIn())
                return@setOnClickListener

            ARouter.getInstance()
                .build(RouteTable.Anime.AnimeHistory)
                .withParcelable("historyData", viewModel.historyData)
                .navigation()
        }

        dataBinding.playerSettingLl.setOnClickListener {
            ARouter.getInstance()
                .build(RouteTable.User.SettingPlayer)
                .navigation()
        }

        dataBinding.scanManagerLl.setOnClickListener {
            ARouter.getInstance()
                .build(RouteTable.User.ScanManager)
                .navigation()
        }

        dataBinding.cacheManagerLl.setOnClickListener {
            ARouter.getInstance()
                .build(RouteTable.User.CacheManager)
                .navigation()
        }

        dataBinding.commonlyManagerLl.setOnClickListener {
            ARouter.getInstance()
                .build(RouteTable.User.CommonManager)
                .navigation()
        }

        dataBinding.bilibiliDanmuLl.setOnClickListener {
            ARouter.getInstance()
                .build(RouteTable.Local.BiliBiliDanmu)
                .navigation()
        }

        dataBinding.shooterSubtitleLl.setOnClickListener {
            ARouter.getInstance()
                .build(RouteTable.Local.ShooterSubtitle)
                .navigation()
        }

        dataBinding.screencastReceiverLl.setOnClickListener {
            ARouter.getInstance()
                .build(RouteTable.Stream.ScreencastReceiver)
                .navigation()
        }

        dataBinding.feedbackLl.setOnClickListener {
            FeedbackAPI.openFeedbackActivity()
        }

        dataBinding.appSettingLl.setOnClickListener {
            ARouter.getInstance()
                .build(RouteTable.User.SettingApp)
                .navigation()
        }
    }

    /**
     * 检查是否已登录
     */
    private fun checkLoggedIn(): Boolean {
        if (!UserConfig.isUserLoggedIn()) {
            ARouter.getInstance()
                .build(RouteTable.User.UserLogin)
                .navigation()
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        // 恢复TV端焦点状态
        restoreTvFocus()
    }

    override fun onPause() {
        super.onPause()
        // 保存TV端焦点状态
        saveTvFocus()
    }

    /**
     * 设置TV端焦点管理
     */
    private fun setupTvFocusManagement() {
        // 为主要的可点击视图设置焦点导航
        val focusableViews = listOf(
            dataBinding.userCoverIv,
            dataBinding.followAnimeLl,
            dataBinding.cloudHistoryLl,
            dataBinding.playerSettingLl,
            dataBinding.scanManagerLl,
            dataBinding.cacheManagerLl,
            dataBinding.commonlyManagerLl,
            dataBinding.bilibiliDanmuLl,
            dataBinding.shooterSubtitleLl,
            dataBinding.screencastReceiverLl,
            dataBinding.feedbackLl,
            dataBinding.appSettingLl
        )

        // 设置焦点顺序 - 垂直方向
        for (i in 0 until focusableViews.size - 1) {
            focusableViews[i].nextFocusDownId = focusableViews[i + 1].id
            focusableViews[i + 1].nextFocusUpId = focusableViews[i].id
        }

        // 设置左右边界焦点（防止焦点跳到左侧菜单栏）
        focusableViews.forEach { view ->
            view.nextFocusLeftId = view.id
            view.nextFocusRightId = view.id
        }

        // 特殊处理横向布局中的焦点关系
        if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // 横向布局下的特殊焦点处理
            dataBinding.followAnimeLl.nextFocusRightId = dataBinding.cloudHistoryLl.id
            dataBinding.cloudHistoryLl.nextFocusLeftId = dataBinding.followAnimeLl.id
            
            // 设置用户头像的下一个焦点
            dataBinding.userCoverIv.nextFocusDownId = dataBinding.followAnimeLl.id
            dataBinding.userCoverIv.nextFocusRightId = dataBinding.followAnimeLl.id
        }

        // 确保所有可点击视图都可聚焦
        focusableViews.forEach { view ->
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            
            // 设置TV端焦点高亮效果
            TvFocusHandler.setupFocusListener(view)
            
            // 设置按键监听器
            view.setOnKeyListener { v, keyCode, event ->
                TvFocusHandler.handleKeyEvent(v, keyCode, event, focusableViews)
            }
        }

        // 处理NestedScrollView的焦点，确保可以滚动
        dataBinding.root.let { rootView ->
            rootView.isFocusable = true
            if (rootView is ViewGroup) {
                rootView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            }
        }
    }

    /**
     * 保存TV端焦点状态
     */
    private fun saveTvFocus() {
        val focusedView = dataBinding.root.findFocus()
        if (focusedView != null) {
            val focusableViews = listOf(
                dataBinding.userCoverIv,
                dataBinding.followAnimeLl,
                dataBinding.cloudHistoryLl,
                dataBinding.playerSettingLl,
                dataBinding.scanManagerLl,
                dataBinding.cacheManagerLl,
                dataBinding.commonlyManagerLl,
                dataBinding.bilibiliDanmuLl,
                dataBinding.shooterSubtitleLl,
                dataBinding.screencastReceiverLl,
                dataBinding.feedbackLl,
                dataBinding.appSettingLl
            )
            
            val focusIndex = focusableViews.indexOfFirst { it.id == focusedView.id }
            if (focusIndex != -1) {
                val focusKey = "${this::class.java.simpleName}_personal_focus_index"
                requireContext().getSharedPreferences("tv_focus", 0)
                    .edit()
                    .putInt(focusKey, focusIndex)
                    .apply()
            }
        }
    }

    /**
     * 恢复TV端焦点状态
     */
    private fun restoreTvFocus() {
        val focusKey = "${this::class.java.simpleName}_personal_focus_index"
        dataBinding.root.post {
            val savedFocusIndex = requireContext().getSharedPreferences("tv_focus", 0)
                .getInt(focusKey, 0)
            
            val focusableViews = listOf(
                dataBinding.userCoverIv,
                dataBinding.followAnimeLl,
                dataBinding.cloudHistoryLl,
                dataBinding.playerSettingLl,
                dataBinding.scanManagerLl,
                dataBinding.cacheManagerLl,
                dataBinding.commonlyManagerLl,
                dataBinding.bilibiliDanmuLl,
                dataBinding.shooterSubtitleLl,
                dataBinding.screencastReceiverLl,
                dataBinding.feedbackLl,
                dataBinding.appSettingLl
            )
            
            if (savedFocusIndex in 0 until focusableViews.size) {
                focusableViews[savedFocusIndex].requestFocus()
            } else {
                // 默认聚焦到第一个可聚焦元素
                dataBinding.userCoverIv.requestFocus()
            }
        }
    }

    /**
     * 处理TV端按键事件
     */
    fun handleKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action != KeyEvent.ACTION_DOWN) {
            return false
        }

        // 处理方向键事件，防止焦点跳到左侧菜单栏
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // 消费左键事件，防止焦点跳到左侧菜单栏
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // 消费右键事件，防止焦点跳到右侧
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                // 允许向上导航
                return false
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // 允许向下导航
                return false
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                // 确认键由系统自动处理
                return false
            }
        }

        return false
    }
}