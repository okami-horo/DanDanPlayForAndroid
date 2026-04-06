package com.xyoye.player

internal data class BilibiliCompletionContext(
    val isLive: Boolean,
    val positionMs: Long,
    val durationMs: Long,
)

internal object BilibiliCompletionTrust {
    const val LONG_VIDEO_MIN_DURATION_MS = 10 * 60_000L
    const val COMPLETION_TRUST_WINDOW_MS = 30_000L

    fun isUnexpectedCompletion(context: BilibiliCompletionContext): Boolean {
        if (context.isLive) {
            return true
        }

        val durationMs = context.durationMs.coerceAtLeast(0L)
        if (durationMs <= 0L || durationMs < LONG_VIDEO_MIN_DURATION_MS) {
            return false
        }

        val remainingMs = (durationMs - context.positionMs.coerceAtLeast(0L)).coerceAtLeast(0L)
        return remainingMs > COMPLETION_TRUST_WINDOW_MS
    }
}
