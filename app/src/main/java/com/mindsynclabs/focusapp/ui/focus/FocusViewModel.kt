package com.mindsynclabs.focusapp.ui.focus

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FocusViewModel @Inject constructor() : ViewModel() {
    private val _focusState = MutableLiveData<FocusState>(FocusState.Stopped)
    val focusState: LiveData<FocusState> = _focusState

    private val _totalFocusTime = MutableLiveData(0L)
    val totalFocusTime: LiveData<Long> = _totalFocusTime

    private var sessionStartTime: Long = 0
    private var currentSessionDuration: Int = 0
    private var pausedStartTime: Long = 0
    private var totalPausedTime: Long = 0

    fun startFocusSession(durationSeconds: Int) {
        sessionStartTime = System.currentTimeMillis()
        currentSessionDuration = durationSeconds
        totalPausedTime = 0
        _focusState.value = FocusState.Running(durationSeconds)
    }

    fun pauseFocusSession(remainingSeconds: Int) {
        pausedStartTime = System.currentTimeMillis()
        _focusState.value = FocusState.Paused(remainingSeconds)
    }

    fun resumeFocusSession(remainingSeconds: Int) {
        val pauseDuration = System.currentTimeMillis() - pausedStartTime
        totalPausedTime += pauseDuration
        _focusState.value = FocusState.Running(remainingSeconds)
    }

    fun stopFocusSession() {
        val endTime = System.currentTimeMillis()
        val totalDuration = endTime - sessionStartTime - totalPausedTime
        updateTotalFocusTime(totalDuration)
        _focusState.value = FocusState.Stopped
    }

    fun completeFocusSession() {
        val endTime = System.currentTimeMillis()
        val totalDuration = endTime - sessionStartTime - totalPausedTime
        updateTotalFocusTime(totalDuration)
        _focusState.value = FocusState.Completed(currentSessionDuration)
    }

    private fun updateTotalFocusTime(sessionDuration: Long) {
        _totalFocusTime.value = (_totalFocusTime.value ?: 0) + sessionDuration
    }
} 