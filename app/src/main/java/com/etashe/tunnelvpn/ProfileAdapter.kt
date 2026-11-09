package com.etashe.tunnelvpn

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.etashe.tunnelvpn.core.ConfigProfile
import com.etashe.tunnelvpn.core.EngineType

class ProfileAdapter(
    private val items: MutableList<ConfigProfile>,
    private val onDelete: (ConfigProfile) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.VH>() {

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val tvRemark: android.widget.TextView = view.findViewById(R.id.tvRemark)
        val tvDetails: android.widget.TextView = view.findViewById(R.id.tvDetails)
        val btnDelete: android.widget.ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.tvRemark.text = p.remark.ifBlank { "Server" }
        val tag = if (p.engine == EngineType.XRAY) p.protocol?.name ?: "XRAY" else "SSH+Payload"
        val transport = if (p.engine == EngineType.XRAY) "${p.network.wire}${if (p.tlsEnabled) "/tls" else ""}" else ""
        holder.tvDetails.text = listOfNotNull(
            tag,
            "${p.address}:${p.port}",
            transport.ifBlank { null }
        ).joinToString(" · ")

        holder.btnDelete.setOnClickListener { onDelete(p) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<ConfigProfile>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
