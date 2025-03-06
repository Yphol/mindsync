package com.example.mindsync.ui.focus

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.mindsync.R
import com.example.mindsync.databinding.FragmentFocusBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class FocusFragment : Fragment() {
    private var _binding: FragmentFocusBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FocusViewModel by viewModels()
    
    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var remainingTimeMillis: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFocusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTimePickers()
        setupButtons()
        observeViewModel()
    }

    private fun setupTimePickers() {
        binding.apply {
            hourPicker.apply {
                minValue = 0
                maxValue = 23
                value = 0
                setOnValueChangedListener { _, _, newVal ->
                    updateTimerDisplay()
                }
            }

            minutePicker.apply {
                minValue = 0
                maxValue = 59
                value = 25
                setOnValueChangedListener { _, _, newVal ->
                    updateTimerDisplay()
                }
            }

            secondPicker.apply {
                minValue = 0
                maxValue = 59
                value = 0
                setOnValueChangedListener { _, _, newVal ->
                    updateTimerDisplay()
                }
            }

            updateTimerDisplay()
        }
    }

    private fun setupButtons() {
        binding.apply {
            btnStartFocus.setOnClickListener {
                startFocusTimer()
            }

            btnStopFocus.setOnClickListener {
                stopFocusTimer()
            }

            btnPauseFocus.setOnClickListener {
                if (isTimerRunning) {
                    pauseFocusTimer()
                } else {
                    resumeFocusTimer()
                }
            }
        }
    }

    private fun startFocusTimer() {
        val totalSeconds = calculateTotalSeconds()
        if (totalSeconds <= 0) return

        binding.apply {
            hourPicker.isEnabled = false
            minutePicker.isEnabled = false
            secondPicker.isEnabled = false
            btnStartFocus.isEnabled = false
            btnStopFocus.isEnabled = true
            btnPauseFocus.isEnabled = true
            btnPauseFocus.text = getString(R.string.pause)

            // Start the animation
            focusAnimation.apply {
                setMinAndMaxProgress(0f, 1f)
                playAnimation()
            }
        }

        startCountDownTimer(totalSeconds * 1000L)
        isTimerRunning = true
        viewModel.startFocusSession(totalSeconds)
    }

    private fun pauseFocusTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        
        binding.apply {
            btnPauseFocus.text = getString(R.string.resume)
            focusAnimation.pauseAnimation()
        }

        viewModel.pauseFocusSession((remainingTimeMillis / 1000).toInt())
    }

    private fun resumeFocusTimer() {
        startCountDownTimer(remainingTimeMillis)
        isTimerRunning = true
        
        binding.apply {
            btnPauseFocus.text = getString(R.string.pause)
            focusAnimation.resumeAnimation()
        }

        viewModel.resumeFocusSession((remainingTimeMillis / 1000).toInt())
    }

    private fun startCountDownTimer(durationMillis: Long) {
        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMillis = millisUntilFinished
                updateTimerDisplayFromMillis(millisUntilFinished)
            }

            override fun onFinish() {
                remainingTimeMillis = 0
                completeTimer()
            }
        }.start()
    }

    private fun stopFocusTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        remainingTimeMillis = 0
        
        binding.apply {
            hourPicker.isEnabled = true
            minutePicker.isEnabled = true
            secondPicker.isEnabled = true
            btnStartFocus.isEnabled = true
            btnStopFocus.isEnabled = false
            btnPauseFocus.isEnabled = false
            btnPauseFocus.text = getString(R.string.pause)

            // Reset and stop the animation
            focusAnimation.apply {
                progress = 0f
                pauseAnimation()
            }
        }

        updateTimerDisplay()
        viewModel.stopFocusSession()
    }

    private fun completeTimer() {
        stopFocusTimer()
        viewModel.completeFocusSession()
    }

    private fun calculateTotalSeconds(): Int {
        return binding.run {
            hourPicker.value * 3600 + minutePicker.value * 60 + secondPicker.value
        }
    }

    private fun updateTimerDisplay() {
        val totalSeconds = calculateTotalSeconds()
        updateTimerDisplayFromMillis(totalSeconds * 1000L)
    }

    private fun updateTimerDisplayFromMillis(millis: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        binding.timerDisplay.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun observeViewModel() {
        viewModel.focusState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FocusState.Completed -> {
                    binding.focusAnimation.pauseAnimation()
                    binding.focusAnimation.progress = 1f
                }
                is FocusState.Running -> {
                    binding.focusAnimation.resumeAnimation()
                }
                is FocusState.Paused -> {
                    binding.focusAnimation.pauseAnimation()
                }
                is FocusState.Stopped -> {
                    binding.focusAnimation.pauseAnimation()
                    binding.focusAnimation.progress = 0f
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
} 