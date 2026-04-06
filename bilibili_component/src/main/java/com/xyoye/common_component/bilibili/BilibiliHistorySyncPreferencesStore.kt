package com.xyoye.common_component.bilibili

import com.tencent.mmkv.MMKV
import com.xyoye.data_component.entity.MediaLibraryEntity

enum class BilibiliHistorySyncMode(
    val label: String
) {
    AUTO("自动"),
    DISABLED("关闭")
}

/**
 * Bilibili 历史同步偏好（按登录会话 storageKey 隔离）。
 *
 * 语义：
 * - AUTO：由程序根据登录态、服务端历史暂停状态和内容类型自动决定是否上报播放心跳
 * - DISABLED：仅关闭当前连接/会话在本客户端的历史同步
 */
object BilibiliHistorySyncPreferencesStore {
    private const val MMKV_ID = "bilibili_history_sync_preferences"
    private const val KEY_MODE = "history_sync_mode"

    fun read(library: MediaLibraryEntity): BilibiliHistorySyncMode = read(BilibiliPlaybackPreferencesStore.storageKey(library))

    fun write(
        library: MediaLibraryEntity,
        mode: BilibiliHistorySyncMode
    ) = write(BilibiliPlaybackPreferencesStore.storageKey(library), mode)

    fun clear(library: MediaLibraryEntity) = clear(BilibiliPlaybackPreferencesStore.storageKey(library))

    fun read(storageKey: String): BilibiliHistorySyncMode {
        val kv = mmkv()
        runCatching {
            kv.decodeString(namespacedKey(storageKey))?.let { BilibiliHistorySyncMode.valueOf(it) }
        }.getOrNull()?.let {
            return it
        }

        val migrated = resolveModeFromLegacy(BilibiliPlaybackPreferencesStore.readLegacyHeartbeatReportOrNull(storageKey))
        write(storageKey, migrated)
        return migrated
    }

    internal fun resolveModeFromLegacy(legacyHeartbeatReport: Boolean?): BilibiliHistorySyncMode =
        when (legacyHeartbeatReport) {
            false -> BilibiliHistorySyncMode.DISABLED
            else -> BilibiliHistorySyncMode.AUTO
        }

    fun write(
        storageKey: String,
        mode: BilibiliHistorySyncMode
    ) {
        val kv = mmkv()
        kv.encode(namespacedKey(storageKey), mode.name)
        BilibiliPlaybackPreferencesStore.clearLegacyHeartbeatReport(storageKey)
    }

    fun clear(storageKey: String) {
        val kv = mmkv()
        kv.removeValueForKey(namespacedKey(storageKey))
        BilibiliPlaybackPreferencesStore.clearLegacyHeartbeatReport(storageKey)
    }

    private fun mmkv(): MMKV = MMKV.mmkvWithID(MMKV_ID)

    private fun namespacedKey(storageKey: String): String = "$storageKey.$KEY_MODE"
}
