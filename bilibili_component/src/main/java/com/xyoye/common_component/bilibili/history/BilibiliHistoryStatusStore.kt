package com.xyoye.common_component.bilibili.history

import com.tencent.mmkv.MMKV
import com.xyoye.common_component.extension.toMd5String

data class BilibiliHistoryStatus(
    val isPaused: Boolean,
    val updatedAtMs: Long
)

/**
 * Bilibili 服务端“历史暂停状态”缓存（按 storageKey 隔离）。
 */
class BilibiliHistoryStatusStore(
    storageKey: String
) {
    private val kv: MMKV =
        MMKV.mmkvWithID("bilibili_history_status_${storageKey.toMd5String()}")

    fun readLatestOrNull(): BilibiliHistoryStatus? {
        val updatedAt = kv.decodeLong(KEY_UPDATED_AT, 0L)
        if (updatedAt <= 0L) {
            return null
        }
        return BilibiliHistoryStatus(
            isPaused = kv.decodeBool(KEY_PAUSED, false),
            updatedAtMs = updatedAt,
        )
    }

    fun readFreshOrNull(
        maxAgeMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): BilibiliHistoryStatus? =
        readLatestOrNull()?.takeIf { nowMs - it.updatedAtMs <= maxAgeMs }

    fun write(
        isPaused: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): BilibiliHistoryStatus {
        kv.encode(KEY_PAUSED, isPaused)
        kv.encode(KEY_UPDATED_AT, nowMs)
        return BilibiliHistoryStatus(
            isPaused = isPaused,
            updatedAtMs = nowMs,
        )
    }

    fun clear() {
        kv.clearAll()
    }

    private companion object {
        private const val KEY_PAUSED = "paused"
        private const val KEY_UPDATED_AT = "updated_at"
    }
}
