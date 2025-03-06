package com.example.mindsync.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mindsync.R
import com.example.mindsync.data.model.AppInfo
import com.example.mindsync.data.model.AppRestriction
import com.example.mindsync.data.model.AppUsageData
import com.example.mindsync.databinding.ActivityDashboardBinding
import com.example.mindsync.ui.analytics.AnalyticsFragment
import com.example.mindsync.ui.focus.FocusFragment
import com.example.mindsync.ui.settings.SettingsFragment
import com.example.mindsync.ui.restriction.AppSelectionBottomSheet
import com.example.mindsync.ui.restriction.RestrictionSettingsBottomSheet
import com.example.mindsync.ui.restriction.adapter.ActiveRestrictionsAdapter
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.components.XAxis
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.AlertDialog
import android.widget.NumberPicker

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
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
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
            binding.screenTimeChart.apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false)
                setDrawGridBackground(false)
                setDrawBorders(false)
                
                xAxis.apply {
                    setDrawAxisLine(false)
                    setDrawGridLines(true)
                    gridColor = ContextCompat.getColor(context, R.color.background_tertiary)
                    gridLineWidth = 0.5f
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawLabels(false)
                }

                axisLeft.apply {
                    setDrawAxisLine(false)
                    setDrawGridLines(true)
                    gridColor = ContextCompat.getColor(context, R.color.background_tertiary)
                    gridLineWidth = 0.5f
                    setDrawLabels(false)
                }

                axisRight.isEnabled = false
                
                setNoDataText("")
                animateX(1000)
                
                setViewPortOffsets(4f, 4f, 4f, 4f)
                extraBottomOffset = 0f
                extraTopOffset = 0f
                extraLeftOffset = 0f
                extraRightOffset = 0f
            }
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
                AppSelectionBottomSheet().apply {
                    onAppSelected = { appInfo ->
                        RestrictionSettingsBottomSheet.newInstance(appInfo).apply {
                            onRestrictionSet = { app, endTime, isForever ->
                                viewModel.addRestriction(app, endTime, isForever)
                            }
                            show(supportFragmentManager, RestrictionSettingsBottomSheet.TAG)
                        }
                    }
                    show(supportFragmentManager, AppSelectionBottomSheet.TAG)
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

    private fun showTimeLimitDialog(app: AppInfo) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_time_limit, null)
        val timePicker = dialogView.findViewById<NumberPicker>(R.id.timePickerSpinner).apply {
            minValue = 1
            maxValue = 24
            value = 2 // Default 2 hours
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Time Limit for ${app.appName}")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                try {
                    val limitInHours = timePicker.value
                    viewModel.setAppTimeLimit(app.packageName, limitInHours * 60 * 60 * 1000L)
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
            binding.tvTotalScreenTime.text = formatDuration(sortedData.sumOf { it.timeSpent })
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
            binding.tvTotalScreenTime.text = getString(R.string.total_screen_time, hours, minutes)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating total screen time", e)
        }
    }

    private fun updateScreenTimeChart() {
        try {
            val entries = (0..23).map { hour ->
                Entry(hour.toFloat(), (Math.sin(hour * Math.PI / 12) * 50 + 50).toFloat())
            }

            val dataSet = LineDataSet(entries, "").apply {  // Empty string for no label
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.2f
                setDrawFilled(true)
                setDrawCircles(false)
                lineWidth = 2f
                color = ContextCompat.getColor(this@DashboardActivity, R.color.chart_blue)
                fillColor = ContextCompat.getColor(this@DashboardActivity, R.color.chart_blue)
                fillAlpha = 25
                setDrawHorizontalHighlightIndicator(false)
                setDrawVerticalHighlightIndicator(false)
                setDrawValues(false)  // Disable value text on the line
            }

            binding.screenTimeChart.apply {
                data = LineData(dataSet)
                invalidate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating screen time chart", e)
        }
    }
} 