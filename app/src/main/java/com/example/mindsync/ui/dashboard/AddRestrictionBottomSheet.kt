package com.example.mindsync.ui.dashboard

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.mindsync.R
import com.example.mindsync.data.model.AppInfo
import com.example.mindsync.databinding.BottomSheetRestrictBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Date

@AndroidEntryPoint
class AddRestrictionBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetRestrictBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRestrictBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        with(binding) {
            // Show/hide time limit input based on restriction type
            radioGroupRestrictionType.setOnCheckedChangeListener { _, checkedId ->
                timeLimitInputLayout.visibility = when (checkedId) {
                    radioTimeLimit.id -> View.VISIBLE
                    else -> View.GONE
                }
            }

            btnApply.setOnClickListener {
                val appName = appNameInput.text.toString()
                if (appName.isBlank()) return@setOnClickListener

                val isForever = radioGroupRestrictionType.checkedRadioButtonId == radioBlock.id
                
                val timeLimit = if (!isForever) {
                    timeLimitInput.text.toString().toIntOrNull() ?: return@setOnClickListener
                } else 0

                val endTime = if (!isForever) {
                    Calendar.getInstance().apply {
                        add(Calendar.MINUTE, timeLimit)
                    }.time
                } else null

                val appInfo = AppInfo(
                    packageName = appName, // Using appName as packageName for now
                    appName = appName,
                    icon = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.purple_500)), // Default icon
                    isSystemApp = false // Default to false for manually added apps
                )

                viewModel.addRestriction(appInfo, endTime, isForever)
                dismiss()
            }

            btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 