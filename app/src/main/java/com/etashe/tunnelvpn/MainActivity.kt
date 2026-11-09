package com.etashe.tunnelvpn

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS = "etashe_prefs"
        private const val KEY_TELEGRAM_SHOWN = "telegram_dialog_shown"
        const val TELEGRAM_CHANNEL_URL = "https://t.me/eTaShe_Tunnel_VPN"
    }

    private var isConnected = false
    private var secondsConnected = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var tvConnectionTime: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvUsage: TextView

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isConnected) {
                secondsConnected++
                val h = secondsConnected / 3600
                val m = (secondsConnected % 3600) / 60
                val s = secondsConnected % 60
                tvConnectionTime.text = String.format("%02d:%02d:%02d", h, m, s)
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvConnectionTime = findViewById(R.id.tvConnectionTime)
        tvStatus = findViewById(R.id.tvStatus)
        tvUsage = findViewById(R.id.tvUsage)
        val btnConnect = findViewById<FrameLayout>(R.id.btnConnect)
        val btnSettings = findViewById<android.widget.ImageButton>(R.id.btnSettings)
        val btnImport = findViewById<android.widget.ImageButton>(R.id.btnImport)

        btnConnect.setOnClickListener { toggleConnection() }
        btnSettings.setOnClickListener {
            Toast.makeText(this, "الإعدادات قريباً", Toast.LENGTH_SHORT).show()
        }
        btnImport.setOnClickListener {
            startActivity(Intent(this, ImportActivity::class.java))
        }

        maybeShowTelegramDialog()
    }

    private fun toggleConnection() {
        isConnected = !isConnected
        if (isConnected) {
            tvStatus.text = getString(R.string.connected)
            tvStatus.setTextColor(getColor(R.color.status_connected))
            secondsConnected = 0
            timerHandler.post(tickRunnable)
        } else {
            tvStatus.text = getString(R.string.disconnected)
            tvStatus.setTextColor(getColor(R.color.status_disconnected))
            timerHandler.removeCallbacks(tickRunnable)
            tvConnectionTime.text = "00:00:00"
        }
    }

    private fun maybeShowTelegramDialog() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_TELEGRAM_SHOWN, false)) return

        AlertDialog.Builder(this, R.style.ThemeOverlay_EtasheTunnel_Dialog)
            .setTitle(R.string.telegram_dialog_title)
            .setMessage(R.string.telegram_dialog_message)
            .setCancelable(false)
            .setPositiveButton(R.string.telegram_dialog_join) { dialog, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_CHANNEL_URL)))
                dialog.dismiss()
            }
            .setNegativeButton(R.string.telegram_dialog_later) { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        prefs.edit().putBoolean(KEY_TELEGRAM_SHOWN, true).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(tickRunnable)
    }
}
