package com.etashe.tunnelvpn.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds a valid Xray-core JSON config from a [ConfigProfile] (VLESS / VMess / Trojan / Shadowsocks).
 *
 * Architecture (same pattern v2rayNG / NetMod-style clients use):
 *   VpnService TUN fd -> tun2socks -> local SOCKS5 inbound (127.0.0.1:socksPort) -> Xray-core
 *   -> configured outbound (vless/vmess/trojan/ss) -> remote server.
 *
 * This class only builds the JSON text Xray-core consumes; it does not talk to the
 * network itself — that happens inside the native Xray-core library at runtime.
 */
object XrayConfigBuilder {

    fun build(profile: ConfigProfile, socksPort: Int = 10808): String {
        require(profile.engine == EngineType.XRAY) { "XrayConfigBuilder only supports XRAY-engine profiles" }
        require(profile.protocol != null) { "Profile is missing an Xray protocol" }

        val root = JSONObject()
        root.put("log", JSONObject().put("loglevel", "warning"))

        root.put("inbounds", JSONArray().put(buildSocksInbound(socksPort)))
        root.put("outbounds", JSONArray().apply {
            put(buildOutbound(profile))
            put(JSONObject().put("protocol", "freedom").put("tag", "direct"))
            put(JSONObject().put("protocol", "blackhole").put("tag", "block"))
        })

        root.put("routing", JSONObject().apply {
            put("domainStrategy", "AsIs")
            put("rules", JSONArray())
        })

        return root.toString()
    }

    private fun buildSocksInbound(port: Int): JSONObject = JSONObject().apply {
        put("tag", "socks-in")
        put("listen", "127.0.0.1")
        put("port", port)
        put("protocol", "socks")
        put("settings", JSONObject().apply {
            put("udp", true)
            put("auth", "noauth")
        })
        put("sniffing", JSONObject().apply {
            put("enabled", true)
            put("destOverride", JSONArray().put("http").put("tls"))
        })
    }

    private fun buildOutbound(p: ConfigProfile): JSONObject {
        val outbound = JSONObject()
        outbound.put("tag", "proxy")

        when (p.protocol) {
            XrayProtocol.VLESS -> {
                outbound.put("protocol", "vless")
                outbound.put("settings", JSONObject().apply {
                    put("vnext", JSONArray().put(JSONObject().apply {
                        put("address", p.address)
                        put("port", p.port)
                        put("users", JSONArray().put(JSONObject().apply {
                            put("id", p.uuid)
                            put("encryption", "none")
                            if (p.flow.isNotBlank()) put("flow", p.flow)
                        }))
                    }))
                })
            }

            XrayProtocol.VMESS -> {
                outbound.put("protocol", "vmess")
                outbound.put("settings", JSONObject().apply {
                    put("vnext", JSONArray().put(JSONObject().apply {
                        put("address", p.address)
                        put("port", p.port)
                        put("users", JSONArray().put(JSONObject().apply {
                            put("id", p.uuid)
                            put("alterId", p.alterId)
                            put("security", p.vmessSecurity.ifBlank { "auto" })
                        }))
                    }))
                })
            }

            XrayProtocol.TROJAN -> {
                outbound.put("protocol", "trojan")
                outbound.put("settings", JSONObject().apply {
                    put("servers", JSONArray().put(JSONObject().apply {
                        put("address", p.address)
                        put("port", p.port)
                        put("password", p.password)
                    }))
                })
            }

            XrayProtocol.SHADOWSOCKS -> {
                outbound.put("protocol", "shadowsocks")
                outbound.put("settings", JSONObject().apply {
                    put("servers", JSONArray().put(JSONObject().apply {
                        put("address", p.address)
                        put("port", p.port)
                        put("method", p.ssMethod.ifBlank { "aes-256-gcm" })
                        put("password", p.password)
                    }))
                })
            }

            null -> throw IllegalArgumentException("Missing protocol")
        }

        outbound.put("streamSettings", buildStreamSettings(p))
        return outbound
    }

    private fun buildStreamSettings(p: ConfigProfile): JSONObject {
        val stream = JSONObject()
        stream.put("network", p.network.wire)

        if (p.tlsEnabled) {
            stream.put("security", "tls")
            stream.put("tlsSettings", JSONObject().apply {
                put("serverName", p.sni.ifBlank { p.address })
                put("allowInsecure", p.allowInsecure)
                if (p.alpn.isNotBlank()) {
                    put("alpn", JSONArray().apply { p.alpn.split(",").forEach { put(it.trim()) } })
                }
            })
        } else {
            stream.put("security", "none")
        }

        when (p.network) {
            XrayNetwork.WS -> stream.put("wsSettings", JSONObject().apply {
                put("path", p.path.ifBlank { "/" })
                if (p.host.isNotBlank()) put("headers", JSONObject().put("Host", p.host))
            })

            XrayNetwork.GRPC -> stream.put("grpcSettings", JSONObject().apply {
                put("serviceName", p.serviceName.ifBlank { p.path })
            })

            XrayNetwork.HTTP_UPGRADE -> stream.put("httpupgradeSettings", JSONObject().apply {
                put("path", p.path.ifBlank { "/" })
                if (p.host.isNotBlank()) put("host", p.host)
            })

            XrayNetwork.TCP -> {
                // plain tcp — nothing extra needed unless an HTTP camouflage header is used,
                // which eTaShe doesn't currently expose in ConfigProfile.
            }

            XrayNetwork.KCP, XrayNetwork.QUIC -> {
                // left at Xray-core defaults; not currently configurable from ConfigProfile.
            }
        }

        return stream
    }
}
