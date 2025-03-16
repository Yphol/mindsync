package com.example.mindsync.ui.analytics

import android.content.Context
import android.content.Intent
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mindsync.R
import com.example.mindsync.data.model.*
import com.example.mindsync.databinding.FragmentAnalyticsBinding
import com.example.mindsync.ui.analytics.adapter.AppUsageAdapter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import java.util.concurrent.TimeUnit

data class AppUsageItem(
    val appName: String,
    val category: String,
    val usageTime: String,
    val percentage: Int
)

@AndroidEntryPoint
class AnalyticsFragment : Fragment() {
    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsViewModel by viewModels()
    private lateinit var appUsageAdapter: AppUsageAdapter
    private lateinit var usageSpikesAdapter: UsageSpikesAdapter
    private val TAG = "AnalyticsFragment"

    enum class TimeRange {
        TODAY, WEEK, MONTH
    }

    private var selectedTimeRange = TimeRange.TODAY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setHourlyChartWidth()  // Ensure the chart is properly sized
        setupRecyclerViews()
        setupTimeRangeSelection()
        setupDetailedAnalyticsButton()
        observeViewModel()
    }

    private fun setupCharts() {
        setupOverallTrendChart()
        setupHourlyUsageChart()
        setupWeeklyTrendChart()
    }

    private fun setupOverallTrendChart() {
        binding.overallTrendChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.background_tertiary)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                textSize = 10f
                setAvoidFirstLastClipping(true)
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.background_tertiary)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                textSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatDuration(value.toLong())
                    }
                }
            }
            
            axisRight.isEnabled = false
            legend.apply {
                textSize = 12f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
            }
            
            animateY(1000)
            extraBottomOffset = 8f
        }
    }

    private fun setupHourlyUsageChart() {
        binding.hourlyUsageChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.background_tertiary)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                textSize = 10f
                labelCount = 6  // Show fewer labels to avoid crowding
                granularity = 1f
                axisMinimum = -0.5f
                axisMaximum = 23.5f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val hour = value.toInt()
                        // Only show a few hour labels to avoid crowding
                        return if (hour % 4 == 0) "${hour}h" else ""
                    }
                }
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.background_tertiary)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                textSize = 10f
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatDuration(value.toLong())
                    }
                }
            }
            
            axisRight.isEnabled = false
            legend.apply {
                textSize = 12f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE
            }
            
            // Disable auto-scaling
            setAutoScaleMinMaxEnabled(false)
            
            // Set fixed visible range
            setVisibleXRange(0f, 24f)
            setVisibleXRangeMinimum(24f)
            setVisibleXRangeMaximum(24f)
            
            // Disable animations
            animateY(0)
            
            extraBottomOffset = 8f
        }
    }

    private fun setupWeeklyTrendChart() {
        binding.weeklyTrendChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.GRAY
                textSize = 12f
                setAvoidFirstLastClipping(true)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                gridLineWidth = 0.5f
                textColor = Color.GRAY
                textSize = 12f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatDuration(value.toLong())
                    }
                }
            }
            
            axisRight.isEnabled = false
            legend.apply {
                textSize = 14f
                textColor = Color.DKGRAY
                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
            }
            
            animateY(1500)
            extraBottomOffset = 8f
        }
    }

    private fun setupRecyclerViews() {
        appUsageAdapter = AppUsageAdapter()
        binding.rvAppUsage.apply {
            adapter = appUsageAdapter
            layoutManager = LinearLayoutManager(context)
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_secondary))
            setPadding(16, 8, 16, 8)
            clipToPadding = false
            addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(
                context, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
            ).apply {
                ContextCompat.getDrawable(context, R.drawable.divider_light)?.let { setDrawable(it) }
            })
        }

        usageSpikesAdapter = UsageSpikesAdapter()
        binding.rvUsageSpikes.apply {
            adapter = usageSpikesAdapter
            layoutManager = LinearLayoutManager(context)
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_secondary))
            setPadding(16, 8, 16, 8)
            clipToPadding = false
            addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(
                context, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
            ).apply {
                ContextCompat.getDrawable(context, R.drawable.divider_light)?.let { setDrawable(it) }
            })
        }
        
        // Add mock data for usage spikes
        updateUsageSpikes()
    }

    private fun setupTimeRangeSelection() {
        binding.chipToday.setOnClickListener {
            selectedTimeRange = TimeRange.TODAY
            viewModel.setTimeRange(TimeRangePreset.TODAY)
            updateCharts()
        }

        binding.chipWeek.setOnClickListener {
            selectedTimeRange = TimeRange.WEEK
            viewModel.setTimeRange(TimeRangePreset.LAST_7_DAYS)
            updateCharts()
        }

        binding.chipMonth.setOnClickListener {
            selectedTimeRange = TimeRange.MONTH
            viewModel.setTimeRange(TimeRangePreset.LAST_30_DAYS)
            updateCharts()
        }

        // Set initial selection
        viewModel.setTimeRange(TimeRangePreset.TODAY)
        updateCharts()
    }

    private fun setupDetailedAnalyticsButton() {
        binding.fabDetailedAnalytics.setOnClickListener {
            val intent = Intent(requireContext(), DetailedAnalyticsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.apply {
            // Observe app usage breakdown
            appUsageBreakdown.observe(viewLifecycleOwner) { appUsageData ->
                if (appUsageData.isNotEmpty()) {
                    updateAppBreakdown()
                }
            }
            
            // Observe hourly usage
            hourlyUsage.observe(viewLifecycleOwner) { hourlyUsageData ->
                Log.d(TAG, "Hourly usage data updated: ${hourlyUsageData.size} data points")
                if (hourlyUsageData.isNotEmpty() && selectedTimeRange == TimeRange.TODAY) {
                    Log.d(TAG, "Triggering hourly usage chart update")
                    updateHourlyUsageChart()
                }
            }
            
            // Observe weekly trends
            weeklyTrends.observe(viewLifecycleOwner) { weeklyTrendsData ->
                if (weeklyTrendsData.isNotEmpty() && selectedTimeRange != TimeRange.TODAY) {
                    if (selectedTimeRange == TimeRange.WEEK) {
                        updateDailyTrendChart()
                    } else if (selectedTimeRange == TimeRange.MONTH) {
                        updateWeeklyTrendChart()
                    }
                }
            }
            
            // Observe overall usage
            overallUsage.observe(viewLifecycleOwner) { overallUsageData ->
                if (overallUsageData != null) {
                    // Format total time
                    val totalTime = formatDuration(overallUsageData.totalTime)
                    binding.tvTotalTime.apply {
                        text = totalTime
                        textSize = 28f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.chart_blue))
                    }
                    
                    // Format over limit time if available
                    if (overallUsageData.overLimitTime > 0) {
                        val overLimitTime = formatDuration(overallUsageData.overLimitTime)
                        binding.tvOverLimitTime.text = getString(R.string.over_limit_format, overLimitTime)
                        binding.tvOverLimitTime.visibility = View.VISIBLE
                    } else {
                        binding.tvOverLimitTime.visibility = View.GONE
                    }
                }
            }
            
            // Observe usage patterns
            usagePatterns.observe(viewLifecycleOwner) { usagePatternsData ->
                if (usagePatternsData != null) {
                    updateUsagePatterns(usagePatternsData)
                }
            }
            
            // Observe usage spikes
            usageSpikes.observe(viewLifecycleOwner) { usageSpikesData ->
                if (usageSpikesData.isNotEmpty()) {
                    updateUsageSpikes()
                }
            }
        }
    }

    private fun formatDuration(millis: Long, forceHourFormat: Boolean = false): String {
        if (millis < 1000) return "< 1s"
        
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        return when {
            hours > 0 || forceHourFormat -> {
                if (minutes > 0 && !forceHourFormat) {
                    "${hours}h ${minutes}m"
                } else {
                    "${hours}h"
                }
            }
            minutes > 0 -> {
                if (seconds > 0 && minutes < 10) {
                    "${minutes}m ${seconds}s"
                } else {
                    "${minutes}m"
                }
            }
            else -> "${seconds}s"
        }
    }

    private fun updateOverallUsage() {
        try {
            // Format total time based on selected time range with better styling
            val totalTime = when (selectedTimeRange) {
                TimeRange.TODAY -> "5h 23m"
                TimeRange.WEEK -> "32h 15m"
                TimeRange.MONTH -> "127h 42m"
            }
            binding.tvTotalTime.apply {
                text = totalTime
                textSize = 28f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.chart_blue))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overall usage", e)
        }
    }

    private fun setHourlyChartWidth() {
        try {
            // Get the screen width
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            
            // Set the chart width to the screen width
            val layoutParams = binding.hourlyUsageChart.layoutParams
            layoutParams.width = screenWidth
            binding.hourlyUsageChart.layoutParams = layoutParams
            
            Log.d(TAG, "Set hourly chart width to screen width: $screenWidth")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting hourly chart width", e)
        }
    }

    private fun updateHourlyUsageChart() {
        try {
            Log.d(TAG, "Updating hourly usage chart with individual bars for each hour")
            val hourlyUsageChart = binding.hourlyUsageChart
            
            // Reset the chart completely
            hourlyUsageChart.clear()
            hourlyUsageChart.fitScreen()
            
            // Set the chart width to ensure it can display all 24 bars
            setHourlyChartWidth()
            
            // Basic chart configuration
            hourlyUsageChart.description.isEnabled = false
            hourlyUsageChart.legend.isEnabled = false
            hourlyUsageChart.setDrawGridBackground(false)
            hourlyUsageChart.setDrawBorders(false)
            hourlyUsageChart.setScaleEnabled(false)  // Disable scaling to prevent user manipulation
            hourlyUsageChart.setPinchZoom(false)
            hourlyUsageChart.setExtraOffsets(10f, 10f, 10f, 10f)
            
            // Configure X axis for 24 hours
            val xAxis = hourlyUsageChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(true)
            xAxis.gridColor = ContextCompat.getColor(requireContext(), R.color.background_tertiary)
            xAxis.gridLineWidth = 0.5f
            xAxis.granularity = 1f
            xAxis.labelCount = 6  // Show fewer labels to avoid crowding
            xAxis.textSize = 10f
            xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val hour = value.toInt()
                    // Only show a few hour labels to avoid crowding
                    return if (hour % 4 == 0) "${hour}h" else ""
                }
            }
            
            // Configure Y axis
            val leftAxis = hourlyUsageChart.axisLeft
            leftAxis.setDrawGridLines(true)
            leftAxis.gridColor = ContextCompat.getColor(requireContext(), R.color.background_secondary)
            leftAxis.gridLineWidth = 0.5f
            leftAxis.axisMinimum = 0f
            leftAxis.textSize = 10f
            leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
            leftAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return formatDuration(value.toLong() * 60 * 1000, forceHourFormat = false)
                }
            }
            
            hourlyUsageChart.axisRight.isEnabled = false
            
            // Get real usage data
            viewModel.hourlyUsage.value?.let { hourlyUsageData ->
                Log.d(TAG, "Received hourly usage data: ${hourlyUsageData.size} data points")
                
                // Create a map to hold data for all 24 hours, initialized to 0
                val hourlyMap = (0..23).associateWith { 0f }.toMutableMap()
                
                // Fill in the data we have
                for (hourData in hourlyUsageData) {
                    // Convert milliseconds to minutes for better visualization
                    val usageMinutes = hourData.timeSpent / (60 * 1000)
                    hourlyMap[hourData.hour] = usageMinutes.toFloat()
                    Log.d(TAG, "Hour ${hourData.hour}: ${usageMinutes}m")
                }
                
                // Create entries for all 24 hours
                val entries = hourlyMap.entries.sortedBy { it.key }.map { 
                    BarEntry(it.key.toFloat(), it.value)
                }
                
                Log.d(TAG, "Created ${entries.size} bar entries for hourly chart")
                
                if (entries.isNotEmpty()) {
                    val dataSet = BarDataSet(entries, "Hourly Usage")
                    dataSet.color = ContextCompat.getColor(requireContext(), R.color.chart_blue)
                    dataSet.highLightColor = ContextCompat.getColor(requireContext(), R.color.chart_green)
                    dataSet.setDrawValues(false)
                    
                    val barData = BarData(dataSet)
                    barData.setValueTextSize(9f)
                    barData.setDrawValues(false)
                    barData.barWidth = 0.5f  // Make bars narrower
                    
                    hourlyUsageChart.data = barData
                    
                    // Set fixed axis ranges
                    xAxis.axisMinimum = -0.5f
                    xAxis.axisMaximum = 23.5f
                    
                    // Disable auto-scaling
                    hourlyUsageChart.setAutoScaleMinMaxEnabled(false)
                    
                    // Set fixed visible range
                    hourlyUsageChart.setVisibleXRange(0f, 24f)
                    
                    // Disable all animations to prevent rendering issues
                    hourlyUsageChart.animateY(0)
                    
                    // Force the chart to display all 24 hours
                    hourlyUsageChart.setVisibleXRangeMinimum(24f)
                    hourlyUsageChart.setVisibleXRangeMaximum(24f)
                    
                    // Ensure the chart is visible
                    hourlyUsageChart.visibility = View.VISIBLE
                    
                    // Force a layout pass
                    hourlyUsageChart.requestLayout()
                    
                    // Invalidate the chart to force a redraw
                    hourlyUsageChart.invalidate()
                    
                    Log.d(TAG, "Hourly usage chart updated with 24 individual bars")
                    return
                } else {
                    Log.d(TAG, "No entries created for hourly chart")
                }
            } ?: run {
                Log.d(TAG, "Hourly usage data is null")
            }
            
            // No data available
            hourlyUsageChart.setNoDataText("No usage data available")
            hourlyUsageChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            hourlyUsageChart.invalidate()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating hourly usage chart", e)
            binding.hourlyUsageChart.setNoDataText("Error loading data")
            binding.hourlyUsageChart.invalidate()
        }
    }

    private fun updateDailyTrendChart() {
        try {
            Log.d(TAG, "Updating daily trend chart")
            val overallTrendChart = binding.overallTrendChart
            
            // Configure chart appearance
            overallTrendChart.description.isEnabled = false
            overallTrendChart.legend.isEnabled = false
            overallTrendChart.setDrawGridBackground(false)
            overallTrendChart.setDrawBorders(false)
            overallTrendChart.setScaleEnabled(false)
            overallTrendChart.setPinchZoom(false)
            overallTrendChart.setViewPortOffsets(50f, 20f, 20f, 50f)
            overallTrendChart.setExtraOffsets(10f, 10f, 10f, 10f)
            
            // Configure X axis for daily data (7 days)
            val xAxis = overallTrendChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.labelCount = 7
            xAxis.textSize = 10f
            xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
            
            // Configure Y axis
            val leftAxis = overallTrendChart.axisLeft
            leftAxis.setDrawGridLines(true)
            leftAxis.gridColor = ContextCompat.getColor(requireContext(), R.color.background_secondary)
            leftAxis.gridLineWidth = 0.5f
            leftAxis.axisMinimum = 0f
            leftAxis.textSize = 10f
            leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
            leftAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return formatDuration(value.toLong() * 60 * 1000, forceHourFormat = true)
                }
            }
            
            overallTrendChart.axisRight.isEnabled = false
            
            // Get real usage data
            viewModel.weeklyTrends.value?.let { dailyUsageData ->
                if (dailyUsageData.isNotEmpty()) {
                    // Set up X axis labels with day names
                    val dayLabels = dailyUsageData.map { it.dayOfWeek }.toTypedArray()
                    xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
                    
                    // Create entries for the chart
                    val entries = ArrayList<Entry>()
                    for (i in dailyUsageData.indices) {
                        // Convert milliseconds to minutes for better visualization
                        val usageMinutes = dailyUsageData[i].timeSpent / (60 * 1000)
                        entries.add(Entry(i.toFloat(), usageMinutes.toFloat()))
                    }
                    
                    if (entries.isNotEmpty()) {
                        val dataSet = LineDataSet(entries, "Daily Usage")
                        dataSet.color = ContextCompat.getColor(requireContext(), R.color.chart_blue)
                        dataSet.setDrawCircles(true)
                        dataSet.circleRadius = 5f
                        dataSet.circleHoleRadius = 2.5f
                        dataSet.circleColors = listOf(ContextCompat.getColor(requireContext(), R.color.chart_blue))
                        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.chart_blue))
                        dataSet.circleHoleColor = ContextCompat.getColor(requireContext(), R.color.background_tertiary)
                        dataSet.lineWidth = 2.5f
                        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
                        dataSet.cubicIntensity = 0.2f
                        dataSet.fillAlpha = 110
                        dataSet.fillColor = ContextCompat.getColor(requireContext(), R.color.chart_blue)
                        dataSet.setDrawFilled(true)
                        dataSet.highLightColor = ContextCompat.getColor(requireContext(), R.color.chart_green)
                        dataSet.setDrawHorizontalHighlightIndicator(false)
                        
                        val lineData = LineData(dataSet)
                        lineData.setValueTextSize(10f)
                        lineData.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_normal))
                        lineData.setDrawValues(true)
                        lineData.setValueFormatter(object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return formatDuration(value.toLong() * 60 * 1000, forceHourFormat = false)
                            }
                        })
                        
                        overallTrendChart.data = lineData
                        overallTrendChart.animateXY(1000, 1000)
                        overallTrendChart.invalidate()
                        overallTrendChart.visibility = View.VISIBLE
                        
                        Log.d(TAG, "Daily trend chart updated successfully with real data")
                        return
                    }
                }
                
                // No data available
                overallTrendChart.setNoDataText("No usage data available")
                overallTrendChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                overallTrendChart.invalidate()
                Log.d(TAG, "No daily usage data available")
            } ?: run {
                // No data available
                overallTrendChart.setNoDataText("No usage data available")
                overallTrendChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                overallTrendChart.invalidate()
                Log.d(TAG, "Weekly trends data is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating daily trend chart", e)
            binding.overallTrendChart.setNoDataText("Error loading data")
            binding.overallTrendChart.invalidate()
        }
    }

    private fun updateWeeklyTrendChart() {
        try {
            Log.d(TAG, "Updating weekly trend chart")
            val weeklyTrendChart = binding.weeklyTrendChart
            
            // Configure chart appearance
            weeklyTrendChart.description.isEnabled = false
            weeklyTrendChart.legend.isEnabled = false
            weeklyTrendChart.setDrawGridBackground(false)
            weeklyTrendChart.setDrawBorders(false)
            weeklyTrendChart.setScaleEnabled(false)
            weeklyTrendChart.setPinchZoom(false)
            weeklyTrendChart.setViewPortOffsets(50f, 20f, 20f, 50f)
            weeklyTrendChart.setExtraOffsets(10f, 10f, 10f, 10f)
            
            // Configure X axis for weekly data
            val xAxis = weeklyTrendChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.labelCount = 4
            xAxis.textSize = 10f
            xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
            
            // Configure Y axis
            val leftAxis = weeklyTrendChart.axisLeft
            leftAxis.setDrawGridLines(true)
            leftAxis.gridColor = ContextCompat.getColor(requireContext(), R.color.background_secondary)
            leftAxis.gridLineWidth = 0.5f
            leftAxis.axisMinimum = 0f
            leftAxis.textSize = 10f
            leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
            leftAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return formatDuration(value.toLong() * 60 * 1000, forceHourFormat = true)
                }
            }
            
            weeklyTrendChart.axisRight.isEnabled = false
            
            // Get real usage data
            viewModel.weeklyTrends.value?.let { weeklyUsageData ->
                if (weeklyUsageData.isNotEmpty()) {
                    // Set up X axis labels with week numbers
                    val weekLabels = weeklyUsageData.map { it.dayOfWeek }.toTypedArray()
                    xAxis.valueFormatter = IndexAxisValueFormatter(weekLabels)
                    
                    // Create entries for the chart
                    val entries = ArrayList<Entry>()
                    for (i in weeklyUsageData.indices) {
                        // Convert milliseconds to minutes for better visualization
                        val usageMinutes = weeklyUsageData[i].timeSpent / (60 * 1000)
                        entries.add(Entry(i.toFloat(), usageMinutes.toFloat()))
                    }
                    
                    if (entries.isNotEmpty()) {
                        val dataSet = LineDataSet(entries, "Weekly Usage")
                        dataSet.color = ContextCompat.getColor(requireContext(), R.color.chart_blue)
                        dataSet.setDrawCircles(true)
                        dataSet.circleRadius = 6f
                        dataSet.circleHoleRadius = 3f
                        dataSet.circleColors = listOf(ContextCompat.getColor(requireContext(), R.color.chart_blue))
                        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.chart_blue))
                        dataSet.circleHoleColor = ContextCompat.getColor(requireContext(), R.color.background_tertiary)
                        dataSet.lineWidth = 3f
                        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
                        dataSet.cubicIntensity = 0.2f
                        dataSet.fillAlpha = 110
                        dataSet.fillColor = ContextCompat.getColor(requireContext(), R.color.chart_blue)
                        dataSet.setDrawFilled(true)
                        dataSet.highLightColor = ContextCompat.getColor(requireContext(), R.color.chart_green)
                        dataSet.setDrawHorizontalHighlightIndicator(false)
                        
                        val lineData = LineData(dataSet)
                        lineData.setValueTextSize(12f)
                        lineData.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_normal))
                        lineData.setDrawValues(true)
                        lineData.setValueFormatter(object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return formatDuration(value.toLong() * 60 * 1000, forceHourFormat = true)
                            }
                        })
                        
                        weeklyTrendChart.data = lineData
                        weeklyTrendChart.animateXY(1200, 1200)
                        weeklyTrendChart.invalidate()
                        weeklyTrendChart.visibility = View.VISIBLE
                        
                        Log.d(TAG, "Weekly trend chart updated successfully with real data")
            return
                    }
                }
                
                // No data available
                weeklyTrendChart.setNoDataText("No usage data available")
                weeklyTrendChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                weeklyTrendChart.invalidate()
                Log.d(TAG, "No weekly usage data available")
            } ?: run {
                // No data available
                weeklyTrendChart.setNoDataText("No usage data available")
                weeklyTrendChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                weeklyTrendChart.invalidate()
                Log.d(TAG, "Weekly trends data is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating weekly trend chart", e)
            binding.weeklyTrendChart.setNoDataText("Error loading data")
            binding.weeklyTrendChart.invalidate()
        }
    }

    private fun updateUsagePatterns(patterns: UsagePatterns) {
        binding.tvPeakHours.apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            text = getString(R.string.peak_hours_format, patterns.peakHours)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setBackgroundResource(R.drawable.rounded_background)
            setPadding(24, 12, 24, 12)
        }
        
        binding.tvAvgSessionTime.apply {
            textSize = 16f
            setTypeface(null, Typeface.NORMAL)
            text = getString(R.string.avg_session_format, formatDuration(patterns.avgSessionTime))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setBackgroundResource(R.drawable.rounded_background)
            setPadding(24, 12, 24, 12)
        }
    }

    private fun updateCharts() {
        try {
            Log.d(TAG, "Updating charts for time range: $selectedTimeRange")
            
            // Check if we have usage stats permission
            if (!checkUsageStatsPermission()) {
                requestUsageStatsPermission()
                return
            }
            
            // Show appropriate chart based on time range
            when (selectedTimeRange) {
                TimeRange.TODAY -> {
                    // For Today: Show hourly data for 24 hours
                    Log.d(TAG, "Selected TODAY timeframe - showing hourly chart")
                    binding.hourlyUsageChart.visibility = View.VISIBLE
                    binding.overallTrendChart.visibility = View.GONE
                    binding.weeklyTrendChart.visibility = View.GONE
                    
                    // Force update the hourly chart
                    updateHourlyUsageChart()
                }
                TimeRange.WEEK -> {
                    // For Week: Show daily data for 7 days
                    Log.d(TAG, "Selected WEEK timeframe - showing daily trend chart")
                    binding.hourlyUsageChart.visibility = View.GONE
                    binding.overallTrendChart.visibility = View.VISIBLE
                    binding.weeklyTrendChart.visibility = View.GONE
                    updateDailyTrendChart()
                }
                TimeRange.MONTH -> {
                    // For Month: Show weekly data
                    Log.d(TAG, "Selected MONTH timeframe - showing weekly trend chart")
                    binding.hourlyUsageChart.visibility = View.GONE
                    binding.overallTrendChart.visibility = View.GONE
                    binding.weeklyTrendChart.visibility = View.VISIBLE
                    updateWeeklyTrendChart()
                }
            }
            
            updateAppBreakdown()
            updateCategoryBreakdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating charts", e)
        }
    }

    /**
     * Check if the app has usage stats permission
     */
    private fun checkUsageStatsPermission(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            requireContext().packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    /**
     * Request usage stats permission by showing a dialog and directing the user to settings
     */
    private fun requestUsageStatsPermission() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Permission Required")
            .setMessage("To show app usage statistics, MindSync needs permission to access usage data. Please grant this permission in Settings.")
            .setPositiveButton("Go to Settings") { dialog: DialogInterface, _: Int ->
                val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                // Show mock data instead
                updateOverallUsage()
                updateAppBreakdown()
                updateCategoryBreakdown()
            }
            .setCancelable(false)
            .show()
    }

    private fun updateAppBreakdown() {
        try {
            // Get real app usage data
            viewModel.appUsageBreakdown.value?.let { appUsageData ->
                if (appUsageData.isNotEmpty()) {
                    Log.d(TAG, "Processing real app usage data: ${appUsageData.size} apps")
                    
                    // Filter out apps with very small usage time (less than 1 minute)
                    val filteredAppData = appUsageData.filter { it.timeSpent > 60 * 1000 }
                    Log.d(TAG, "Filtered to ${filteredAppData.size} apps with significant usage")
                    
                    if (filteredAppData.isEmpty()) {
                        Log.d(TAG, "No apps with significant usage found")
                        binding.rvAppUsage.visibility = View.GONE
                        return
                    }
                    
                    // Create adapter data
                    val appUsageItems = filteredAppData.map { appData ->
                        val category = getCategoryForApp(appData.packageName)
                        val usageTime = formatDuration(appData.timeSpent)
                        val percentage = calculatePercentage(appData.timeSpent, filteredAppData.sumOf { it.timeSpent })
                        
                        Log.d(TAG, "App: ${appData.appName}, Category: $category, Usage: $usageTime, Percentage: $percentage%")
                        
                        AppUsageItem(
                            appName = appData.appName,
                            category = category,
                            usageTime = usageTime,
                            percentage = percentage
                        )
                    }
                    
                    // Update RecyclerView
                    if (!::appUsageAdapter.isInitialized) {
                        appUsageAdapter = AppUsageAdapter()
                        binding.rvAppUsage.adapter = appUsageAdapter
                        binding.rvAppUsage.layoutManager = LinearLayoutManager(requireContext())
                    }
                    
                    binding.rvAppUsage.visibility = View.VISIBLE
                    binding.rvAppUsage.post {
                        appUsageAdapter.submitList(appUsageItems)
                    }
                    
                    Log.d(TAG, "App breakdown updated with real data: ${appUsageItems.size} apps")
                } else {
                    Log.d(TAG, "No app usage data available")
                    binding.rvAppUsage.visibility = View.GONE
                }
            } ?: run {
                Log.d(TAG, "App usage breakdown value is null")
                binding.rvAppUsage.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating app breakdown", e)
            binding.rvAppUsage.visibility = View.GONE
        }
    }

    private fun updateUsageSpikes() {
        try {
            // Get real usage spikes data
            viewModel.usageSpikes.value?.let { usageSpikesData ->
                if (usageSpikesData.isNotEmpty()) {
                    // Convert to adapter items
                    val spikes = usageSpikesData.map { spike ->
                        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                        
                        UsageSpikeItem(
                            appName = spike.appName,
                            date = dateFormat.format(spike.startTime),
                            duration = formatDuration(spike.duration),
                            timeRange = "${timeFormat.format(spike.startTime)} - ${timeFormat.format(spike.endTime)}"
                        )
                    }
                    
                    usageSpikesAdapter.submitList(spikes)
                    binding.rvUsageSpikes.visibility = View.VISIBLE
                    Log.d(TAG, "Usage spikes updated with real data: ${spikes.size} spikes")
                    return
                }
                
                // No spikes available
                binding.rvUsageSpikes.visibility = View.GONE
                Log.d(TAG, "No usage spikes available")
            } ?: run {
                // No spikes available
                binding.rvUsageSpikes.visibility = View.GONE
                Log.d(TAG, "Usage spikes data is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating usage spikes", e)
            binding.rvUsageSpikes.visibility = View.GONE
        }
    }

    private fun updateCategoryBreakdown() {
        try {
            // Since categoryPieChart doesn't exist in the layout, we'll skip this function
            // or implement it when the chart is added to the layout
            Log.d(TAG, "Category breakdown chart not implemented yet")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating category breakdown", e)
        }
    }

    /**
     * Calculate percentage of app usage compared to total usage
     */
    private fun calculatePercentage(appTime: Long, totalTime: Long): Int {
        if (totalTime <= 0) return 0
        return ((appTime.toDouble() / totalTime.toDouble()) * 100).toInt()
    }
    
    /**
     * Get category for app based on package name
     * This is a simple implementation - in a real app, you would have a more sophisticated categorization
     */
    private fun getCategoryForApp(packageName: String): String {
        val lowerPackage = packageName.lowercase()
        
        // Log the package name for debugging
        Log.d(TAG, "Categorizing app: $packageName")
        
        return when {
            // Social Media
            lowerPackage.contains("instagram") || 
            lowerPackage.contains("facebook") || 
            lowerPackage.contains("twitter") || 
            lowerPackage.contains("snapchat") || 
            lowerPackage.contains("whatsapp") || 
            lowerPackage.contains("telegram") || 
            lowerPackage.contains("messenger") ||
            lowerPackage.contains("tiktok") ||
            lowerPackage.contains("discord") ||
            lowerPackage.contains("linkedin") ||
            lowerPackage.contains("pinterest") ||
            lowerPackage.contains("reddit") ||
            lowerPackage.contains("wechat") ||
            lowerPackage.contains("line") ||
            lowerPackage.contains("viber") -> "Social"
            
            // Entertainment
            lowerPackage.contains("youtube") || 
            lowerPackage.contains("netflix") || 
            lowerPackage.contains("spotify") || 
            lowerPackage.contains("music") || 
            lowerPackage.contains("video") || 
            lowerPackage.contains("game") ||
            lowerPackage.contains("play") ||
            lowerPackage.contains("hulu") ||
            lowerPackage.contains("disney") ||
            lowerPackage.contains("prime") ||
            lowerPackage.contains("twitch") ||
            lowerPackage.contains("pandora") ||
            lowerPackage.contains("tidal") ||
            lowerPackage.contains("deezer") ||
            lowerPackage.contains("hbo") ||
            lowerPackage.contains("media") -> "Entertainment"
            
            // Productivity
            lowerPackage.contains("gmail") || 
            lowerPackage.contains("outlook") || 
            lowerPackage.contains("mail") || 
            lowerPackage.contains("calendar") || 
            lowerPackage.contains("docs") || 
            lowerPackage.contains("office") || 
            lowerPackage.contains("sheets") ||
            lowerPackage.contains("word") ||
            lowerPackage.contains("excel") ||
            lowerPackage.contains("powerpoint") ||
            lowerPackage.contains("notion") ||
            lowerPackage.contains("evernote") ||
            lowerPackage.contains("onenote") ||
            lowerPackage.contains("trello") ||
            lowerPackage.contains("asana") ||
            lowerPackage.contains("slack") ||
            lowerPackage.contains("teams") ||
            lowerPackage.contains("zoom") ||
            lowerPackage.contains("meet") ||
            lowerPackage.contains("drive") -> "Productivity"
            
            // Web Browsers
            lowerPackage.contains("chrome") || 
            lowerPackage.contains("firefox") || 
            lowerPackage.contains("browser") || 
            lowerPackage.contains("edge") || 
            lowerPackage.contains("opera") ||
            lowerPackage.contains("safari") ||
            lowerPackage.contains("brave") ||
            lowerPackage.contains("duckduckgo") -> "Web"
            
            // Shopping
            lowerPackage.contains("amazon") ||
            lowerPackage.contains("ebay") ||
            lowerPackage.contains("walmart") ||
            lowerPackage.contains("shop") ||
            lowerPackage.contains("store") ||
            lowerPackage.contains("etsy") ||
            lowerPackage.contains("wish") ||
            lowerPackage.contains("aliexpress") ||
            lowerPackage.contains("target") -> "Shopping"
            
            // Health & Fitness
            lowerPackage.contains("fitness") ||
            lowerPackage.contains("health") ||
            lowerPackage.contains("workout") ||
            lowerPackage.contains("exercise") ||
            lowerPackage.contains("fit") ||
            lowerPackage.contains("run") ||
            lowerPackage.contains("step") ||
            lowerPackage.contains("track") ||
            lowerPackage.contains("diet") ||
            lowerPackage.contains("meditation") ||
            lowerPackage.contains("calm") ||
            lowerPackage.contains("headspace") -> "Health & Fitness"
            
            // System Apps
            lowerPackage.contains("android") ||
            lowerPackage.contains("google") ||
            lowerPackage.contains("system") ||
            lowerPackage.contains("settings") ||
            lowerPackage.contains("phone") ||
            lowerPackage.contains("dialer") ||
            lowerPackage.contains("contacts") ||
            lowerPackage.contains("camera") ||
            lowerPackage.contains("gallery") ||
            lowerPackage.contains("photo") ||
            lowerPackage.contains("clock") ||
            lowerPackage.contains("calculator") -> "System"
            
            // Default category for unknown apps
            else -> {
                Log.d(TAG, "Unknown category for app: $packageName")
                "Other"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




