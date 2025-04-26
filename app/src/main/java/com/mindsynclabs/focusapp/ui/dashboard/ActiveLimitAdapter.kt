package com.mindsynclabs.focusapp.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindsynclabs.focusapp.R
import com.mindsynclabs.focusapp.data.model.AppTimeLimit

class ActiveLimitAdapter : ListAdapter<AppTimeLimit, ActiveLimitAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_limit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val limit = getItem(position)
        holder.bind(limit)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val appName: TextView = itemView.findViewById(R.id.tvAppName)
        private val limitInfo: TextView = itemView.findViewById(R.id.tvLimitInfo)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

        fun bind(limit: AppTimeLimit) {
            appIcon.setImageDrawable(limit.appIcon)
            appName.text = limit.appName
            
            val percentUsed = ((limit.timeUsed.toFloat() / limit.timeLimit) * 100).toInt()
            val timeLeftMinutes = (limit.timeLimit - limit.timeUsed) / 60000 // Convert ms to minutes
            
            limitInfo.text = "${percentUsed}% used - ${timeLeftMinutes}m left"
            progressBar.progress = percentUsed
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppTimeLimit>() {
            override fun areItemsTheSame(oldItem: AppTimeLimit, newItem: AppTimeLimit): Boolean {
                return oldItem.packageName == newItem.packageName
            }

            override fun areContentsTheSame(oldItem: AppTimeLimit, newItem: AppTimeLimit): Boolean {
                return oldItem == newItem
            }
        }
    }
} 