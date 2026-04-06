package com.xyoye.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BilibiliCompletionTrustTest {
    @Test
    fun shouldTreatLiveCompletionAsUnexpected() {
        val context =
            BilibiliCompletionContext(
                isLive = true,
                positionMs = 5 * 60_000L,
                durationMs = 30 * 60_000L,
            )

        assertTrue(BilibiliCompletionTrust.isUnexpectedCompletion(context))
    }

    @Test
    fun shouldTreatLongVideoCompletionOutsideTailWindowAsUnexpected() {
        val context =
            BilibiliCompletionContext(
                isLive = false,
                positionMs = 11 * 60_000L,
                durationMs = 12 * 60_000L,
            )

        assertTrue(BilibiliCompletionTrust.isUnexpectedCompletion(context))
    }

    @Test
    fun shouldTreatLongVideoCompletionInsideTailWindowAsTrusted() {
        val context =
            BilibiliCompletionContext(
                isLive = false,
                positionMs = BilibiliCompletionTrust.LONG_VIDEO_MIN_DURATION_MS,
                durationMs = BilibiliCompletionTrust.LONG_VIDEO_MIN_DURATION_MS +
                    BilibiliCompletionTrust.COMPLETION_TRUST_WINDOW_MS,
            )

        assertFalse(BilibiliCompletionTrust.isUnexpectedCompletion(context))
    }

    @Test
    fun shouldKeepShortVideoCompletionBehaviorUnchanged() {
        val context =
            BilibiliCompletionContext(
                isLive = false,
                positionMs = 1_000L,
                durationMs = 5 * 60_000L,
            )

        assertFalse(BilibiliCompletionTrust.isUnexpectedCompletion(context))
    }
}
