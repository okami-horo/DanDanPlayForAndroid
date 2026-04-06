package com.xyoye.common_component.bilibili

import org.junit.Assert.assertEquals
import org.junit.Test

class BilibiliHistorySyncPreferencesStoreTest {
    @Test
    fun missingLegacyKeyMigratesToAuto() {
        val mode = BilibiliHistorySyncPreferencesStore.resolveModeFromLegacy(null)

        assertEquals(BilibiliHistorySyncMode.AUTO, mode)
    }

    @Test
    fun explicitLegacyFalseMigratesToDisabled() {
        val mode = BilibiliHistorySyncPreferencesStore.resolveModeFromLegacy(false)

        assertEquals(BilibiliHistorySyncMode.DISABLED, mode)
    }

    @Test
    fun explicitLegacyTrueMigratesToAuto() {
        val mode = BilibiliHistorySyncPreferencesStore.resolveModeFromLegacy(true)

        assertEquals(BilibiliHistorySyncMode.AUTO, mode)
    }
}
