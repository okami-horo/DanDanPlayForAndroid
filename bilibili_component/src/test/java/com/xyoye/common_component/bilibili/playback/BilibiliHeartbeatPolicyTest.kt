package com.xyoye.common_component.bilibili.playback

import com.xyoye.common_component.bilibili.BilibiliHistorySyncMode
import com.xyoye.common_component.bilibili.BilibiliKeys
import com.xyoye.common_component.bilibili.history.BilibiliHistoryStatus
import com.xyoye.common_component.bilibili.history.BilibiliHistoryStatusStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BilibiliHeartbeatPolicyTest {
    @Test
    fun disabledModeSkipsHeartbeat() {
        val decision =
            BilibiliHeartbeatPolicy.decide(
                mode = BilibiliHistorySyncMode.DISABLED,
                historyStatus = null,
                isLoggedIn = true,
                csrf = "csrf-value",
                key = BilibiliKeys.ArchiveKey("BV1disabled", 1L),
            )

        assertFalse(decision.allowed)
        assertEquals(BilibiliHeartbeatPolicy.SkipReason.LOCAL_DISABLED, decision.skipReason)
    }

    @Test
    fun serverPausedSkipsHeartbeat() {
        val decision =
            BilibiliHeartbeatPolicy.decide(
                mode = BilibiliHistorySyncMode.AUTO,
                historyStatus = BilibiliHistoryStatus(isPaused = true, updatedAtMs = 1L),
                isLoggedIn = true,
                csrf = "csrf-value",
                key = BilibiliKeys.ArchiveKey("BV1paused", 2L),
            )

        assertFalse(decision.allowed)
        assertEquals(BilibiliHeartbeatPolicy.SkipReason.SERVER_PAUSED, decision.skipReason)
    }

    @Test
    fun loggedInArchiveAllowsHeartbeat() {
        val decision =
            BilibiliHeartbeatPolicy.decide(
                mode = BilibiliHistorySyncMode.AUTO,
                historyStatus = BilibiliHistoryStatus(isPaused = false, updatedAtMs = 1L),
                isLoggedIn = true,
                csrf = "csrf-value",
                key = BilibiliKeys.ArchiveKey("BV1archive", 3L),
            )

        assertTrue(decision.allowed)
        assertEquals(null, decision.skipReason)
    }

    @Test
    fun liveKeySkipsPlaybackHeartbeat() {
        val decision =
            BilibiliHeartbeatPolicy.decide(
                mode = BilibiliHistorySyncMode.AUTO,
                historyStatus = BilibiliHistoryStatus(isPaused = false, updatedAtMs = 1L),
                isLoggedIn = true,
                csrf = "csrf-value",
                key = BilibiliKeys.LiveKey(roomId = 1000L),
            )

        assertFalse(decision.allowed)
        assertEquals(BilibiliHeartbeatPolicy.SkipReason.LIVE_NOT_SUPPORTED, decision.skipReason)
    }

    @Test
    fun missingArchiveCidSkipsHeartbeat() {
        val decision =
            BilibiliHeartbeatPolicy.decide(
                mode = BilibiliHistorySyncMode.AUTO,
                historyStatus = BilibiliHistoryStatus(isPaused = false, updatedAtMs = 1L),
                isLoggedIn = true,
                csrf = "csrf-value",
                key = BilibiliKeys.ArchiveKey("BV1missing", null),
            )

        assertFalse(decision.allowed)
        assertEquals(BilibiliHeartbeatPolicy.SkipReason.MISSING_ARCHIVE_CID, decision.skipReason)
    }
}
