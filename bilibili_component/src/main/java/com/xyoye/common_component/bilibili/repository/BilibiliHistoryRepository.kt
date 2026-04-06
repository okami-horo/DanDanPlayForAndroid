package com.xyoye.common_component.bilibili.repository

import com.xyoye.common_component.bilibili.history.BilibiliHistoryStatus
import com.xyoye.data_component.data.bilibili.BilibiliHistoryCursorData

class BilibiliHistoryRepository internal constructor(
    private val core: BilibiliRepositoryCore
) {
    suspend fun historyCursor(
        max: Long? = null,
        viewAt: Long? = null,
        business: String? = null,
        ps: Int = 30,
        type: String = "archive",
        preferCache: Boolean = true
    ): Result<BilibiliHistoryCursorData> =
        core.historyCursor(
            max = max,
            viewAt = viewAt,
            business = business,
            ps = ps,
            type = type,
            preferCache = preferCache,
        )

    suspend fun historyStatus(forceRefresh: Boolean = false): Result<BilibiliHistoryStatus?> =
        core.historyStatus(forceRefresh)

    fun cachedHistoryStatusOrNull(maxAgeMs: Long? = null): BilibiliHistoryStatus? =
        core.cachedHistoryStatusOrNull(maxAgeMs)
}
