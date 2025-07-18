package com.xyoye.common_component.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * 版本比较工具类测试
 * Created by xyoye on 2025/7/18.
 */

class VersionUtilsTest {

    @Test
    fun testParseVersion() {
        // 测试正常版本号解析
        val version1 = VersionUtils.parseVersion("4.1.3")
        assertNotNull(version1)
        assertEquals(4, version1!!.major)
        assertEquals(1, version1.minor)
        assertEquals(3, version1.patch)
        assertFalse(version1.isPreRelease)

        // 测试Beta版本解析
        val version2 = VersionUtils.parseVersion("4.2.0-beta.1")
        assertNotNull(version2)
        assertEquals(4, version2!!.major)
        assertEquals(2, version2.minor)
        assertEquals(0, version2.patch)
        assertEquals("beta", version2.preReleaseType)
        assertEquals(1, version2.preReleaseVersion)
        assertTrue(version2.isPreRelease)

        // 测试Alpha版本解析
        val version3 = VersionUtils.parseVersion("4.1.0-alpha")
        assertNotNull(version3)
        assertEquals("alpha", version3!!.preReleaseType)
        assertTrue(version3.isPreRelease)

        // 测试带v前缀的版本号
        val version4 = VersionUtils.parseVersion("v4.1.3")
        assertNotNull(version4)
        assertEquals(4, version4!!.major)
        assertEquals(1, version4.minor)
        assertEquals(3, version4.patch)

        // 测试无效版本号
        val version5 = VersionUtils.parseVersion("invalid.version")
        assertNull(version5)
    }

    @Test
    fun testVersionComparison() {
        // 测试正式版本比较
        assertTrue(VersionUtils.isNewerVersion("4.1.3", "4.1.4"))
        assertTrue(VersionUtils.isNewerVersion("4.1.3", "4.2.0"))
        assertTrue(VersionUtils.isNewerVersion("4.1.3", "5.0.0"))
        assertFalse(VersionUtils.isNewerVersion("4.1.3", "4.1.3"))
        assertFalse(VersionUtils.isNewerVersion("4.1.4", "4.1.3"))

        // 测试Beta版本比较
        assertTrue(VersionUtils.isNewerVersion("4.1.3", "4.1.4-beta.1"))
        assertTrue(VersionUtils.isNewerVersion("4.1.3-beta.1", "4.1.3"))
        assertTrue(VersionUtils.isNewerVersion("4.1.3-beta.1", "4.1.3-beta.2"))
        assertTrue(VersionUtils.isNewerVersion("4.1.3-alpha.1", "4.1.3-beta.1"))

        // 测试Alpha版本比较
        assertTrue(VersionUtils.isNewerVersion("4.1.3-alpha.1", "4.1.3-alpha.2"))
        assertTrue(VersionUtils.isNewerVersion("4.1.3-alpha.1", "4.1.3-beta.1"))
        assertTrue(VersionUtils.isNewerVersion("4.1.3-alpha.1", "4.1.3"))
    }

    @Test
    fun testIsBetaVersion() {
        assertTrue(VersionUtils.isBetaVersion("4.1.3-beta.1"))
        assertTrue(VersionUtils.isBetaVersion("4.1.3-beta"))
        assertFalse(VersionUtils.isBetaVersion("4.1.3"))
        assertFalse(VersionUtils.isBetaVersion("4.1.3-alpha.1"))
        assertFalse(VersionUtils.isBetaVersion("4.1.3-rc.1"))
    }

    @Test
    fun testIsPreReleaseVersion() {
        assertTrue(VersionUtils.isPreReleaseVersion("4.1.3-beta.1"))
        assertTrue(VersionUtils.isPreReleaseVersion("4.1.3-alpha.1"))
        assertTrue(VersionUtils.isPreReleaseVersion("4.1.3-rc.1"))
        assertFalse(VersionUtils.isPreReleaseVersion("4.1.3"))
    }

    @Test
    fun testGetVersionDisplayName() {
        assertEquals("4.1.3", VersionUtils.getVersionDisplayName("4.1.3"))
        assertEquals("4.1.3 Beta 1", VersionUtils.getVersionDisplayName("4.1.3-beta.1"))
        assertEquals("4.1.3 Beta", VersionUtils.getVersionDisplayName("4.1.3-beta"))
        assertEquals("4.1.3 Alpha 1", VersionUtils.getVersionDisplayName("4.1.3-alpha.1"))
        assertEquals("4.1.3 RC 1", VersionUtils.getVersionDisplayName("4.1.3-rc.1"))
    }

    @Test
    fun testVersionInfoCompareTo() {
        val version1 = VersionUtils.parseVersion("4.1.3")!!
        val version2 = VersionUtils.parseVersion("4.1.4")!!
        val version3 = VersionUtils.parseVersion("4.1.3-beta.1")!!
        val version4 = VersionUtils.parseVersion("4.1.3-beta.2")!!
        val version5 = VersionUtils.parseVersion("4.1.3-alpha.1")!!

        // 正式版本比较
        assertTrue(version2.compareTo(version1) > 0)
        assertTrue(version1.compareTo(version2) < 0)
        assertEquals(0, version1.compareTo(version1))

        // Beta版本与正式版本比较
        assertTrue(version1.compareTo(version3) > 0)
        assertTrue(version3.compareTo(version1) < 0)

        // Beta版本之间比较
        assertTrue(version4.compareTo(version3) > 0)
        assertTrue(version3.compareTo(version4) < 0)

        // Alpha与Beta比较
        assertTrue(version3.compareTo(version5) > 0)
        assertTrue(version5.compareTo(version3) < 0)
    }
}
