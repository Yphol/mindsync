package com.mindsynclabs.focusapp.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    private val _notificationsEnabled = MutableLiveData<Boolean>()
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _darkModeEnabled = MutableLiveData<Boolean>()
    val darkModeEnabled: LiveData<Boolean> = _darkModeEnabled

    private val _dailyReminderEnabled = MutableLiveData<Boolean>()
    val dailyReminderEnabled: LiveData<Boolean> = _dailyReminderEnabled

    init {
        // Load initial settings
        loadSettings()
    }

    private fun loadSettings() {
        // In a real app, these would be loaded from SharedPreferences or a settings repository
        _notificationsEnabled.value = true
        _darkModeEnabled.value = false
        _dailyReminderEnabled.value = true
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // In a real app, save to SharedPreferences or a settings repository
            _notificationsEnabled.value = enabled
        }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // In a real app, save to SharedPreferences or a settings repository
            _darkModeEnabled.value = enabled
        }
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // In a real app, save to SharedPreferences or a settings repository
            _dailyReminderEnabled.value = enabled
        }
    }
} 