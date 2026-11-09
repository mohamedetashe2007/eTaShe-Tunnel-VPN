package com.etashe.tunnelvpn.core

import java.util.UUID

/**
 * Which tunneling engine a profile belongs to.
 * eTaShe Tunnel VPN supports two independent engines:
 *  - SSH_PAYLOAD: classic SSH + custom HTTP payload / bug-host injector
 *  - XRAY: VLESS / VMess / Trojan / Shadowsocks via Xray-core
 */
enum class EngineType {
    SSH_PAYLOAD,
    XRAY
}

enum class XrayProtocol {
    VLESS,
    VMESS,
    TROJAN,
    SHADOWSOCKS
}

enum class XrayNetwork(val wire: String) {
    TCP("tcp"),
    WS("ws"),
    GRPC("grpc"),
    HTTP_UPGRADE("httpupgrade"),
    KCP("kcp"),
    QUIC("quic");

    companion object {
        fun fromWire(value: String?): XrayNetwork =
            entries.firstOrNull { it.wire.equals(value, ignoreCase = true) } ?: TCP
    }
}

/**
 * Unified config model. Not every field applies to every protocol;
 * unused fields are simply left at their defaults.
 */
data class ConfigProfile(
    val id: String = UUID.randomUUID().toString(),
    val engine: EngineType,
    val protocol: XrayProtocol? = null, // only for engine == XRAY

    var remark: String = "",
    var address: String = "",
    var port: Int = 443,

    // auth
    var uuid: String = "",       // vless / vmess
    var password: String = "",   // trojan / shadowsocks
    var ssMethod: String = "",   // shadowsocks cipher, e.g. aes-256-gcm

    // vmess extras
    var alterId: Int = 0,
    var vmessSecurity: String = "auto",

    // vless extras
    var flow: String = "",

    // transport
    var network: XrayNetwork = XrayNetwork.TCP,
    var path: String = "/",
    var host: String = "",
    var serviceName: String = "", // grpc

    // TLS
    var tlsEnabled: Boolean = false,
    var sni: String = "",
    var alpn: String = "",
    var allowInsecure: Boolean = false,

    // SSH + payload engine fields
    var sshUsername: String = "",
    var sshPassword: String = "",
    var payload: String = "",

    val createdAt: Long = System.currentTimeMillis(),

    // moderation state for the admin-approval pending queue (user-submitted configs)
    var approved: Boolean = true
)
