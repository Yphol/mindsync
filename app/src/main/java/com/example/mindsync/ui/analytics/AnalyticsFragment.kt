package com.example.mindsync.ui.analytics

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mindsync.R
import com.example.mindsync.data.model.*
import com.example.mindsync.databinding.FragmentAnalyticsBinding
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
            setPinchZoom(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.background_tertiary)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                textSize = 10f
                labelCount = 12
                granularity = 2f
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
                form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE
            }
            
            animateY(1000)
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
        }

        usageSpikesAdapter = UsageSpikesAdapter()
        binding.rvUsageSpikes.apply {
            adapter = usageSpikesAdapter
            layoutManager = LinearLayoutManager(context)
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_secondary))
            setPadding(16, 8, 16, 8)
            clipToPadding = false
        }
    }

    private fun setupTimeRangeSelection() {
        binding.timeRangeChipGroup.setOnCheckedChangeListener { group: ChipGroup, checkedId: Int ->
            when (checkedId) {
                binding.chipDaily.id -> {
                    viewModel.setTimeRange(TimeRangePreset.TODAY)
                    selectedTimeRange = TimeRange.TODAY
                }
                binding.chipWeekly.id -> {
                    viewModel.setTimeRange(TimeRangePreset.LAST_7_DAYS)
                    selectedTimeRange = TimeRange.WEEK
                }
                binding.chipMonthly.id -> {
                    viewModel.setTimeRange(TimeRangePreset.LAST_30_DAYS)
                    selectedTimeRange = TimeRange.MONTH
                }
            }
        }
        binding.chipDaily.isChecked = true
    }

    private fun setupDetailedAnalyticsButton() {
        binding.fabDetailedAnalytics.setOnClickListener {
            val intent = Intent(requireContext(), DetailedAnalyticsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.apply {
            overallUsage.observe(viewLifecycleOwner) { usage ->
                updateOverallUsage(usage)
            }
            appUsageBreakdown.observe(viewLifecycleOwner) { breakdown ->
                appUsageAdapter.submitList(breakdown)
            }
            hourlyUsage.observe(viewLifecycleOwner) { hourlyData ->
                if (selectedTimeRange == TimeRange.TODAY) {
                    updateHourlyUsageChart(hourlyData)
                }
            }
            weeklyTrends.observe(viewLifecycleOwner) { trends ->
                updateWeeklyTrendChart(trends)
            }
            usageSpikes.observe(viewLifecycleOwner) { spikes ->
                usageSpikesAdapter.submitList(spikes)
            }
            usagePatterns.observe(viewLifecycleOwner) { patterns ->
                updateUsagePatterns(patterns)
            }
        }
    }

    private fun updateOverallUsage(usage: OverallUsage) {
        // Format total time based on selected time range
        binding.tvTotalTime.apply {
            textSize = 32f
            setTypeface(null, Typeface.BOLD)
            text = formatDuration(usage.totalTime, selectedTimeRange == TimeRange.TODAY)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
        
        binding.tvOverLimitTime.apply {
            textSize = 14f
            setTypeface(null, Typeface.NORMAL)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_yellow))
            text = getString(R.string.over_limit_format, formatDuration(usage.overLimitTime, selectedTimeRange == TimeRange.TODAY))
        }

        when (selectedTimeRange) {
            TimeRange.TODAY -> {
                binding.overallTrendChart.visibility = View.GONE
                binding.hourlyUsageChart.visibility = View.VISIBLE
                // Hourly data will be updated by hourlyUsage observer
            }
            else -> {
                binding.overallTrendChart.visibility = View.VISIBLE
                binding.hourlyUsageChart.visibility = View.GONE
                
                val entries = usage.dailyUsage.mapIndexed { index, timeSpent ->
                    Entry(index.toFloat(), timeSpent.toFloat())
                }

                val dataSet = LineDataSet(entries, "Daily Usage").apply {
                    color = ContextCompat.getColor(requireContext(), R.color.chart_blue)
                    setDrawFilled(true)
                    fillColor = ContextCompat.getColor(requireContext(), R.color.chart_blue)
                    fillAlpha = 20
                    setDrawCircles(true)
                    circleRadius = 4f
                    circleHoleRadius = 2f
                    circleColors = listOf(ContextCompat.getColor(requireContext(), R.color.chart_blue))
                    setDrawCircleHole(true)
                    circleHoleColor = ContextCompat.getColor(requireContext(), R.color.background_primary)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    lineWidth = 2f
                    valueTextSize = 10f
                    valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                    setDrawValues(true)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return formatDuration(value.toLong(), false)
                        }
                    }
                }

                binding.overallTrendChart.apply {
                    data = LineData(dataSet)
                    invalidate()
                }
            }
        }
    }

    private fun updateHourlyUsageChart(hourlyData: List<HourlyUsage>) {
        val entries = hourlyData.mapIndexed { index, usage ->
            BarEntry(index.toFloat(), usage.timeSpent.toFloat())
        }

        val dataSet = BarDataSet(entries, "Hourly Usage").apply {
            color = ContextCompat.getColor(requireContext(), R.color.chart_blue)
            valueTextSize = 10f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return formatDuration(value.toLong())
                }
            }
            setDrawValues(false)
        }

        binding.hourlyUsageChart.apply {
            data = BarData(dataSet).apply {
                barWidth = 0.8f
            }
            xAxis.valueFormatter = IndexAxisValueFormatter((0..23).map { "${it}:00" }.toTypedArray())
            axisLeft.axisMinimum = 0f
            invalidate()
        }
    }

    private fun updateWeeklyTrendChart(trends: List<DailyUsage>) {
        val entries = trends.mapIndexed { index, usage ->
            Entry(index.toFloat(), usage.timeSpent.toFloat())
        }

        val dataSet = LineDataSet(entries, "Weekly Trend").apply {
            color = ContextCompat.getColor(requireContext(), R.color.chart_purple)
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.chart_purple)
            fillAlpha = 30
            setDrawCircles(true)
            circleRadius = 3f
            circleHoleRadius = 1.5f
            circleColors = listOf(ContextCompat.getColor(requireContext(), R.color.chart_purple))
            mode = LineDataSet.Mode.CUBIC_BEZIER
            lineWidth = 2f
            valueTextSize = 10f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return formatDuration(value.toLong())
                }
            }
        }

        binding.weeklyTrendChart.apply {
            data = LineData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(trends.map { it.dayOfWeek }.toTypedArray())
            invalidate()
        }
    }

    private fun updateUsagePatterns(patterns: UsagePatterns) {
        binding.tvPeakHours.apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            text = getString(R.string.peak_hours_format, patterns.peakHours)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_secondary))
            setPadding(16, 8, 16, 8)
        }
        
        binding.tvAvgSessionTime.apply {
            textSize = 16f
            setTypeface(null, Typeface.NORMAL)
            text = getString(R.string.avg_session_format, formatDuration(patterns.avgSessionTime))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_secondary))
            setPadding(16, 8, 16, 8)
        }
    }

    private fun formatDuration(timeInMillis: Long, forceHourFormat: Boolean = false): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
        
        return when {
            forceHourFormat -> String.format("%.1fh", hours + (minutes / 60.0))
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    private fun updateCharts(data: List<AppUsageData>) {
        try {
            // Update time format based on selected time range
            val useHourFormat = selectedTimeRange == TimeRange.TODAY
            
            // Update usage data with proper formatting
            updateUsageData(data, useHourFormat)
            
            // Update session analysis bar chart
            binding.overallTrendChart.apply {
                setDrawGridBackground(false)
                setDrawBorders(false)
                
                xAxis.apply {
                    setDrawGridLines(false)
                    setDrawAxisLine(true)
                    position = XAxis.XAxisPosition.BOTTOM
                    axisLineColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                    textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    setDrawAxisLine(false)
                    gridColor = ContextCompat.getColor(requireContext(), R.color.background_tertiary)
                    textColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
                    axisMinimum = 0f  // This ensures bars touch the x-axis
                }

                axisRight.isEnabled = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating charts", e)
        }
    }

    private fun updateUsageData(data: List<AppUsageData>, useHourFormat: Boolean) {
        data.forEach { usage ->
            val duration = formatDuration(usage.timeSpent, useHourFormat)
            // Update any UI elements that show the duration
            when (usage.packageName) {
                // Update specific app usage displays
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



