package com.example.mindsync.ui.analytics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mindsync.data.model.UsageSpike
import com.example.mindsync.databinding.ItemUsageSpikeBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class UsageSpikesAdapter : ListAdapter<UsageSpike, UsageSpikesAdapter.ViewHolder>(UsageSpikeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUsageSpikeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemUsageSpikeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(item: UsageSpike) {
            binding.apply {
                tvAppName.text = item.appName
                tvDate.text = dateFormat.format(item.startTime)
                tvDuration.text = formatDuration(item.duration)
                tvTimeRange.text = "${timeFormat.format(item.startTime)} - ${timeFormat.format(item.endTime)}"
                
                if (item.sessionCount > 1) {
                    tvSessionCount.text = "${item.sessionCount} sessions"
                    tvSessionCount.visibility = android.view.View.VISIBLE
                } else {
                    tvSessionCount.visibility = android.view.View.GONE
                }
            }
        }

        private fun formatDuration(millis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        }
    }
}

private class UsageSpikeDiffCallback : DiffUtil.ItemCallback<UsageSpike>() {
    override fun areItemsTheSame(oldItem: UsageSpike, newItem: UsageSpike): Boolean {
        return oldItem.startTime == newItem.startTime && oldItem.appName == newItem.appName
    }

    override fun areContentsTheSame(oldItem: UsageSpike, newItem: UsageSpike): Boolean {
        return oldItem == newItem
    }
} 