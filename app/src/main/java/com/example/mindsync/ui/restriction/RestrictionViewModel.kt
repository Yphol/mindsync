package com.example.mindsync.ui.restriction

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mindsync.data.model.AppInfo
import com.example.mindsync.data.model.AppRestriction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class RestrictionViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _filteredApps = MutableLiveData<List<AppInfo>>()
    val filteredApps: LiveData<List<AppInfo>> = _filteredApps

    private val _activeRestrictions = MutableLiveData<List<AppRestriction>>()
    val activeRestrictions: LiveData<List<AppRestriction>> = _activeRestrictions

    private var allApps = listOf<AppInfo>()
    private var currentQuery = ""
    private var showSystemApps = false

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                allApps = withContext(Dispatchers.IO) {
                    val packageManager = getApplication<Application>().packageManager
                    val installedApps = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
                    Log.d("RestrictionViewModel", "Found ${installedApps.size} total installed packages")
                    
                    installedApps
                        .filter { packageInfo ->
                            // Include all apps that have a launch intent
                            val launchIntent = packageManager.getLaunchIntentForPackage(packageInfo.packageName)
                            val hasLaunchIntent = launchIntent != null
                            Log.d("RestrictionViewModel", "Package: ${packageInfo.packageName}, Has Launch Intent: $hasLaunchIntent")
                            hasLaunchIntent
                        }
                        .map { packageInfo ->
                            val appInfo = packageInfo.applicationInfo
                            AppInfo(
                                packageName = packageInfo.packageName,
                                appName = packageManager.getApplicationLabel(appInfo).toString(),
                                icon = packageManager.getApplicationIcon(appInfo),
                                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                            )
                        }
                        .sortedBy { it.appName }
                }
                Log.d("RestrictionViewModel", "Filtered to ${allApps.size} launchable apps")
                updateFilteredApps()
            } catch (e: Exception) {
                Log.e("RestrictionViewModel", "Error loading apps", e)
            }
        }
    }

    fun searchApps(query: String) {
        currentQuery = query
        updateFilteredApps()
    }

    fun setShowSystemApps(show: Boolean) {
        showSystemApps = show
        updateFilteredApps()
    }

    private fun updateFilteredApps() {
        var filtered = allApps

        // Apply system apps filter
        if (!showSystemApps) {
            filtered = filtered.filter { !it.isSystemApp }
        }

        // Apply search query
        if (currentQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.appName.contains(currentQuery, ignoreCase = true) ||
                it.packageName.contains(currentQuery, ignoreCase = true)
            }
        }

        _filteredApps.value = filtered
    }

    fun addRestriction(appInfo: AppInfo, endTime: Date?, isForever: Boolean) {
        val restriction = AppRestriction(
            packageName = appInfo.packageName,
            appName = appInfo.appName,
            icon = appInfo.icon,
            startTime = Date(),
            endTime = endTime,
            isForever = isForever
        )
        val currentList = _activeRestrictions.value.orEmpty().toMutableList()
        currentList.add(restriction)
        _activeRestrictions.value = currentList
    }

    fun removeRestriction(restriction: AppRestriction) {
        val currentList = _activeRestrictions.value.orEmpty().toMutableList()
        currentList.remove(restriction)
        _activeRestrictions.value = currentList
    }
} 