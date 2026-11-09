package com.etashe.tunnelvpn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkParserTest {

    @Test
    fun `parses a basic vless ws tls link`() {
        val link = "vless://11111111-2222-3333-4444-555555555555@example.com:443" +
            "?encryption=none&security=tls&type=ws&path=%2Fetashe&host=example.com&sni=example.com#eTaShe%20Server%201"

        val profile = LinkParser.parse(link)

        assertEquals(EngineType.XRAY, profile.engine)
        assertEquals(XrayProtocol.VLESS, profile.protocol)
        assertEquals("example.com", profile.address)
        assertEquals(443, profile.port)
        assertEquals("11111111-2222-3333-4444-555555555555", profile.uuid)
        assertEquals(XrayNetwork.WS, profile.network)
        assertEquals("/etashe", profile.path)
        assertTrue(profile.tlsEnabled)
        assertEquals("eTaShe Server 1", profile.remark)
    }

    @Test
    fun `parses a vmess base64 json link`() {
        val json = """{"v":"2","ps":"eTaShe VMess","add":"vm.example.com","port":"8443",
            "id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","aid":"0","net":"ws","type":"none",
            "host":"vm.example.com","path":"/ray","tls":"tls","sni":"vm.example.com"}"""
        val b64 = java.util.Base64.getEncoder().encodeToString(json.toByteArray())
        val link = "vmess://$b64"

        val profile = LinkParser.parse(link)

        assertEquals(XrayProtocol.VMESS, profile.protocol)
        assertEquals("vm.example.com", profile.address)
        assertEquals(8443, profile.port)
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", profile.uuid)
        assertEquals(XrayNetwork.WS, profile.network)
        assertTrue(profile.tlsEnabled)
        assertEquals("eTaShe VMess", profile.remark)
    }

    @Test
    fun `parses a trojan link`() {
        val link = "trojan://mypassword@tr.example.com:443?security=tls&sni=tr.example.com#Trojan%20eTaShe"
        val profile = LinkParser.parse(link)

        assertEquals(XrayProtocol.TROJAN, profile.protocol)
        assertEquals("tr.example.com", profile.address)
        assertEquals("mypassword", profile.password)
        assertTrue(profile.tlsEnabled)
        assertEquals("Trojan eTaShe", profile.remark)
    }

    @Test
    fun `parses a shadowsocks link with encoded userinfo`() {
        val userInfo = java.util.Base64.getEncoder().encodeToString("aes-256-gcm:supersecret".toByteArray())
        val link = "ss://$userInfo@ss.example.com:8388#SS%20eTaShe"
        val profile = LinkParser.parse(link)

        assertEquals(XrayProtocol.SHADOWSOCKS, profile.protocol)
        assertEquals("ss.example.com", profile.address)
        assertEquals(8388, profile.port)
        assertEquals("aes-256-gcm", profile.ssMethod)
        assertEquals("supersecret", profile.password)
        assertEquals("SS eTaShe", profile.remark)
    }

    @Test
    fun `parses a fully-encoded shadowsocks link`() {
        val whole = java.util.Base64.getEncoder().encodeToString("chacha20-ietf-poly1305:pw123@ss2.example.com:9000".toByteArray())
        val link = "ss://$whole#SS2"
        val profile = LinkParser.parse(link)

        assertEquals("ss2.example.com", profile.address)
        assertEquals(9000, profile.port)
        assertEquals("chacha20-ietf-poly1305", profile.ssMethod)
        assertEquals("pw123", profile.password)
    }

    @Test(expected = LinkParseException::class)
    fun `rejects unsupported scheme`() {
        LinkParser.parse("http://not-a-tunnel-link.com")
    }

    @Test(expected = LinkParseException::class)
    fun `rejects vless link without address`() {
        LinkParser.parse("vless://uuid-only@:443?type=tcp")
    }

    @Test
    fun `isSupportedLink recognizes all four schemes`() {
        assertTrue(LinkParser.isSupportedLink("vless://a@b:1"))
        assertTrue(LinkParser.isSupportedLink("vmess://abc"))
        assertTrue(LinkParser.isSupportedLink("trojan://a@b:1"))
        assertTrue(LinkParser.isSupportedLink("ss://abc"))
        assertFalse(LinkParser.isSupportedLink("https://example.com"))
    }
}
