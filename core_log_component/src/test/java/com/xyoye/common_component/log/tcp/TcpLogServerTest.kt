package com.xyoye.common_component.log.tcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class TcpLogServerTest {
    @Test
    fun broadcastToMultipleClients() {
        val server =
            TcpLogServer(
                port = 0,
                backlogProvider = { emptyList() },
            )
        val port = server.start()

        val client1 = Socket("127.0.0.1", port).apply { soTimeout = 2000 }
        val client2 = Socket("127.0.0.1", port).apply { soTimeout = 2000 }
        waitUntil { server.clientCount() >= 2 }

        val reader1 = BufferedReader(InputStreamReader(client1.getInputStream(), Charsets.UTF_8))
        val reader2 = BufferedReader(InputStreamReader(client2.getInputStream(), Charsets.UTF_8))

        assertTrue(server.tryOffer("hello"))
        assertEquals("hello", reader1.readLine())
        assertEquals("hello", reader2.readLine())

        client1.close()

        assertTrue(server.tryOffer("world"))
        assertEquals("world", reader2.readLine())

        client2.close()
        server.stop()
    }

    @Test
    fun newClientReceivesBacklog() {
        val server =
            TcpLogServer(
                port = 0,
                backlogProvider = { listOf("a", "b") },
            )
        val port = server.start()

        val client = Socket("127.0.0.1", port).apply { soTimeout = 2000 }
        waitUntil { server.clientCount() >= 1 }

        val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
        assertEquals("a", reader.readLine())
        assertEquals("b", reader.readLine())

        client.close()
        server.stop()
    }

    private fun waitUntil(
        timeoutMs: Long = 1500,
        block: () -> Boolean
    ) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (block()) return
            Thread.sleep(20)
        }
        throw AssertionError("condition not met in ${timeoutMs}ms")
    }
}
