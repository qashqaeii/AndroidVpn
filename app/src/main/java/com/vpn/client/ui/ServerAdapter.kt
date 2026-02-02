package com.vpn.client.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vpn.client.R
import com.vpn.client.data.model.ServerItem
import com.vpn.client.data.model.ServerStatus
import com.vpn.client.databinding.ItemServerBinding

class ServerAdapter(
    private var selectedId: Int?,
    private val onSelect: (ServerItem) -> Unit
) : ListAdapter<ServerItem, ServerAdapter.Holder>(Diff) {

    fun setSelectedId(id: Int?) {
        val old = selectedId
        selectedId = id
        if (old != id) {
            currentList.forEachIndexed { index, item ->
                if (item.id == old || item.id == id) notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemServerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding, selectedId, onSelect)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position), this.selectedId)
    }

    class Holder(
        private val binding: ItemServerBinding,
        private var selectedId: Int?,
        private val onSelect: (ServerItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ServerItem, currentSelectedId: Int?) {
            binding.root.isSelected = item.id == currentSelectedId
            binding.flag.text = item.flag
            binding.name.text = item.name
            binding.status.text = when (item.status) {
                ServerStatus.ONLINE -> binding.root.context.getString(R.string.status_online)
                ServerStatus.SLOW -> binding.root.context.getString(R.string.status_slow)
                ServerStatus.OFFLINE -> binding.root.context.getString(R.string.status_offline)
            } + if (item.pingMs >= 0) " • ${item.pingMs} ms" else ""
            binding.ping.text = if (item.pingMs >= 0) binding.root.context.getString(R.string.ping_ms, item.pingMs) else "—"
            binding.ping.visibility = if (item.pingMs >= 0) android.view.View.VISIBLE else android.view.View.GONE
            binding.statusDot.setBackgroundResource(
                when (item.status) {
                    ServerStatus.ONLINE -> R.drawable.bg_status_online
                    ServerStatus.SLOW -> R.drawable.bg_status_slow
                    ServerStatus.OFFLINE -> R.drawable.bg_status_offline
                }
            )
            binding.root.setOnClickListener { onSelect(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ServerItem>() {
        override fun areItemsTheSame(a: ServerItem, b: ServerItem) = a.id == b.id
        override fun areContentsTheSame(a: ServerItem, b: ServerItem) = a == b
    }
}
