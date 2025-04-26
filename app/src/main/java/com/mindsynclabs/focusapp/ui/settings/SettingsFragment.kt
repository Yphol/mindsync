package com.mindsynclabs.focusapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mindsynclabs.focusapp.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSettings()
        observeViewModel()
    }

    private fun setupSettings() {
        binding.apply {
            // Setup notification settings
            switchNotifications.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setNotificationsEnabled(isChecked)
            }

            // Setup dark mode settings
            switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setDarkModeEnabled(isChecked)
            }

            // Setup daily reminder settings
            switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setDailyReminderEnabled(isChecked)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.notificationsEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchNotifications.isChecked = enabled
        }

        viewModel.darkModeEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchDarkMode.isChecked = enabled
        }

        viewModel.dailyReminderEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchDailyReminder.isChecked = enabled
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 