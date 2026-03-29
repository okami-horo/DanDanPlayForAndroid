package com.xyoye.common_component.log.tcp

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TcpLogServerManagerTest {
    private lateinit var inMemoryConfig: InMemoryTcpLogServerConfig

    @Before
    fun setUp() {
        TcpLogServerManager.resetForTests()
        inMemoryConfig =
            InMemoryTcpLogServerConfig(
                tcpLogServerEnabled = false,
                storedPort = 0,
                debugSessionEnabled = false,
            )
        TcpLogServerManager.config = inMemoryConfig
    }

    @After
    fun tearDown() {
        TcpLogServerManager.resetForTests()
    }

    @Test
    fun enableWithoutDebugSessionKeepsReason() {
        val state = TcpLogServerManager.setEnabled(enabled = true, port = 0)

        assertTrue(state.enabled)
        assertFalse(state.running)
        assertEquals("TCP 日志仅在调试会话中可用", state.lastError)

        val snapshot = TcpLogServerManager.snapshot()
        assertEquals("TCP 日志仅在调试会话中可用", snapshot.lastError)
    }

    @Test
    fun applyFromStorageStartsServerAfterDebugSessionEnabled() {
        TcpLogServerManager.setEnabled(enabled = true, port = 0)

        inMemoryConfig.debugSessionEnabled = true
        val state = TcpLogServerManager.applyFromStorage()

        assertTrue(state.enabled)
        assertTrue(state.running)
        assertTrue(state.boundPort > 0)
        assertNull(state.lastError)
    }

    @Test
    fun disableClearsLastError() {
        TcpLogServerManager.setEnabled(enabled = true, port = 0)

        val disabled = TcpLogServerManager.setEnabled(enabled = false, port = 0)

        assertFalse(disabled.enabled)
        assertFalse(disabled.running)
        assertNull(disabled.lastError)
    }
}

private class InMemoryTcpLogServerConfig(
    var tcpLogServerEnabled: Boolean,
    var storedPort: Int,
    var debugSessionEnabled: Boolean,
) : TcpLogServerConfig {
    override fun isTcpLogServerEnabled(): Boolean = tcpLogServerEnabled

    override fun putTcpLogServerEnabled(enabled: Boolean) {
        tcpLogServerEnabled = enabled
    }

    override fun getTcpLogServerPort(): Int = storedPort

    override fun putTcpLogServerPort(port: Int) {
        storedPort = port
    }

    override fun isDebugSessionEnabled(): Boolean = debugSessionEnabled
}
