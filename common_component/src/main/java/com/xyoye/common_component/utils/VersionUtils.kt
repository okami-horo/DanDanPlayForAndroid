package com.xyoye.common_component.utils

import java.util.regex.Pattern

/**
 * 版本比较工具类
 * 支持语义化版本号比较 (Semantic Versioning)
 * Created by xyoye on 2025/7/18.
 */

object VersionUtils {
    
    private val VERSION_PATTERN = Pattern.compile(
        "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-(alpha|beta|rc)(?:\\.(\\d+))?)?(?:\\+([0-9A-Za-z\\-\\.]+))?$"
    )
    
    /**
     * 版本信息数据类
     */
    data class VersionInfo(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preReleaseType: String? = null,
        val preReleaseVersion: Int = 0,
        val buildMetadata: String? = null
    ) {
        /**
         * 是否为预发布版本
         */
        val isPreRelease: Boolean
            get() = preReleaseType != null
            
        /**
         * 获取预发布类型的优先级
         */
        private fun getPreReleaseTypePriority(): Int {
            return when (preReleaseType?.lowercase()) {
                "alpha" -> 1
                "beta" -> 2
                "rc" -> 3
                else -> 4 // 正式版本
            }
        }
        
        /**
         * 比较版本大小
         * @param other 另一个版本
         * @return 1: 当前版本更新, 0: 版本相同, -1: 当前版本更旧
         */
        fun compareTo(other: VersionInfo): Int {
            // 比较主版本号
            if (major != other.major) {
                return major.compareTo(other.major)
            }
            
            // 比较次版本号
            if (minor != other.minor) {
                return minor.compareTo(other.minor)
            }
            
            // 比较修订版本号
            if (patch != other.patch) {
                return patch.compareTo(other.patch)
            }
            
            // 如果一个是预发布版本，一个是正式版本
            if (isPreRelease != other.isPreRelease) {
                return if (isPreRelease) -1 else 1
            }
            
            // 如果都是预发布版本，比较预发布类型
            if (isPreRelease && other.isPreRelease) {
                val typePriority = getPreReleaseTypePriority()
                val otherTypePriority = other.getPreReleaseTypePriority()
                
                if (typePriority != otherTypePriority) {
                    return typePriority.compareTo(otherTypePriority)
                }
                
                // 比较预发布版本号
                return preReleaseVersion.compareTo(other.preReleaseVersion)
            }
            
            return 0
        }
    }
    
    /**
     * 解析版本字符串
     * @param versionString 版本字符串，如 "4.1.3", "4.2.0-beta.1", "4.1.3-alpha"
     * @return 解析后的版本信息，解析失败返回null
     */
    fun parseVersion(versionString: String): VersionInfo? {
        val cleanVersion = versionString.removePrefix("v").trim()
        val matcher = VERSION_PATTERN.matcher(cleanVersion)
        
        if (!matcher.matches()) {
            return null
        }
        
        try {
            val major = matcher.group(1)?.toInt() ?: return null
            val minor = matcher.group(2)?.toInt() ?: return null
            val patch = matcher.group(3)?.toInt() ?: return null
            val preReleaseType = matcher.group(4)
            val preReleaseVersion = matcher.group(5)?.toInt() ?: 0
            val buildMetadata = matcher.group(6)
            
            return VersionInfo(
                major = major,
                minor = minor,
                patch = patch,
                preReleaseType = preReleaseType,
                preReleaseVersion = preReleaseVersion,
                buildMetadata = buildMetadata
            )
        } catch (e: NumberFormatException) {
            return null
        }
    }
    
    /**
     * 比较两个版本字符串
     * @param currentVersion 当前版本
     * @param newVersion 新版本
     * @return true: 新版本更新, false: 新版本不更新或解析失败
     */
    fun isNewerVersion(currentVersion: String, newVersion: String): Boolean {
        val current = parseVersion(currentVersion) ?: return false
        val new = parseVersion(newVersion) ?: return false
        
        return new.compareTo(current) > 0
    }
    
    /**
     * 检查版本是否为Beta版本
     * @param versionString 版本字符串
     * @return true: 是Beta版本, false: 不是Beta版本
     */
    fun isBetaVersion(versionString: String): Boolean {
        val versionInfo = parseVersion(versionString) ?: return false
        return versionInfo.preReleaseType?.lowercase() == "beta"
    }
    
    /**
     * 检查版本是否为预发布版本
     * @param versionString 版本字符串
     * @return true: 是预发布版本, false: 不是预发布版本
     */
    fun isPreReleaseVersion(versionString: String): Boolean {
        val versionInfo = parseVersion(versionString) ?: return false
        return versionInfo.isPreRelease
    }
    
    /**
     * 获取版本的显示名称
     * @param versionString 版本字符串
     * @return 格式化后的版本显示名称
     */
    fun getVersionDisplayName(versionString: String): String {
        val versionInfo = parseVersion(versionString) ?: return versionString
        
        val baseVersion = "${versionInfo.major}.${versionInfo.minor}.${versionInfo.patch}"
        
        return if (versionInfo.isPreRelease) {
            val preReleaseText = when (versionInfo.preReleaseType?.lowercase()) {
                "alpha" -> "Alpha"
                "beta" -> "Beta"
                "rc" -> "RC"
                else -> versionInfo.preReleaseType?.uppercase() ?: ""
            }
            
            if (versionInfo.preReleaseVersion > 0) {
                "$baseVersion $preReleaseText ${versionInfo.preReleaseVersion}"
            } else {
                "$baseVersion $preReleaseText"
            }
        } else {
            baseVersion
        }
    }
}
