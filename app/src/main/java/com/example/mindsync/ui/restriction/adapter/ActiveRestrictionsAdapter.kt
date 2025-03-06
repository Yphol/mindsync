package com.example.mindsync.ui.restriction.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mindsync.data.model.AppRestriction
import com.example.mindsync.databinding.ItemActiveRestrictionBinding
import java.util.concurrent.TimeUnit

class ActiveRestrictionsAdapter(
    private val onRemoveClick: (AppRestriction) -> Unit
) : ListAdapter<AppRestriction, ActiveRestrictionsAdapter.RestrictionViewHolder>(RestrictionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestrictionViewHolder {
        val binding = ItemActiveRestrictionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RestrictionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RestrictionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RestrictionViewHolder(
        private val binding: ItemActiveRestrictionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(restriction: AppRestriction) {
            binding.apply {
                appIcon.setImageDrawable(restriction.icon)
                appName.text = restriction.appName
                
                restrictionTime.text = if (restriction.isForever) {
                    "Restricted Forever"
                } else {
                    val remainingTime = restriction.endTime?.time?.minus(System.currentTimeMillis()) ?: 0
                    val hours = TimeUnit.MILLISECONDS.toHours(remainingTime)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60
                    if (hours > 0) {
                        "${hours}h ${minutes}m remaining"
                    } else {
                        "${minutes}m remaining"
                    }
                }

                btnRemove.setOnClickListener { onRemoveClick(restriction) }
            }
        }
    }

    private class RestrictionDiffCallback : DiffUtil.ItemCallback<AppRestriction>() {
        override fun areItemsTheSame(oldItem: AppRestriction, newItem: AppRestriction): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppRestriction, newItem: AppRestriction): Boolean {
            return oldItem == newItem
        }
    }
} 