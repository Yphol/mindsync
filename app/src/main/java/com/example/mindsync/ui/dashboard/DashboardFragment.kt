package com.example.mindsync.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mindsync.data.model.AppRestriction
import com.example.mindsync.databinding.FragmentDashboardBinding
import com.example.mindsync.ui.restriction.AppSelectionBottomSheet
import com.example.mindsync.ui.restriction.RestrictionSettingsBottomSheet
import com.example.mindsync.ui.restriction.adapter.ActiveRestrictionsAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var activeRestrictionsAdapter: ActiveRestrictionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAppRestrictions()
        setupButtons()
        observeViewModel()
        observeEvents()
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.restrictionExpiredEvent.collect { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAppRestrictions() {
        activeRestrictionsAdapter = ActiveRestrictionsAdapter { restriction ->
            viewModel.removeRestriction(restriction.packageName)
        }

        binding.activeRestrictionsList.apply {
            adapter = activeRestrictionsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupButtons() {
        binding.btnStartFocusMode.setOnClickListener {
            // TODO: Implement focus mode
        }

        binding.btnAddRestriction.setOnClickListener {
            AppSelectionBottomSheet().apply {
                onAppSelected = { appInfo ->
                    RestrictionSettingsBottomSheet.newInstance(appInfo).apply {
                        onRestrictionSet = { app, endTime, isForever ->
                            viewModel.addRestriction(app, endTime, isForever)
                        }
                        show(childFragmentManager, RestrictionSettingsBottomSheet.TAG)
                    }
                }
                show(childFragmentManager, AppSelectionBottomSheet.TAG)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.todaysUsageData.observe(viewLifecycleOwner) { _ ->
            // TODO: Update pie chart with usage data
        }

        viewModel.activeRestrictions.observe(viewLifecycleOwner) { restrictions ->
            activeRestrictionsAdapter.submitList(restrictions.toMutableList())
            binding.activeRestrictionsGroup.isVisible = restrictions.isNotEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = DashboardFragment()
    }
} 