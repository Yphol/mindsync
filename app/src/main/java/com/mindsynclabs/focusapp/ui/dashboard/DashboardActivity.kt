package com.mindsynclabs.focusapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mindsynclabs.focusapp.R
import com.mindsynclabs.focusapp.data.model.AppInfo
import com.mindsynclabs.focusapp.data.model.AppRestriction
import com.mindsynclabs.focusapp.data.model.AppUsageData
import com.mindsynclabs.focusapp.databinding.ActivityDashboardBinding
import com.mindsynclabs.focusapp.ui.analytics.AnalyticsFragment
import com.mindsynclabs.focusapp.ui.focus.FocusFragment
import com.mindsynclabs.focusapp.ui.settings.SettingsFragment
import com.mindsynclabs.focusapp.ui.restriction.AppSelectionBottomSheet
import com.mindsynclabs.focusapp.ui.restriction.RestrictionSettingsBottomSheet
import com.mindsynclabs.focusapp.ui.restriction.adapter.ActiveRestrictionsAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.AlertDialog
import android.widget.NumberPicker
import android.provider.Settings

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var restrictionsAdapter: ActiveRestrictionsAdapter
    private lateinit var activeLimitAdapter: ActiveLimitAdapter
    private val TAG = "DashboardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "Creating DashboardActivity")
            binding = ActivityDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupViews()
            checkRequiredPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check permissions again when returning to the app
        checkRequiredPermissions(showPrompt = false)
    }

    private fun setupViews() {
        try {
            Log.d(TAG, "Setting up views")
            setupToolbar()
            setupCharts()
            setupRecyclerViews()
            setupButtons()
            setupBottomNavigation()
            observeViewModel()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up views", e)
            Toast.makeText(this, "Error setting up views: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up toolbar", e)
        }
    }

    private fun setupCharts() {
        try {
            // We'll use a simpler approach without MPAndroidChart
            Log.d(TAG, "Setting up charts with a simpler approach")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up charts", e)
        }
    }

    private fun setupRecyclerViews() {
        restrictionsAdapter = ActiveRestrictionsAdapter { restriction ->
            viewModel.removeRestriction(restriction.packageName)
        }
        binding.rvActiveRestrictions.apply {
            adapter = restrictionsAdapter
            layoutManager = LinearLayoutManager(this@DashboardActivity)
        }

        activeLimitAdapter = ActiveLimitAdapter()
        binding.rvActiveLimits.apply {
            adapter = activeLimitAdapter
            layoutManager = LinearLayoutManager(this@DashboardActivity)
        }
    }

    private fun setupButtons() {
        try {
            binding.btnStartFocusMode.setOnClickListener {
                showFragment(FocusFragment())
            }

            binding.btnAddRestriction.setOnClickListener {
                // Check permissions before showing app selection
                checkPermissionsAndProceed {
                    AppSelectionBottomSheet().apply {
                        onAppSelected = { appInfo ->
                            RestrictionSettingsBottomSheet.newInstance(appInfo).apply {
                                onRestrictionSet = { app, endTime, isForever ->
                                    viewModel.addRestriction(app, endTime, isForever)
                                    Toast.makeText(
                                        this@DashboardActivity,
                                        "${app.appName} has been restricted",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    
                                    // Check permissions again after adding a restriction
                                    checkRequiredPermissions()
                                }
                                show(supportFragmentManager, RestrictionSettingsBottomSheet.TAG)
                            }
                        }
                        show(supportFragmentManager, AppSelectionBottomSheet.TAG)
                    }
                }
            }

            binding.btnSetTimeLimit.setOnClickListener {
                val bottomSheet = AppSelectionBottomSheet.newInstance(
                    onAppSelected = { app -> showTimeLimitDialog(app) },
                    isForTimeLimit = true
                )
                bottomSheet.show(supportFragmentManager, "timeLimit")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up buttons", e)
        }
    }

    private fun checkRequiredPermissions(showPrompt: Boolean = true) {
        try {
            // Check for usage stats permission
            if (!viewModel.hasUsageStatsPermission()) {
                if (showPrompt) {
                    showUsageStatsPermissionDialog()
                }
                return
            }
            
            // Only check these if we have active restrictions
            if (viewModel.activeRestrictions.value?.isNotEmpty() == true) {
                // Check for accessibility service permission
                if (!viewModel.checkAccessibilityPermission()) {
                    if (showPrompt) {
                        showAccessibilityPermissionDialog()
                    }
                    return
                }
                
                // Check for overlay permission
                if (!viewModel.checkOverlayPermission()) {
                    if (showPrompt) {
                        showOverlayPermissionDialog()
                    }
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
        }
    }
    
    private fun showUsageStatsPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.usage_stats_permission_message)
            .setPositiveButton(R.string.enable) { _, _ ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showAccessibilityPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.accessibility_permission_message)
            .setPositiveButton(R.string.enable) { _, _ ->
                viewModel.requestAccessibilityPermission()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showOverlayPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.overlay_permission_message)
            .setPositiveButton(R.string.enable) { _, _ ->
                viewModel.requestOverlayPermission()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showTimeLimitDialog(app: AppInfo) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_time_limit, null)
        
        // Set up hour picker
        val hourPicker = dialogView.findViewById<NumberPicker>(R.id.hourPicker).apply {
            minValue = 0
            maxValue = 23
            value = 1 // Default 1 hour
        }
        
        // Set up minute picker
        val minutePicker = dialogView.findViewById<NumberPicker>(R.id.minutePicker).apply {
            minValue = 0
            maxValue = 59
            value = 0 // Default 0 minutes
        }
        
        // Set up second picker
        val secondPicker = dialogView.findViewById<NumberPicker>(R.id.secondPicker).apply {
            minValue = 0
            maxValue = 59
            value = 0 // Default 0 seconds
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Time Limit for ${app.appName}")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                try {
                    // Calculate total time in milliseconds
                    val hours = hourPicker.value
                    val minutes = minutePicker.value
                    val seconds = secondPicker.value
                    
                    val totalTimeMillis = (hours * 60 * 60 * 1000L) + 
                                         (minutes * 60 * 1000L) + 
                                         (seconds * 1000L)
                    
                    // Ensure at least 1 second is set
                    if (totalTimeMillis > 0) {
                        viewModel.setAppTimeLimit(app.packageName, totalTimeMillis)
                        Toast.makeText(
                            this, 
                            "Time limit set: ${formatDuration(totalTimeMillis)}", 
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Please set a time limit greater than zero",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting time limit", e)
                    Toast.makeText(this, "Failed to set time limit", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupBottomNavigation() {
        try {
            binding.bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_dashboard -> {
                        showDashboard()
                        true
                    }
                    R.id.nav_analytics -> {
                        showFragment(AnalyticsFragment())
                        true
                    }
                    R.id.nav_focus -> {
                        showFragment(FocusFragment())
                        true
                    }
                    R.id.nav_settings -> {
                        showFragment(SettingsFragment())
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation", e)
        }
    }

    private fun showDashboard() {
        try {
            binding.dashboardContent.visibility = View.VISIBLE
            binding.fragmentContainer.visibility = View.GONE
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing dashboard", e)
        }
    }

    private fun showFragment(fragment: androidx.fragment.app.Fragment) {
        try {
            binding.dashboardContent.visibility = View.GONE
            binding.fragmentContainer.visibility = View.VISIBLE
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing fragment", e)
            Toast.makeText(this, "Error showing screen: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        try {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
                if (supportFragmentManager.backStackEntryCount == 1) {
                    showDashboard()
                    binding.bottomNav.selectedItemId = R.id.nav_dashboard
                }
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling back press", e)
            super.onBackPressed()
        }
    }

    private fun observeViewModel() {
        try {
            viewModel.todaysUsageData.observe(this) { usageData ->
                try {
                    updateTotalScreenTime(usageData.sumOf { it.timeSpent })
                    updateAppUsage(usageData)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating usage data", e)
                }
            }

            viewModel.activeRestrictions.observe(this) { restrictions ->
                try {
                    restrictionsAdapter.submitList(restrictions)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating restrictions", e)
                }
            }

            viewModel.activeLimits.observe(this) { limits ->
                try {
                    activeLimitAdapter.submitList(limits)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating active limits", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up observers", e)
        }
    }

    private fun updateAppUsage(usageData: List<AppUsageData>) {
        try {
            // Sort by time spent and take top apps
            val sortedData = usageData.sortedByDescending { it.timeSpent }.take(4)
            
            // Update the card values with the usage data
            updateTotalScreenTime(sortedData.sumOf { it.timeSpent })
            binding.tvFocusScore.text = calculateFocusScore(sortedData).toString()
            binding.tvProductiveTime.text = formatDuration(calculateProductiveTime(sortedData))
            binding.tvSuccessRate.text = "${calculateSuccessRate(sortedData)}%"

            // Update screen time chart
            updateScreenTimeChart()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating app usage", e)
        }
    }

    private fun calculateFocusScore(usageData: List<AppUsageData>): Int {
        val totalTime = usageData.sumOf { it.timeSpent }
        val overLimitTime = usageData.sumOf { it.overLimitTime }
        return ((1 - (overLimitTime.toDouble() / totalTime)) * 100).toInt().coerceIn(0, 100)
    }

    private fun calculateProductiveTime(usageData: List<AppUsageData>): Long {
        return usageData.filter { !it.packageName.contains("social") && !it.packageName.contains("game") }
            .sumOf { it.timeSpent }
    }

    private fun calculateSuccessRate(usageData: List<AppUsageData>): Int {
        val totalApps = usageData.size
        val appsWithinLimit = usageData.count { it.overLimitTime == 0L }
        return ((appsWithinLimit.toDouble() / totalApps) * 100).toInt()
    }

    private fun formatDuration(totalTimeMillis: Long): String {
        try {
            val hours = TimeUnit.MILLISECONDS.toHours(totalTimeMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTimeMillis) % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting duration", e)
            return "0m"
        }
    }

    private fun updateTotalScreenTime(totalTimeMillis: Long) {
        try {
            val hours = TimeUnit.MILLISECONDS.toHours(totalTimeMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTimeMillis) % 60
            
            // Format for the new UI
            binding.tvTotalScreenTime.text = when {
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating total screen time", e)
            binding.tvTotalScreenTime.text = "0m"
        }
    }

    private fun updateScreenTimeChart() {
        try {
            // We'll use a simpler approach without MPAndroidChart
            Log.d(TAG, "Updating screen time chart with a simpler approach")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating screen time chart", e)
        }
    }

    private fun checkPermissionsAndProceed(onPermissionsGranted: () -> Unit) {
        try {
            // Check for usage stats permission first
            if (!viewModel.hasUsageStatsPermission()) {
                showUsageStatsPermissionDialog()
                return
            }
            
            // Check for accessibility service permission
            if (!viewModel.checkAccessibilityPermission()) {
                showAccessibilityPermissionDialog()
                return
            }
            
            // Check for overlay permission
            if (!viewModel.checkOverlayPermission()) {
                showOverlayPermissionDialog()
                return
            }
            
            // All permissions granted, proceed
            onPermissionsGranted()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
        }
    }
} 