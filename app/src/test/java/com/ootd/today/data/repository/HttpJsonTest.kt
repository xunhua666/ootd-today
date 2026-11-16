package com.ootd.today.data.repository

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [HttpJson] 的传输安全约束。
 *
 * 这些用例保证「用户数据只走 HTTPS」不是一句注释，而是有回归保护的行为：
 * 远端明文 HTTP 与其它协议都会被直接拒绝，根本不会发起连接。
 */
class HttpJsonTest {

    @Test
    fun `https is allowed`() {
        assertTrue(HttpJson.isAllowed(URI("https://api.example.com/v1/chat")))
    }

    @Test
    fun `loopback http is allowed for local debugging`() {
        assertTrue(HttpJson.isAllowed(URI("http://localhost:8080/ping")))
        assertTrue(HttpJson.isAllowed(URI("http://127.0.0.1:8080/ping")))
    }

    @Test
    fun `remote plain http is rejected`() {
        assertFalse(HttpJson.isAllowed(URI("http://api.example.com/v1/chat")))
        assertFalse(HttpJson.isAllowed(URI("http://10.0.2.2:8080/ping")))
    }

    @Test
    fun `non http schemes are rejected`() {
        assertFalse(HttpJson.isAllowed(URI("ftp://example.com/file")))
        assertFalse(HttpJson.isAllowed(URI("file:///etc/passwd")))
        assertFalse(HttpJson.isAllowed(URI("javascript:alert(1)")))
    }

    @Test
    fun `requests to remote plain http are never sent`() {
        assertNull(HttpJson.get("http://example.com/a"))
        assertNull(HttpJson.postJson("http://example.com/a", "{}"))
    }

    @Test
    fun `malformed urls return null instead of crashing`() {
        assertNull(HttpJson.get("not a url"))
        assertNull(HttpJson.get(""))
    }

    @Test
    fun `loopback http request works end to end`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/ping") { exchange ->
            val body = "pong".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val port = server.address.port
            assertEquals("pong", HttpJson.get("http://127.0.0.1:$port/ping"))
        } finally {
            server.stop(0)
        }
    }
}