package com.etashe.tunnelvpn.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XrayConfigBuilderTest {

    @Test
    fun `builds valid vless ws tls config`() {
        val profile = LinkParser.parse(
            "vless://11111111-2222-3333-4444-555555555555@example.com:443" +
                "?encryption=none&security=tls&type=ws&path=%2Fetashe&host=example.com&sni=example.com#eTaShe"
        )

        val json = JSONObject(XrayConfigBuilder.build(profile, socksPort = 10900))

        val inbound = json.getJSONArray("inbounds").getJSONObject(0)
        assertEquals(10900, inbound.getInt("port"))
        assertEquals("socks", inbound.getString("protocol"))

        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        assertEquals("vless", outbound.getString("protocol"))
        val vnext = outbound.getJSONObject("settings").getJSONArray("vnext").getJSONObject(0)
        assertEquals("example.com", vnext.getString("address"))
        assertEquals(443, vnext.getInt("port"))
        val user = vnext.getJSONArray("users").getJSONObject(0)
        assertEquals("11111111-2222-3333-4444-555555555555", user.getString("id"))

        val stream = outbound.getJSONObject("streamSettings")
        assertEquals("ws", stream.getString("network"))
        assertEquals("tls", stream.getString("security"))
        assertEquals("/etashe", stream.getJSONObject("wsSettings").getString("path"))
    }

    @Test
    fun `builds valid trojan tcp tls config`() {
        val profile = LinkParser.parse("trojan://mypassword@tr.example.com:443?security=tls&sni=tr.example.com#T")
        val json = JSONObject(XrayConfigBuilder.build(profile))

        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        assertEquals("trojan", outbound.getString("protocol"))
        val server = outbound.getJSONObject("settings").getJSONArray("servers").getJSONObject(0)
        assertEquals("mypassword", server.getString("password"))
        assertEquals("tls", outbound.getJSONObject("streamSettings").getString("security"))
    }

    @Test
    fun `builds valid shadowsocks config without tls`() {
        val userInfo = java.util.Base64.getEncoder().encodeToString("aes-256-gcm:supersecret".toByteArray())
        val profile = LinkParser.parse("ss://$userInfo@ss.example.com:8388#SS")
        val json = JSONObject(XrayConfigBuilder.build(profile))

        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        assertEquals("shadowsocks", outbound.getString("protocol"))
        val server = outbound.getJSONObject("settings").getJSONArray("servers").getJSONObject(0)
        assertEquals("aes-256-gcm", server.getString("method"))
        assertEquals("none", outbound.getJSONObject("streamSettings").getString("security"))
    }

    @Test
    fun `builds valid vmess grpc config`() {
        val json = """{"v":"2","ps":"V","add":"vm.example.com","port":"8443",
            "id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","aid":"0","net":"grpc","type":"none",
            "host":"vm.example.com","path":"etashe-service","tls":"tls","sni":"vm.example.com"}"""
        val b64 = java.util.Base64.getEncoder().encodeToString(json.toByteArray())
        val profile = LinkParser.parse("vmess://$b64")

        val config = JSONObject(XrayConfigBuilder.build(profile))
        val outbound = config.getJSONArray("outbounds").getJSONObject(0)
        assertEquals("vmess", outbound.getString("protocol"))
        val stream = outbound.getJSONObject("streamSettings")
        assertEquals("grpc", stream.getString("network"))
        assertTrue(stream.getJSONObject("grpcSettings").getString("serviceName").isNotBlank())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non-xray engine profiles`() {
        val sshProfile = ConfigProfile(engine = EngineType.SSH_PAYLOAD, address = "1.2.3.4", port = 22)
        XrayConfigBuilder.build(sshProfile)
    }
}
