package com.example.mindsync.ui.analytics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mindsync.R
import com.example.mindsync.data.model.AppUsageData
import com.example.mindsync.databinding.ItemAppUsageBinding
import java.util.concurrent.TimeUnit

class AppUsageAdapter : ListAdapter<AppUsageData, AppUsageAdapter.ViewHolder>(AppUsageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppUsageBinding.inflate(
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
        private val binding: ItemAppUsageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppUsageData) {
            binding.apply {
                tvAppName.text = item.appName
                tvTimeSpent.text = formatDuration(item.timeSpent)
                tvSessionCount.text = "${item.sessionCount} sessions"
                
                // Update progress bar
                val progress = calculateProgress(item.timeSpent)
                progressBarUsage.progress = progress
                
                // Show over limit warning with better formatting
                if (item.overLimitTime > 0) {
                    tvOverLimit.apply {
                        text = root.context.getString(R.string.over_limit_format, formatDuration(item.overLimitTime))
                        visibility = android.view.View.VISIBLE
                    }
                } else {
                    tvOverLimit.visibility = android.view.View.GONE
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

        private fun calculateProgress(timeSpent: Long): Int {
            // More granular progress calculation
            val maxTime = TimeUnit.HOURS.toMillis(2) // Reduced to 2 hours for better visualization
            return ((timeSpent.toFloat() / maxTime) * 100).toInt().coerceIn(0, 100)
        }
    }
}

private class AppUsageDiffCallback : DiffUtil.ItemCallback<AppUsageData>() {
    override fun areItemsTheSame(oldItem: AppUsageData, newItem: AppUsageData): Boolean {
        return oldItem.appName == newItem.appName
    }

    override fun areContentsTheSame(oldItem: AppUsageData, newItem: AppUsageData): Boolean {
        return oldItem == newItem
    }
} 