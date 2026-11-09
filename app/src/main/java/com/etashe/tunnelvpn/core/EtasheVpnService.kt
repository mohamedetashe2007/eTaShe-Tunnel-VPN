package com.etashe.tunnelvpn.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.etashe.tunnelvpn.MainActivity
import com.etashe.tunnelvpn.R

/**
 * Owns the lifecycle of the real Xray-core engine ([XrayCoreManager]) and the Android
 * TUN interface for a connected tunnel.
 *
 * Current state: VPN-permission handling, TUN interface setup (addresses/routes/DNS),
 * the foreground notification, and starting/stopping the real Xray-core engine against
 * a [ConfigProfile] (via [XrayConfigBuilder]) are all implemented and real.
 *
 * NOT yet implemented: the tun2socks packet bridge that reads raw IP packets off the
 * TUN file descriptor and feeds them into the local SOCKS5 inbound Xray-core listens on
 * (and writes return packets back). Until that bridge exists, device traffic does not
 * actually flow through the tunnel yet even though the engine itself is really running —
 * this is the next sub-step of the Xray engine task, not a cosmetic TODO.
 */
class EtasheVpnService : VpnService(), XrayCoreManager.Listener {

    private var tunInterface: ParcelFileDescriptor? = null
    private var core: XrayCoreManager? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                stopTunnel()
                return START_NOT_STICKY
            }
            else -> {
                val profileId = intent?.getStringExtra(EXTRA_PROFILE_ID)
                val profile = profileId?.let { id -> ProfileStore(this).getAll().firstOrNull { it.id == id } }
                if (profile == null || profile.engine != EngineType.XRAY) {
                    Log.w(TAG, "No valid Xray profile to connect with")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startTunnel(profile)
            }
        }
        return START_STICKY
    }

    private fun startTunnel(profile: ConfigProfile) {
        startForeground(NOTIFICATION_ID, buildNotification())

        val manager = XrayCoreManager(this).also { core = it }
        manager.initEnvironment(filesDir.absolutePath)

        val configJson = XrayConfigBuilder.build(profile, SOCKS_PORT)
        val started = manager.start(configJson, tunFd = 0)
        if (!started) {
            Log.e(TAG, "Xray-core failed to start")
            stopSelf()
            return
        }

        tunInterface = Builder()
            .setSession(profile.remark.ifBlank { "eTaShe Tunnel VPN" })
            .addAddress(TUN_ADDRESS, 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer(DNS_PRIMARY)
            .addDnsServer(DNS_SECONDARY)
            .setMtu(1500)
            .establish()

        // NOTE: tunInterface is established but no packet bridge (tun2socks) reads/writes
        // it yet, so device traffic does not actually route through `manager`'s SOCKS
        // inbound (127.0.0.1:SOCKS_PORT) yet. See class doc.
    }

    private fun stopTunnel() {
        core?.stop()
        core = null
        try {
            tunInterface?.close()
        } catch (t: Throwable) {
            Log.e(TAG, "Error closing TUN interface", t)
        }
        tunInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopTunnel()
        super.onRevoke()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            manager?.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    // --- XrayCoreManager.Listener ---

    override fun onCoreStarted() {
        Log.i(TAG, "Xray-core started")
    }

    override fun onCoreStopped() {
        Log.i(TAG, "Xray-core stopped")
    }

    override fun onCoreStatus(code: Int, message: String) {
        Log.i(TAG, "Xray-core status [$code]: $message")
    }

    companion object {
        private const val TAG = "EtasheVpnService"
        private const val CHANNEL_ID = "etashe_vpn_channel"
        private const val NOTIFICATION_ID = 1001
        private const val SOCKS_PORT = 10808
        private const val TUN_ADDRESS = "26.26.26.1"
        private const val DNS_PRIMARY = "1.1.1.1"
        private const val DNS_SECONDARY = "8.8.8.8"

        const val ACTION_DISCONNECT = "com.etashe.tunnelvpn.action.DISCONNECT"
        const val EXTRA_PROFILE_ID = "profile_id"
    }
}
