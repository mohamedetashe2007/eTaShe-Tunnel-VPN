package com.etashe.tunnelvpn.core

import android.util.Log
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

/**
 * Thin Kotlin wrapper around the real Xray-core engine shipped in `libv2ray.aar`
 * (built by CI from github.com/2dust/AndroidLibXrayLite — the same gomobile binding
 * that powers v2rayNG). This class owns the single [CoreController] instance and
 * exposes a small, app-friendly start/stop/status API.
 *
 * Packet-level routing (the VpnService TUN interface + tun2socks bridge that feeds
 * real device traffic into the local SOCKS5 inbound this engine listens on) is a
 * separate, not-yet-implemented piece — see EtasheVpnService. On its own this class
 * can start and stop a real Xray-core instance against a [XrayConfigBuilder]-built
 * config and report success/failure; it does not yet move any device traffic.
 */
class XrayCoreManager(
    private val listener: Listener,
) : CoreCallbackHandler {

    interface Listener {
        fun onCoreStarted()
        fun onCoreStopped()
        fun onCoreStatus(code: Int, message: String)
    }

    private var controller: CoreController? = null

    val isRunning: Boolean
        get() = controller?.isRunning == true

    /** One-time process-wide setup; safe to call more than once. */
    fun initEnvironment(assetDir: String) {
        Libv2ray.initCoreEnv(assetDir, "")
    }

    fun engineVersion(): String = Libv2ray.checkVersionX()

    /**
     * Starts Xray-core with the given JSON config (see [XrayConfigBuilder]).
     * [tunFd] is the raw TUN file descriptor when Xray should own the TUN device
     * directly, or 0 when packet routing is handled outside the core (our current
     * SOCKS-inbound architecture) — see this class's class doc.
     */
    fun start(configJson: String, tunFd: Int = 0): Boolean {
        return try {
            val c = Libv2ray.newCoreController(this).also { controller = it }
            c.startLoop(configJson, tunFd)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start Xray-core", t)
            controller = null
            false
        }
    }

    fun stop() {
        try {
            controller?.stopLoop()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to stop Xray-core", t)
        } finally {
            controller = null
        }
    }

    // --- CoreCallbackHandler ---

    override fun startup(): Long {
        listener.onCoreStarted()
        return 0
    }

    override fun shutdown(): Long {
        listener.onCoreStopped()
        return 0
    }

    override fun onEmitStatus(code: Long, message: String?): Long {
        listener.onCoreStatus(code.toInt(), message.orEmpty())
        return 0
    }

    companion object {
        private const val TAG = "XrayCoreManager"
    }
}
