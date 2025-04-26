package com.mindsynclabs.focusapp.ui.restriction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mindsynclabs.focusapp.data.model.AppInfo
import com.mindsynclabs.focusapp.databinding.BottomSheetRestrictionSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar
import java.util.Date

class RestrictionSettingsBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetRestrictionSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var appInfo: AppInfo
    var onRestrictionSet: ((AppInfo, Date?, Boolean) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRestrictionSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTimePickers()
        setupButtons()
        setupSwitch()
    }

    private fun setupTimePickers() {
        binding.apply {
            hourPicker.apply {
                minValue = 0
                maxValue = 23
                value = 1
            }

            minutePicker.apply {
                minValue = 0
                maxValue = 59
                value = 0
            }

            secondPicker.apply {
                minValue = 0
                maxValue = 59
                value = 0
            }
        }
    }

    private fun setupButtons() {
        binding.apply {
            btnBack.setOnClickListener {
                dismiss()
            }

            btnSubmit.setOnClickListener {
                val isForever = binding.switchRestrictForever.isChecked
                val endTime = if (!isForever) {
                    calculateEndTime()
                } else {
                    null
                }
                onRestrictionSet?.invoke(appInfo, endTime, isForever)
                dismiss()
            }
        }
    }

    private fun setupSwitch() {
        binding.switchRestrictForever.setOnCheckedChangeListener { _, isChecked ->
            binding.apply {
                hourPicker.isEnabled = !isChecked
                minutePicker.isEnabled = !isChecked
                secondPicker.isEnabled = !isChecked
            }
        }
    }

    private fun calculateEndTime(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, binding.hourPicker.value)
        calendar.add(Calendar.MINUTE, binding.minutePicker.value)
        calendar.add(Calendar.SECOND, binding.secondPicker.value)
        return calendar.time
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RestrictionSettingsBottomSheet"

        fun newInstance(appInfo: AppInfo): RestrictionSettingsBottomSheet {
            return RestrictionSettingsBottomSheet().apply {
                this.appInfo = appInfo
            }
        }
    }
} 