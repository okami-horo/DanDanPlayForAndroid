package com.xyoye.common_component.utils

import android.app.Activity
import androidx.core.content.pm.PackageInfoCompat
import com.taobao.update.datasource.UpdateDataSource
import com.xyoye.common_component.base.app.BaseApplication
import com.xyoye.common_component.config.AppConfig
import com.xyoye.common_component.utils.update.UpdateManager

/**
 * Created by xyoye on 2020/8/19.
 */

object AppUtils {
    fun getVersionCode(): Long {
        if (SecurityHelper.getInstance().isOfficialApplication) {
            val packageName = BaseApplication.getAppContext().applicationInfo.packageName
            val packageInfo =
                BaseApplication.getAppContext().packageManager.getPackageInfo(packageName, 0)
            return PackageInfoCompat.getLongVersionCode(packageInfo)
        }
        return 0L
    }

    fun getVersionName(): String {
        if (SecurityHelper.getInstance().isOfficialApplication) {
            val packageName = BaseApplication.getAppContext().applicationInfo.packageName
            val packageInfo =
                BaseApplication.getAppContext().packageManager.getPackageInfo(packageName, 0)
            return packageInfo.versionName
        }
        return "unknown"
    }

    fun checkUpdate() {
        UpdateDataSource.getInstance().startManualUpdate(false)
    }

    /**
     * 检查GitHub更新
     * @param activity 当前Activity
     * @param showNoUpdateToast 没有更新时是否显示提示
     */
    fun checkGitHubUpdate(activity: Activity, showNoUpdateToast: Boolean = true) {
        UpdateManager.checkUpdateManually(activity, showNoUpdateToast = showNoUpdateToast)
    }

    /**
     * 自动检查GitHub更新
     */
    fun autoCheckGitHubUpdate() {
        val context = BaseApplication.getAppContext()
        UpdateManager.checkUpdateAutomatically(context)
    }
}