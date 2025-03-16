package com.example.mindsync.ui.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mindsync.R
import com.example.mindsync.data.model.UsageSpikeItem

class UsageSpikesAdapter : ListAdapter<UsageSpikeItem, UsageSpikesAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usage_spike, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvTimeRange: TextView = itemView.findViewById(R.id.tvTimeRange)

        fun bind(item: UsageSpikeItem) {
            tvAppName.text = item.appName
            tvDate.text = item.date
            tvDuration.text = item.duration
            tvTimeRange.text = item.timeRange
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UsageSpikeItem>() {
            override fun areItemsTheSame(oldItem: UsageSpikeItem, newItem: UsageSpikeItem): Boolean {
                return oldItem.appName == newItem.appName && oldItem.date == newItem.date
            }

            override fun areContentsTheSame(oldItem: UsageSpikeItem, newItem: UsageSpikeItem): Boolean {
                return oldItem == newItem
            }
        }
    }
} 