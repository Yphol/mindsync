package com.mindsynclabs.focusapp.ui.restriction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.mindsynclabs.focusapp.data.model.AppInfo
import com.mindsynclabs.focusapp.databinding.BottomSheetAppSelectionBinding
import com.mindsynclabs.focusapp.ui.restriction.adapter.AppListAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppSelectionBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAppSelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RestrictionViewModel by viewModels()
    private lateinit var appListAdapter: AppListAdapter
    private var isForTimeLimit: Boolean = false

    var onAppSelected: ((AppInfo) -> Unit)? = null

    companion object {
        const val TAG = "AppSelectionBottomSheet"
        
        fun newInstance(onAppSelected: (AppInfo) -> Unit, isForTimeLimit: Boolean = false): AppSelectionBottomSheet {
            return AppSelectionBottomSheet().apply {
                this.onAppSelected = onAppSelected
                this.isForTimeLimit = isForTimeLimit
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAppSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = if (isForTimeLimit) "Select App to Limit" else "Select App to Restrict"
        setupRecyclerView()
        setupSearch()
        setupSystemAppsToggle()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        appListAdapter = AppListAdapter { app ->
            onAppSelected?.invoke(app)
            dismiss()
        }

        binding.appList.apply {
            adapter = appListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearch() {
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.searchApps(text?.toString() ?: "")
        }
    }

    private fun setupSystemAppsToggle() {
        binding.switchShowSystemApps.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowSystemApps(isChecked)
        }
    }

    private fun observeViewModel() {
        viewModel.filteredApps.observe(viewLifecycleOwner) { apps ->
            appListAdapter.submitList(apps)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 