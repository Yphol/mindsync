package com.example.mindsync.ui.analytics.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mindsync.R
import com.example.mindsync.ui.analytics.AppUsageItem

class AppUsageAdapter : ListAdapter<AppUsageItem, AppUsageAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvUsageTime: TextView = itemView.findViewById(R.id.tvUsageTime)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

        fun bind(item: AppUsageItem) {
            tvAppName.text = item.appName
            tvCategory.text = item.category
            tvUsageTime.text = item.usageTime
            progressBar.progress = item.percentage
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppUsageItem>() {
            override fun areItemsTheSame(oldItem: AppUsageItem, newItem: AppUsageItem): Boolean {
                return oldItem.appName == newItem.appName
            }

            override fun areContentsTheSame(oldItem: AppUsageItem, newItem: AppUsageItem): Boolean {
                return oldItem == newItem
            }
        }
    }
} 