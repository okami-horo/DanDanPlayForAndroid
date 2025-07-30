package com.xyoye.user_component.ui.fragment

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.alibaba.android.arouter.launcher.ARouter
import com.xyoye.common_component.config.AppConfig
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.network.config.Api
import com.xyoye.common_component.utils.AppUtils
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.user_component.R
import com.xyoye.common_component.network.repository.UpdateRepository
import com.xyoye.common_component.utils.update.UpdateManager
import com.xyoye.common_component.weight.dialog.GitHubUpdateDialog
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.*

/**
 * Created by xyoye on 2021/2/23.
 */

class AppSettingFragment : PreferenceFragmentCompat() {

    companion object {
        fun newInstance() = AppSettingFragment()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = AppSettingDataStore()
        addPreferencesFromResource(R.xml.preference_app_setting)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val backupDomainAddress = findPreference<EditTextPreference>("backup_domain_address")

        findPreference<Preference>("dark_mode")?.apply {
            setOnPreferenceClickListener {
                ARouter.getInstance()
                    .build(RouteTable.User.SwitchTheme)
                    .navigation()
                return@setOnPreferenceClickListener true
            }
        }

        findPreference<Preference>("app_version")?.apply {
            summary = AppUtils.getVersionName()
            setOnPreferenceClickListener {
                AppUtils.checkUpdate()
                return@setOnPreferenceClickListener true
            }
        }

        findPreference<Preference>("license")?.apply {
            setOnPreferenceClickListener {
                ARouter.getInstance()
                    .build(RouteTable.User.License)
                    .navigation()
                return@setOnPreferenceClickListener true
            }
        }

        findPreference<Preference>("about_us")?.apply {
            setOnPreferenceClickListener {
                ARouter.getInstance()
                    .build(RouteTable.User.AboutUs)
                    .navigation()
                return@setOnPreferenceClickListener true
            }
        }

        // 下载最新的beta包
        findPreference<Preference>("download_beta_package")?.apply {
            setOnPreferenceClickListener {
                // 实现下载最新的beta包功能
                downloadLatestBetaPackage()
                return@setOnPreferenceClickListener true
            }
        }

        // 下载最新的release包
        findPreference<Preference>("download_release_package")?.apply {
            setOnPreferenceClickListener {
                // 实现下载最新的release包功能
                downloadLatestReleasePackage()
                return@setOnPreferenceClickListener true
            }
        }

        findPreference<SwitchPreference>("backup_domain_enable")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                backupDomainAddress?.isVisible = newValue as Boolean
                return@setOnPreferenceChangeListener true
            }
            backupDomainAddress?.isVisible = isChecked
        }

        backupDomainAddress?.apply {
            summary = AppConfig.getBackupDomain()
            setOnPreferenceChangeListener { _, newValue ->
                val newAddress = newValue as String
                if (checkDomainUrl(newAddress)) {
                    summary = newAddress
                    return@setOnPreferenceChangeListener true
                }
                return@setOnPreferenceChangeListener false
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun downloadLatestBetaPackage() {
        val activity = activity ?: return
        ToastCenter.showToast("正在检查最新Beta版本")

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val result = withContext(Dispatchers.IO) {
                    // 获取最新的Beta版本
                    UpdateRepository.checkUpdate(includeBeta = true, allowSameVersionBeta = true)
                }

                if (result.isSuccess) {
                    val updateInfo = result.getOrNull()
                    if (updateInfo != null) {
                        // 显示下载对话框
                        showDownloadDialog(activity, updateInfo)
                    } else {
                        ToastCenter.showToast("未找到可用的Beta版本")
                    }
                } else {
                    ToastCenter.showToast("检查Beta版本失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ToastCenter.showToast("检查Beta版本失败: ${e.message}")
            }
        }
    }

    private fun downloadLatestReleasePackage() {
        val activity = activity ?: return
        ToastCenter.showToast("正在检查最新正式版本")

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val result = withContext(Dispatchers.IO) {
                    // 获取最新的正式版本
                    UpdateRepository.getLatestRelease()
                }

                if (result.isSuccess) {
                    val updateInfo = result.getOrNull()
                    if (updateInfo != null) {
                        // 显示下载对话框
                        showDownloadDialog(activity, updateInfo)
                    } else {
                        ToastCenter.showToast("未找到可用的正式版本")
                    }
                } else {
                    ToastCenter.showToast("检查正式版本失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ToastCenter.showToast("检查正式版本失败: ${e.message}")
            }
        }
    }

    private fun showDownloadDialog(activity: FragmentActivity, updateInfo: com.xyoye.common_component.network.bean.UpdateInfo) {
        val dialog = GitHubUpdateDialog(activity, updateInfo, GitHubUpdateDialog.Status.Update)

        dialog.setPositive {
            // 开始下载更新
            UpdateManager.downloadUpdate(activity, updateInfo, object : UpdateManager.UpdateDownloadCallback {
                override fun onDownloadStart() {
                    // 下载开始
                }

                override fun onDownloadProgress(progress: Int) {
                    dialog.updateProgress(progress)
                }

                override fun onDownloadComplete(filePath: String) {
                    // 下载完成，显示安装对话框
                    val installDialog = GitHubUpdateDialog(
                        activity,
                        updateInfo,
                        GitHubUpdateDialog.Status.Install
                    )
                    installDialog.setPositive {
                        UpdateManager.installUpdate(activity, filePath)
                    }
                    installDialog.show()
                }

                override fun onDownloadFailed(error: Throwable) {
                    ToastCenter.showToast("下载失败: ${error.message}")
                }
            })
        }

        dialog.setNegative {
            // 用户取消更新
        }

        dialog.show()
    }

    private fun checkDomainUrl(url: String): Boolean {
        if (TextUtils.isEmpty(url)) {
            ToastCenter.showError("地址保存失败，地址为空")
            return false
        }
        val uri = Uri.parse(url)
        if (TextUtils.isEmpty(uri.scheme)) {
            ToastCenter.showError("地址保存失败，协议错误")
            return false
        }
        if (TextUtils.isEmpty(uri.host)) {
            ToastCenter.showError("地址保存失败，域名错误")
            return false
        }
        if (uri.port == -1) {
            ToastCenter.showError("地址保存失败，端口错误")
            return false
        }
        return true
    }


    inner class AppSettingDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                "hide_file" -> AppConfig.isShowHiddenFile()
                "splash_page" -> AppConfig.isShowSplashAnimation()
                "backup_domain_enable" -> AppConfig.isBackupDomainEnable()
                "enable_github_proxy" -> AppConfig.isEnableGitHubProxy()
                else -> super.getBoolean(key, defValue)
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                "hide_file" -> AppConfig.putShowHiddenFile(value)
                "splash_page" -> AppConfig.putShowSplashAnimation(value)
                "backup_domain_enable" -> AppConfig.putBackupDomainEnable(value)
                "enable_github_proxy" -> AppConfig.putEnableGitHubProxy(value)
                else -> super.putBoolean(key, value)
            }
        }

        override fun getString(key: String?, defValue: String?): String? {
            return when (key) {
                "backup_domain_address" -> AppConfig.getBackupDomain()
                else -> super.getString(key, defValue)
            }
        }

        override fun putString(key: String?, value: String?) {
            when (key) {
                "backup_domain_address" -> AppConfig.putBackupDomain(value ?: Api.DAN_DAN_SPARE)
                else -> super.putString(key, value)
            }
        }
    }
}