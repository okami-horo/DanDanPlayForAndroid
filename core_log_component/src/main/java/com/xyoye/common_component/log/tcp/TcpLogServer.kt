package com.xyoye.common_component.log.tcp

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple TCP log server:
 * - Accepts multiple clients
 * - Broadcasts lines (UTF-8 + '\n') to all connected clients
 * - Client IO runs off the caller thread (each client has its own writer thread)
 */
class TcpLogServer(
    private val port: Int,
    private val backlogProvider: () -> List<String>,
    private val broadcastQueueCapacity: Int = DEFAULT_BROADCAST_QUEUE_CAPACITY,
    private val clientQueueCapacity: Int = DEFAULT_CLIENT_QUEUE_CAPACITY,
    private val charset: Charset = Charsets.UTF_8
) {
    private val running = AtomicBoolean(false)
    private val lock = Any()
    private val clients = CopyOnWriteArrayList<ClientConnection>()
    private val broadcastQueue = ArrayBlockingQueue<String>(broadcastQueueCapacity)

    @Volatile
    private var serverSocket: ServerSocket? = null

    private var acceptExecutor: ExecutorService? = null
    private var broadcastExecutor: ExecutorService? = null

    fun start(): Int {
        synchronized(lock) {
            if (running.get()) {
                return boundPort()
            }
            val socket =
                ServerSocket(port).apply {
                    reuseAddress = true
                }
            serverSocket = socket
            running.set(true)

            val accept =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "TcpLogAccept").apply { isDaemon = true }
                }
            accept.execute { acceptLoop(socket) }
            acceptExecutor = accept

            val broadcast =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "TcpLogBroadcast").apply { isDaemon = true }
                }
            broadcast.execute { broadcastLoop() }
            broadcastExecutor = broadcast

            return socket.localPort
        }
    }

    fun stop() {
        synchronized(lock) {
            if (!running.get()) {
                return
            }
            running.set(false)
            runCatching { serverSocket?.close() }
            serverSocket = null

            acceptExecutor?.shutdownNow()
            broadcastExecutor?.shutdownNow()
            acceptExecutor = null
            broadcastExecutor = null
        }

        clients.forEach { it.close() }
        clients.clear()
        broadcastQueue.clear()
    }

    fun isRunning(): Boolean = running.get()

    fun requestedPort(): Int = port

    fun boundPort(): Int = serverSocket?.localPort ?: -1

    fun clientCount(): Int = clients.size

    fun tryOffer(line: String): Boolean {
        if (!running.get()) return false
        return broadcastQueue.offer(line)
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (running.get()) {
            val client =
                runCatching { socket.accept() }.getOrNull()
                    ?: break
            if (!running.get()) {
                runCatching { client.close() }
                break
            }
            runCatching {
                client.tcpNoDelay = true
                client.keepAlive = true
            }
            val connection =
                ClientConnection(
                    socket = client,
                    queueCapacity = clientQueueCapacity,
                    charset = charset,
                    onClosed = { closed -> clients.remove(closed) },
                )
            clients.add(connection)
            connection.start()

            val backlog = runCatching { backlogProvider() }.getOrNull().orEmpty()
            if (backlog.isNotEmpty()) {
                connection.offerAll(backlog)
            }
        }
    }

    private fun broadcastLoop() {
        try {
            while (running.get()) {
                val line = broadcastQueue.take()
                clients.forEach { client ->
                    if (!client.offer(line)) {
                        client.offerDropOldest(line)
                    }
                }
            }
        } catch (_: InterruptedException) {
            // ignore
        }
    }

    private class ClientConnection(
        private val socket: Socket,
        queueCapacity: Int,
        private val charset: Charset,
        private val onClosed: (ClientConnection) -> Unit
    ) {
        private val running = AtomicBoolean(true)
        private val queue = ArrayBlockingQueue<String>(queueCapacity)
        private val executor =
            Executors.newSingleThreadExecutor { runnable ->
                val remote =
                    runCatching { socket.inetAddress?.hostAddress }.getOrNull().orEmpty().ifBlank { "unknown" }
                Thread(runnable, "TcpLogClient-$remote").apply { isDaemon = true }
            }

        fun start() {
            executor.execute { writeLoop() }
        }

        fun offer(line: String): Boolean {
            if (!running.get()) return false
            return queue.offer(line)
        }

        fun offerAll(lines: List<String>) {
            lines.forEach { offerDropOldest(it) }
        }

        fun offerDropOldest(line: String): Boolean {
            if (!running.get()) return false
            if (queue.offer(line)) return true
            queue.poll()
            return queue.offer(line)
        }

        private fun writeLoop() {
            try {
                val output =
                    runCatching { socket.getOutputStream() }.getOrNull()
                        ?: return
                val writer = BufferedWriter(OutputStreamWriter(output, charset))
                while (running.get()) {
                    val line = queue.take()
                    writer.write(line)
                    writer.write('\n'.code)
                    writer.flush()
                }
            } catch (_: InterruptedException) {
                // ignore
            } catch (_: Exception) {
                // ignore
            } finally {
                close()
            }
        }

        fun close() {
            if (!running.getAndSet(false)) return
            runCatching { socket.close() }
            executor.shutdownNow()
            onClosed(this)
        }
    }

    companion object {
        private const val DEFAULT_BROADCAST_QUEUE_CAPACITY = 512
        private const val DEFAULT_CLIENT_QUEUE_CAPACITY = 256
    }
}
