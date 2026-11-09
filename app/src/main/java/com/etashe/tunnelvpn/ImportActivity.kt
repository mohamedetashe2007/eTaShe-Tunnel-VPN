package com.etashe.tunnelvpn

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etashe.tunnelvpn.core.LinkParseException
import com.etashe.tunnelvpn.core.LinkParser
import com.etashe.tunnelvpn.core.ProfileStore

/**
 * Manual link import screen (VLESS / VMess / Trojan / Shadowsocks),
 * in the spirit of NetMod's "paste a link, get a config" flow.
 */
class ImportActivity : AppCompatActivity() {

    private lateinit var store: ProfileStore
    private lateinit var adapter: ProfileAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        store = ProfileStore(this)

        val etLink = findViewById<EditText>(R.id.etLink)
        val btnParse = findViewById<Button>(R.id.btnParse)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val rv = findViewById<RecyclerView>(R.id.rvProfiles)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = ProfileAdapter(store.getAll()) { profile ->
            store.remove(profile.id)
            refreshList()
            Toast.makeText(this, getString(R.string.import_deleted), Toast.LENGTH_SHORT).show()
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnBack.setOnClickListener { finish() }

        btnParse.setOnClickListener {
            val raw = etLink.text?.toString().orEmpty()
            if (raw.isBlank()) {
                Toast.makeText(this, getString(R.string.import_empty_input), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                val profile = LinkParser.parse(raw)
                store.add(profile)
                etLink.setText("")
                refreshList()
                Toast.makeText(
                    this,
                    getString(R.string.import_success, profile.remark),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: LinkParseException) {
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.import_generic_error), Toast.LENGTH_LONG).show()
            }
        }

        refreshList()
    }

    private fun refreshList() {
        val all = store.getAll()
        adapter.submit(all)
        tvEmpty.visibility = if (all.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }
}
