package com.xyoye.common_component.weight.dialog

import android.app.Dialog
import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.xyoye.common_component.R
import com.xyoye.common_component.databinding.DialogGithubUpdateBinding
import com.xyoye.common_component.network.bean.UpdateInfo

/**
 * GitHub更新弹窗
 * Created by xyoye on 2025/7/18.
 */

class GitHubUpdateDialog(
    context: Context,
    private val updateInfo: UpdateInfo,
    private val status: Status = Status.Update
) : Dialog(context, R.style.UpdateDialog) {

    enum class Status {
        Update,
        UpdateForce,
        Updating,
        Install
    }

    private val dataBinding: DialogGithubUpdateBinding = DataBindingUtil.inflate(
        layoutInflater,
        R.layout.dialog_github_update,
        null,
        false
    )

    private var positiveBlock: (() -> Unit)? = null
    private var negativeBlock: (() -> Unit)? = null

    init {
        setContentView(dataBinding.root)

        setCancelable(false)
        setCanceledOnTouchOutside(false)

        initWindow()
        initListener()
        setupStatus(status)
        setupUpdateInfo()
    }

    private fun initWindow() {
        window?.apply {
            decorView.setPadding(0, decorView.top, 0, decorView.bottom)

            val layoutParams = attributes
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
            attributes = layoutParams

            setGravity(Gravity.CENTER)
        }
    }

    private fun initListener() {
        dataBinding.tvUpdate.setOnClickListener {
            positiveBlock?.invoke()
            setupStatus(Status.Updating)
            dataBinding.tvProgress.text = "下载中：0%"
        }

        dataBinding.tvInstall.setOnClickListener {
            positiveBlock?.invoke()
            dismiss()
        }

        dataBinding.ivDialogClose.setOnClickListener {
            negativeBlock?.invoke()
            dismiss()
        }
    }

    private fun setupStatus(status: Status) {
        dataBinding.tvUpdate.isVisible = status == Status.Update || status == Status.UpdateForce
        dataBinding.tvInstall.isVisible = status == Status.Install
        dataBinding.tvProgress.isVisible = status == Status.Updating
        dataBinding.viewProgress.isVisible = status == Status.Updating
        dataBinding.ivDialogClose.isVisible = !updateInfo.isForceUpdate
    }

    private fun setupUpdateInfo() {
        // 版本号显示
        val versionDisplay = "v${updateInfo.versionName}"
        dataBinding.tvVersion.text = versionDisplay
        
        // 版本类型标签
        dataBinding.tvVersionType.text = updateInfo.getVersionTypeLabel()
        dataBinding.tvVersionType.isVisible = updateInfo.isBeta
        
        // 更新内容
        dataBinding.tvUpdateContent.text = updateInfo.updateContent
        dataBinding.tvUpdateContent.movementMethod = ScrollingMovementMethod()
        
        // 文件大小
        dataBinding.tvFileSize.text = "文件大小：${updateInfo.getFormattedFileSize()}"
        
        // 发布日期
        dataBinding.tvReleaseDate.text = "发布日期：${updateInfo.releaseDate}"
        
        // 如果是Beta版本，显示特殊提示
        if (updateInfo.isBeta) {
            dataBinding.tvBetaWarning.isVisible = true
            dataBinding.tvBetaWarning.text = "⚠️ 这是一个测试版本，可能存在不稳定因素"
        } else {
            dataBinding.tvBetaWarning.isVisible = false
        }
    }

    fun setPositive(block: () -> Unit) {
        this.positiveBlock = block
    }

    fun setNegative(block: () -> Unit) {
        this.negativeBlock = block
    }

    fun updateProgress(progress: Int) {
        if (status == Status.Install || !isShowing) {
            return
        }

        dataBinding.tvProgress.post {
            setupStatus(Status.Updating)
            val tips = "下载中：$progress%"
            dataBinding.tvProgress.text = tips
            dataBinding.viewProgress.progress = progress
        }
    }
}
