package com.xyoye.common_component.network.bean

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * GitHub Release API响应数据模型
 * Created by xyoye on 2025/7/18.
 */

@JsonClass(generateAdapter = true)
data class GitHubReleaseBean(
    @Json(name = "id")
    val id: Long,
    
    @Json(name = "tag_name")
    val tagName: String,
    
    @Json(name = "name")
    val name: String,
    
    @Json(name = "body")
    val body: String?,
    
    @Json(name = "prerelease")
    val prerelease: Boolean,
    
    @Json(name = "draft")
    val draft: Boolean,
    
    @Json(name = "created_at")
    val createdAt: String,
    
    @Json(name = "published_at")
    val publishedAt: String?,
    
    @Json(name = "assets")
    val assets: List<GitHubAssetBean>,
    
    @Json(name = "html_url")
    val htmlUrl: String
)

@JsonClass(generateAdapter = true)
data class GitHubAssetBean(
    @Json(name = "id")
    val id: Long,
    
    @Json(name = "name")
    val name: String,
    
    @Json(name = "size")
    val size: Long,
    
    @Json(name = "download_count")
    val downloadCount: Int,
    
    @Json(name = "browser_download_url")
    val browserDownloadUrl: String,
    
    @Json(name = "content_type")
    val contentType: String
)

/**
 * 更新信息UI模型
 */
data class UpdateInfo(
    val versionName: String,
    val versionCode: Long,
    val updateContent: String,
    val downloadUrl: String,
    val fileSize: Long,
    val isBeta: Boolean,
    val isForceUpdate: Boolean = false,
    val releaseDate: String
) {
    /**
     * 获取格式化的文件大小
     */
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "${fileSize}B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024}KB"
            fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)}MB"
            else -> "${fileSize / (1024 * 1024 * 1024)}GB"
        }
    }
    
    /**
     * 获取版本类型标识
     */
    fun getVersionTypeLabel(): String {
        return if (isBeta) "Beta" else "正式版"
    }
}
