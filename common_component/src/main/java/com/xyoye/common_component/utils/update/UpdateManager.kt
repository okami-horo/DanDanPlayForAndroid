package com.xyoye.common_component.utils.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.xyoye.common_component.config.AppConfig
import com.xyoye.common_component.network.bean.UpdateInfo
import com.xyoye.common_component.network.repository.UpdateRepository
import com.xyoye.common_component.utils.AppUtils
import com.xyoye.common_component.utils.VersionUtils
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.common_component.weight.dialog.GitHubUpdateDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 更新检查管理器
 * Created by xyoye on 2025/7/18.
 */

object UpdateManager {
    
    /**
     * 更新检查回调接口
     */
    interface UpdateCheckCallback {
        /**
         * 发现新版本
         */
        fun onUpdateAvailable(updateInfo: UpdateInfo)
        
        /**
         * 没有新版本
         */
        fun onNoUpdate()
        
        /**
         * 检查失败
         */
        fun onCheckFailed(error: Throwable)
    }
    
    /**
     * 更新下载回调接口
     */
    interface UpdateDownloadCallback {
        /**
         * 下载开始
         */
        fun onDownloadStart()
        
        /**
         * 下载进度
         */
        fun onDownloadProgress(progress: Int)
        
        /**
         * 下载完成
         */
        fun onDownloadComplete(filePath: String)
        
        /**
         * 下载失败
         */
        fun onDownloadFailed(error: Throwable)
    }
    
    /**
     * 手动检查更新
     * @param activity 当前Activity
     * @param callback 检查回调
     * @param showNoUpdateToast 没有更新时是否显示提示
     */
    fun checkUpdateManually(
        activity: Activity,
        callback: UpdateCheckCallback? = null,
        showNoUpdateToast: Boolean = true
    ) {
        (activity as LifecycleOwner).lifecycleScope.launch {
            try {
                val includeBeta = AppConfig.isCheckBetaUpdate()
                val allowSameVersionBeta = AppConfig.isAllowSameVersionBeta()
                
                val result = withContext(Dispatchers.IO) {
                    UpdateRepository.checkUpdate(includeBeta, allowSameVersionBeta) as Result<UpdateInfo?>
                }
                
                if (result.isSuccess) {
                    val updateInfo = result.getOrNull()
                    if (updateInfo != null) {
                    callback?.onUpdateAvailable(updateInfo)
                    showUpdateDialog(activity, updateInfo)
                } else {
                    callback?.onNoUpdate()
                    if (showNoUpdateToast) {
                        ToastCenter.showToast("已是最新版本")
                    }
                }
                } else {
                    callback?.onCheckFailed(result.exceptionOrNull() ?: Exception("未知错误"))
                    if (showNoUpdateToast) {
                        ToastCenter.showToast("检查更新失败，请稍后重试")
                    }
                }
            } catch (e: Exception) {
                callback?.onCheckFailed(e)
                if (showNoUpdateToast) {
                    ToastCenter.showToast("检查更新失败，请稍后重试")
                }
            }
        }
    }
    
    /**
     * 自动检查更新（静默检查）
     * @param context 上下文
     * @param callback 检查回调
     */
    fun checkUpdateAutomatically(
        context: Context,
        callback: UpdateCheckCallback? = null
    ) {
        // 检查是否启用自动更新检查
        if (!AppConfig.isAutoCheckUpdate()) {
            return
        }
        
        // 检查距离上次检查的时间间隔
        val lastCheckTime = AppConfig.getLastUpdateCheckTime()
        val currentTime = System.currentTimeMillis()
        val checkInterval = AppConfig.getUpdateCheckInterval() * 24 * 60 * 60 * 1000L // 转换为毫秒
        
        if (currentTime - lastCheckTime < checkInterval) {
            return
        }
        
        // 执行检查
        kotlinx.coroutines.GlobalScope.launch {
            try {
                val includeBeta = AppConfig.isCheckBetaUpdate()
                val allowSameVersionBeta = AppConfig.isAllowSameVersionBeta()
                
                val result = UpdateRepository.checkUpdate(includeBeta, allowSameVersionBeta) as Result<UpdateInfo?>
                
                if (result.isSuccess) {
                    // 更新最后检查时间
                    AppConfig.putLastUpdateCheckTime(currentTime)
                    
                    val updateInfo = result.getOrNull()
                    if (updateInfo != null) {
                        callback?.onUpdateAvailable(updateInfo)
                        // 自动检查发现更新时，可以选择显示通知或其他提示方式
                    } else {
                        callback?.onNoUpdate()
                    }
                } else {
                    callback?.onCheckFailed(result.exceptionOrNull() ?: Exception("未知错误"))
                }
            } catch (e: Exception) {
                callback?.onCheckFailed(e)
            }
        }
    }
    
    /**
     * 显示更新对话框
     */
    private fun showUpdateDialog(activity: Activity, updateInfo: UpdateInfo) {
        val dialog = GitHubUpdateDialog(activity, updateInfo, GitHubUpdateDialog.Status.Update)

        dialog.setPositive {
            // 开始下载更新
            downloadUpdate(activity, updateInfo, object : UpdateDownloadCallback {
                override fun onDownloadStart() {
                    // 下载开始，可以在这里显示通知
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
                        installUpdate(activity, filePath)
                    }
                    installDialog.show()
                }

                override fun onDownloadFailed(error: Throwable) {
                    // TODO: 显示下载失败的提示
                }
            })
        }

        dialog.setNegative {
            // 用户取消更新
        }

        dialog.show()
    }
    
    /**
     * 下载更新包
     * @param context 上下文
     * @param updateInfo 更新信息
     * @param callback 下载回调
     */
    fun downloadUpdate(
        context: Context,
        updateInfo: UpdateInfo,
        callback: UpdateDownloadCallback? = null
    ) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                callback?.onDownloadStart()

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(updateInfo.downloadUrl)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("下载失败: ${response.code}")
                }

                val body = response.body ?: throw IOException("响应体为空")
                val contentLength = body.contentLength()

                // 创建下载目录
                val downloadDir = File(context.getExternalFilesDir(null), "downloads")
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                // 生成文件名
                val fileName = "DanDanPlay_${updateInfo.versionName}.apk"
                val file = File(downloadDir, fileName)

                // 如果文件已存在，删除旧文件
                if (file.exists()) {
                    file.delete()
                }

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(file)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (contentLength > 0) {
                        val progress = (totalBytesRead * 100 / contentLength).toInt()
                        withContext(Dispatchers.Main) {
                            callback?.onDownloadProgress(progress)
                        }
                    }
                }

                outputStream.close()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    callback?.onDownloadComplete(file.absolutePath)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onDownloadFailed(e)
                }
            }
        }
    }
    
    /**
     * 安装更新包
     * @param context 上下文
     * @param filePath APK文件路径
     */
    fun installUpdate(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                // TODO: 显示文件不存在的提示
                return
            }

            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0及以上使用FileProvider
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            context.startActivity(intent)

        } catch (e: Exception) {
            // TODO: 显示安装失败的提示
            e.printStackTrace()
        }
    }
    
    /**
     * 检查当前版本是否为Beta版本
     */
    fun isCurrentVersionBeta(): Boolean {
        val currentVersion = AppUtils.getVersionName()
        return VersionUtils.isBetaVersion(currentVersion)
    }
    
    /**
     * 获取当前版本的显示名称
     */
    fun getCurrentVersionDisplayName(): String {
        val currentVersion = AppUtils.getVersionName()
        return VersionUtils.getVersionDisplayName(currentVersion)
    }
}
