package com.xyoye.common_component.network.repository

import com.xyoye.common_component.config.AppConfig
import com.xyoye.common_component.network.Retrofit
import com.xyoye.common_component.network.bean.GitHubReleaseBean
import com.xyoye.common_component.network.bean.UpdateInfo
import com.xyoye.common_component.utils.AppUtils
import com.xyoye.common_component.utils.VersionUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * 更新检查Repository
 * Created by xyoye on 2025/7/18.
 */

object UpdateRepository : BaseRepository() {
    
    private const val GITHUB_API_BASE_URL = "https://api.github.com/"
    private const val REPO_OWNER = "okami-horo"
    private const val REPO_NAME = "DanDanPlayForAndroid"
    private const val APK_FILE_EXTENSION = ".apk"
    private const val GITHUB_PROXY_URL = "https://ghproxy.net/"
    
    /**
     * 检查更新
     * @param includeBeta 是否包含Beta版本
     * @param allowSameVersionBeta 是否允许重新下载相同版本的Beta包
     * @return 更新信息，如果没有更新返回null
     */
    suspend fun checkUpdate(
        includeBeta: Boolean = false,
        allowSameVersionBeta: Boolean = true
    ): Result<UpdateInfo?> = request()
        .doGet<List<GitHubReleaseBean>> {
            Retrofit.gitHubService.getReleases(
                baseUrl = GITHUB_API_BASE_URL,
                owner = REPO_OWNER,
                repo = REPO_NAME,
                page = 1,
                perPage = 20
            )
        }.map { releases ->
            val currentVersion = AppUtils.getVersionName()
            findAvailableUpdate(
                releases = releases,
                currentVersion = currentVersion,
                includeBeta = includeBeta,
                allowSameVersionBeta = allowSameVersionBeta
            )
        }
    
    /**
     * 获取最新的正式版本
     */
    suspend fun getLatestRelease(): Result<UpdateInfo?> = request()
        .doGet<GitHubReleaseBean> {
            Retrofit.gitHubService.getLatestRelease(
                baseUrl = GITHUB_API_BASE_URL,
                owner = REPO_OWNER,
                repo = REPO_NAME
            )
        }.map { release ->
            convertToUpdateInfo(release)
        }.mapCatching { updateInfo ->
            if (updateInfo != null) {
                updateInfo
            } else {
                throw Exception("无法解析Release信息")
            }
        }
    
    /**
     * 查找可用的更新
     */
    private fun findAvailableUpdate(
        releases: List<GitHubReleaseBean>,
        currentVersion: String,
        includeBeta: Boolean,
        allowSameVersionBeta: Boolean
    ): UpdateInfo? {
        val validReleases = releases.filter { release ->
            // 过滤掉草稿版本
            !release.draft &&
            // 根据设置过滤Beta版本
            (includeBeta || !release.prerelease) &&
            // 必须有APK文件
            release.assets.any { it.name.endsWith(APK_FILE_EXTENSION, ignoreCase = true) }
        }
        
        // 如果明确请求beta包，优先返回最新的beta版本
        if (includeBeta) {
            val latestBeta = validReleases
                .filter { it.prerelease }
                .maxByOrNull { it.publishedAt ?: it.createdAt ?: "" }
            
            if (latestBeta != null) {
                return convertToUpdateInfo(latestBeta)
            }
        }
        
        // 正常的版本检查逻辑
        for (release in validReleases) {
            val releaseVersion = extractVersionFromTag(release.tagName) ?: continue
            val updateInfo = convertToUpdateInfo(release) ?: continue
            
            // 如果是Beta版本且允许重新下载相同版本
            if (release.prerelease && allowSameVersionBeta) {
                if (VersionUtils.isBetaVersion(currentVersion) && 
                    VersionUtils.parseVersion(currentVersion)?.let { current ->
                        VersionUtils.parseVersion(releaseVersion)?.let { remote ->
                            current.major == remote.major &&
                            current.minor == remote.minor &&
                            current.patch == remote.patch
                        }
                    } == true) {
                    return updateInfo
                }
            }
            
            // 检查是否为更新版本
            if (VersionUtils.isNewerVersion(currentVersion, releaseVersion)) {
                return updateInfo
            }
            
            // 如果允许相同版本Beta包下载，且当前版本是Beta版本，且版本号相同
            if (allowSameVersionBeta && release.prerelease && 
                VersionUtils.isBetaVersion(currentVersion) &&
                currentVersion == releaseVersion) {
                return updateInfo
            }
        }
        
        return null
    }
    
    /**
     * 将GitHub Release转换为UpdateInfo
     */
    private fun convertToUpdateInfo(release: GitHubReleaseBean): UpdateInfo? {
        val versionName = extractVersionFromTag(release.tagName) ?: return null
        val versionInfo = VersionUtils.parseVersion(versionName) ?: return null
        
        // 查找APK文件
        val apkAsset = release.assets.find { 
            it.name.endsWith(APK_FILE_EXTENSION, ignoreCase = true) 
        } ?: return null
        
        // 解析发布日期
        val releaseDate = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(release.publishedAt ?: release.createdAt)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "未知"
        }
        
        // 为下载URL添加代理加速
        val downloadUrl = if (AppConfig.isEnableGitHubProxy()) {
            GITHUB_PROXY_URL + apkAsset.browserDownloadUrl
        } else {
            apkAsset.browserDownloadUrl
        }
        
        return UpdateInfo(
            versionName = versionName,
            versionCode = generateVersionCode(versionInfo),
            updateContent = release.body ?: "暂无更新说明",
            downloadUrl = downloadUrl,
            fileSize = apkAsset.size,
            isBeta = release.prerelease,
            isForceUpdate = false,
            releaseDate = releaseDate
        )
    }
    
    /**
     * 从tag中提取版本号
     */
    private fun extractVersionFromTag(tag: String): String? {
        // 移除常见的前缀
        val cleanTag = tag.removePrefix("v").removePrefix("version-").removePrefix("release-")
        
        // 验证是否为有效的版本号格式
        return if (VersionUtils.parseVersion(cleanTag) != null) {
            cleanTag
        } else {
            null
        }
    }
    
    /**
     * 根据版本信息生成版本代码
     */
    private fun generateVersionCode(versionInfo: VersionUtils.VersionInfo): Long {
        // 主版本号 * 10000 + 次版本号 * 100 + 修订版本号
        var code = (versionInfo.major * 10000 + versionInfo.minor * 100 + versionInfo.patch).toLong()
        
        // 如果是预发布版本，减去一定的值以确保正式版本的版本代码更大
        if (versionInfo.isPreRelease) {
            code -= when (versionInfo.preReleaseType?.lowercase()) {
                "alpha" -> 30
                "beta" -> 20
                "rc" -> 10
                else -> 5
            }
            // 加上预发布版本号
            code += versionInfo.preReleaseVersion
        }
        
        return code
    }
}