package com.mindsynclabs.focusapp.ui.dashboard

import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindsynclabs.focusapp.data.model.AppRestriction
import com.mindsynclabs.focusapp.databinding.ItemRestrictedAppBinding
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ActiveRestrictionsAdapter(
    private val onRemoveClick: (AppRestriction) -> Unit
) : ListAdapter<AppRestriction, ActiveRestrictionsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRestrictedAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }

    class ViewHolder(
        private val binding: ItemRestrictedAppBinding,
        private val onRemoveClick: (AppRestriction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var countDownTimer: CountDownTimer? = null

        fun bind(restriction: AppRestriction) {
            // Cancel any existing timer
            cleanup()

            binding.apply {
                imgAppIcon.setImageDrawable(restriction.icon)
                tvAppName.text = restriction.appName

                if (restriction.isForever) {
                    tvRestrictionType.text = "Forever"
                } else {
                    val endTime = restriction.endTime?.time ?: return
                    updateTimeDisplay(endTime - System.currentTimeMillis())
                    
                    countDownTimer = object : CountDownTimer(endTime - System.currentTimeMillis(), 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            updateTimeDisplay(millisUntilFinished)
                        }

                        override fun onFinish() {
                            onRemoveClick(restriction)
                        }
                    }.start()
                }

                btnRemove.setOnClickListener { 
                    cleanup()
                    onRemoveClick(restriction) 
                }
            }
        }

        fun cleanup() {
            countDownTimer?.cancel()
            countDownTimer = null
        }

        private fun updateTimeDisplay(millisUntilFinished: Long) {
            val remaining = max(0, millisUntilFinished)
            val hours = TimeUnit.MILLISECONDS.toHours(remaining)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
            
            binding.tvRestrictionType.text = String.format("%02d:%02d:%02d remaining", hours, minutes, seconds)
            Log.d("Timer", "Updated time: ${binding.tvRestrictionType.text}")
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppRestriction>() {
            override fun areItemsTheSame(oldItem: AppRestriction, newItem: AppRestriction): Boolean {
                return oldItem.packageName == newItem.packageName
            }

            override fun areContentsTheSame(oldItem: AppRestriction, newItem: AppRestriction): Boolean {
                return false
            }
        }
    }
} 