package com.xyoye.common_component.utils.update

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.xyoye.common_component.config.AppConfig
import com.xyoye.common_component.utils.AppUtils
import com.xyoye.common_component.utils.VersionUtils
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.common_component.weight.dialog.GitHubUpdateDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 极简版更新检查管理器
 * 使用OkHttp + 内置JSON解析，代码量<50行
 */
object SimpleUpdateManager {
    
    private const val GITHUB_API_URL = "https://api.github.com/repos/okami-horo/DanDanPlayForAndroid/releases/latest"
    
    data class UpdateInfo(
        val versionName: String,
        val downloadUrl: String,
        val updateContent: String
    )

    /**
     * 检查更新（核心功能）
     */
    suspend fun checkUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(GITHUB_API_URL).build()
        
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: return@withContext null)
        
        val tagName = json.getString("tag_name").removePrefix("v")
        if (!VersionUtils.isNewerVersion(AppUtils.getVersionName(), tagName)) return@withContext null
        
        val assets = json.getJSONArray("assets")
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk")) {
                val downloadUrl = asset.getString("browser_download_url")
                val proxyUrl = if (AppConfig.isEnableGitHubProxy()) "https://ghproxy.net/$downloadUrl" else downloadUrl
                
                return@withContext UpdateInfo(
                    versionName = tagName,
                    downloadUrl = proxyUrl,
                    updateContent = json.optString("body", "暂无更新说明")
                )
            }
        }
        null
    }

    /**
     * 手动检查更新
     */
    suspend fun checkUpdateManually(activity: Activity): Boolean {
        return try {
            val info = checkUpdate()
            if (info != null) {
                showUpdateDialog(activity, info)
                true
            } else {
                ToastCenter.showToast("已是最新版本")
                false
            }
        } catch (e: Exception) {
            ToastCenter.showToast("检查更新失败")
            false
        }
    }

    /**
     * 显示更新对话框
     */
    private fun showUpdateDialog(activity: Activity, info: UpdateInfo) {
        val dialog = GitHubUpdateDialog(activity, 
            com.xyoye.common_component.network.bean.UpdateInfo(
                versionName = info.versionName,
                versionCode = 0,
                updateContent = info.updateContent,
                downloadUrl = info.downloadUrl,
                fileSize = 0,
                isBeta = false,
                isForceUpdate = false,
                releaseDate = ""
            ), 
            GitHubUpdateDialog.Status.Update
        )
        dialog.setPositive { 
            downloadUpdate(activity, info.downloadUrl)
        }
        dialog.show()
    }

    /**
     * 下载更新文件
     */
    private fun downloadUpdate(activity: Activity, downloadUrl: String) {
        // 复用现有下载逻辑
        val updateInfo = com.xyoye.common_component.network.bean.UpdateInfo(
            versionName = "",
            versionCode = 0,
            updateContent = "",
            downloadUrl = downloadUrl,
            fileSize = 0,
            isBeta = false,
            isForceUpdate = false,
            releaseDate = ""
        )
        UpdateManager.downloadUpdate(activity, updateInfo)
    }
}