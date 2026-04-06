package com.xyoye.common_component.bilibili.playback

import com.xyoye.common_component.bilibili.BilibiliHistorySyncMode
import com.xyoye.common_component.bilibili.BilibiliHistorySyncPreferencesStore
import com.xyoye.common_component.bilibili.BilibiliKeys
import com.xyoye.common_component.bilibili.auth.BilibiliAuthStore
import com.xyoye.common_component.bilibili.history.BilibiliHistoryStatus
import com.xyoye.common_component.bilibili.repository.BilibiliRepository

object BilibiliHeartbeatPolicy {
    enum class SkipReason(
        val logLabel: String
    ) {
        LOCAL_DISABLED("local_disabled"),
        SERVER_PAUSED("server_paused"),
        UNAUTHENTICATED("unauthenticated"),
        MISSING_CSRF("missing_csrf"),
        MISSING_ARCHIVE_CID("missing_archive_cid"),
        LIVE_NOT_SUPPORTED("live_not_supported"),
        PGC_SEASON_NOT_SUPPORTED("pgc_season_not_supported")
    }

    data class Decision(
        val allowed: Boolean,
        val skipReason: SkipReason? = null
    )

    suspend fun decide(
        storageKey: String,
        key: BilibiliKeys.Key,
        repository: BilibiliRepository
    ): Decision =
        decide(
            mode = BilibiliHistorySyncPreferencesStore.read(storageKey),
            historyStatus = repository.history.historyStatus(forceRefresh = false).getOrNull(),
            isLoggedIn = repository.isLoggedIn(),
            csrf = BilibiliAuthStore.read(storageKey).csrf,
            key = key,
        )

    internal fun decide(
        mode: BilibiliHistorySyncMode,
        historyStatus: BilibiliHistoryStatus?,
        isLoggedIn: Boolean,
        csrf: String?,
        key: BilibiliKeys.Key
    ): Decision {
        if (mode == BilibiliHistorySyncMode.DISABLED) {
            return Decision(allowed = false, skipReason = SkipReason.LOCAL_DISABLED)
        }

        if (historyStatus?.isPaused == true) {
            return Decision(allowed = false, skipReason = SkipReason.SERVER_PAUSED)
        }

        if (!isLoggedIn) {
            return Decision(allowed = false, skipReason = SkipReason.UNAUTHENTICATED)
        }

        if (csrf.isNullOrBlank()) {
            return Decision(allowed = false, skipReason = SkipReason.MISSING_CSRF)
        }

        return when (key) {
            is BilibiliKeys.ArchiveKey -> {
                if (key.cid == null) {
                    Decision(allowed = false, skipReason = SkipReason.MISSING_ARCHIVE_CID)
                } else {
                    Decision(allowed = true)
                }
            }

            is BilibiliKeys.PgcEpisodeKey -> Decision(allowed = true)
            is BilibiliKeys.LiveKey -> Decision(allowed = false, skipReason = SkipReason.LIVE_NOT_SUPPORTED)
            is BilibiliKeys.PgcSeasonKey -> Decision(allowed = false, skipReason = SkipReason.PGC_SEASON_NOT_SUPPORTED)
        }
    }
}
