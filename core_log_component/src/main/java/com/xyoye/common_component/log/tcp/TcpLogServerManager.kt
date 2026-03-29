package com.xyoye.common_component.log.tcp

import com.xyoye.common_component.config.LogConfig

internal interface TcpLogServerConfig {
    fun isTcpLogServerEnabled(): Boolean

    fun putTcpLogServerEnabled(enabled: Boolean)

    fun getTcpLogServerPort(): Int

    fun putTcpLogServerPort(port: Int)

    fun isDebugSessionEnabled(): Boolean
}

private object MmkvTcpLogServerConfig : TcpLogServerConfig {
    override fun isTcpLogServerEnabled(): Boolean = LogConfig.isTcpLogServerEnabled()

    override fun putTcpLogServerEnabled(enabled: Boolean) {
        LogConfig.putTcpLogServerEnabled(enabled)
    }

    override fun getTcpLogServerPort(): Int = LogConfig.getTcpLogServerPort()

    override fun putTcpLogServerPort(port: Int) {
        LogConfig.putTcpLogServerPort(port)
    }

    override fun isDebugSessionEnabled(): Boolean = LogConfig.isDebugSessionEnabled()
}

/**
 * Runtime manager for [TcpLogServer].
 *
 * - Reads/writes persisted config via [LogConfig]
 * - Owns the running server instance
 * - Keeps an in-memory ring buffer for new connections
 */
object TcpLogServerManager {
    const val DEFAULT_PORT = 17010

    private const val DEFAULT_RING_BUFFER_SIZE = 200
    private const val DEBUG_SESSION_REQUIRED_ERROR = "TCP 日志仅在调试会话中可用"

    internal var config: TcpLogServerConfig = MmkvTcpLogServerConfig

    private val lock = Any()
    private val bufferLock = Any()
    private val ringBuffer = ArrayDeque<String>(DEFAULT_RING_BUFFER_SIZE)

    @Volatile
    private var server: TcpLogServer? = null

    @Volatile
    private var lastError: String? = null

    fun applyFromStorage(): TcpLogServerState {
        val enabled = config.isTcpLogServerEnabled()
        val debugSessionEnabled = config.isDebugSessionEnabled()
        val storedPort = config.getTcpLogServerPort()
        val normalizedPort = normalizePort(storedPort)
        if (storedPort != normalizedPort) {
            config.putTcpLogServerPort(normalizedPort)
        }
        if (!debugSessionEnabled) {
            return if (enabled) {
                lastError = DEBUG_SESSION_REQUIRED_ERROR
                stop(clearError = false)
            } else {
                stop(clearError = true)
            }
        }
        lastError = null
        return if (enabled) {
            start(normalizedPort)
        } else {
            stop(clearError = true)
        }
    }

    fun setEnabled(
        enabled: Boolean,
        port: Int = config.getTcpLogServerPort()
    ): TcpLogServerState {
        val normalizedPort = normalizePort(port)
        config.putTcpLogServerEnabled(enabled)
        config.putTcpLogServerPort(normalizedPort)
        val debugSessionEnabled = config.isDebugSessionEnabled()
        if (enabled && !debugSessionEnabled) {
            lastError = DEBUG_SESSION_REQUIRED_ERROR
            return stop(clearError = false)
        }
        lastError = null
        return if (enabled) {
            start(normalizedPort)
        } else {
            stop(clearError = true)
        }
    }

    fun snapshot(): TcpLogServerState {
        val enabled = config.isTcpLogServerEnabled()
        val requestedPort = normalizePort(config.getTcpLogServerPort())
        val current = server
        val running = current?.isRunning() == true
        val boundPort = if (running) current?.boundPort() ?: -1 else -1
        val clientCount = if (running) current?.clientCount() ?: 0 else 0
        return TcpLogServerState(
            enabled = enabled,
            running = running,
            requestedPort = requestedPort,
            boundPort = boundPort,
            clientCount = clientCount,
            lastError = lastError,
        )
    }

    fun isRunning(): Boolean = server?.isRunning() == true

    fun tryEmit(line: String) {
        val current = server ?: return
        if (!current.isRunning()) return
        appendRingBuffer(line)
        current.tryOffer(line)
    }

    private fun start(port: Int): TcpLogServerState {
        synchronized(lock) {
            lastError = null
            val current = server
            if (current != null && current.isRunning()) {
                val currentPort = current.requestedPort()
                if (currentPort == port) {
                    return snapshot()
                }
                runCatching { current.stop() }
                server = null
            }

            val newServer =
                TcpLogServer(
                    port = port,
                    backlogProvider = { snapshotRingBuffer() },
                )
            runCatching { newServer.start() }
                .onSuccess {
                    server = newServer
                }.onFailure { error ->
                    lastError = error.message ?: error::class.java.simpleName
                    config.putTcpLogServerEnabled(false)
                    runCatching { newServer.stop() }
                    server = null
                }
            return snapshot()
        }
    }

    private fun stop(clearError: Boolean): TcpLogServerState {
        synchronized(lock) {
            val current = server
            if (current != null) {
                runCatching { current.stop() }
                server = null
            }
            if (clearError) {
                lastError = null
            }
            return snapshot()
        }
    }

    private fun appendRingBuffer(line: String) {
        synchronized(bufferLock) {
            if (ringBuffer.size >= DEFAULT_RING_BUFFER_SIZE) {
                ringBuffer.removeFirst()
            }
            ringBuffer.addLast(line)
        }
    }

    private fun snapshotRingBuffer(): List<String> {
        synchronized(bufferLock) {
            return ringBuffer.toList()
        }
    }

    private fun normalizePort(port: Int): Int {
        if (port == 0) return 0
        if (port in 1..65535) return port
        return DEFAULT_PORT
    }

    internal fun resetForTests() {
        synchronized(lock) {
            val current = server
            if (current != null) {
                runCatching { current.stop() }
                server = null
            }
            lastError = null
            config = MmkvTcpLogServerConfig
        }
        synchronized(bufferLock) {
            ringBuffer.clear()
        }
    }
}
