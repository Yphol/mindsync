package com.example.mindsync

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.PopupMenu
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var rgTimeFrame: RadioGroup
    private lateinit var llTimeItems: LinearLayout
    private lateinit var switchGraphPercentage: Switch
    private lateinit var btnSettings: ImageButton
    // Footer buttons (using TextView for simplicity)
    private lateinit var btnHome: TextView
    private lateinit var btnStart: TextView
    private lateinit var btnReport: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SharedPreferences and set a default value for testing
        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        if (!sharedPreferences.contains("logged_in")) {
            // For testing purposes, set logged_in to true so the app doesn't redirect to LoginActivity
            sharedPreferences.edit().putBoolean("logged_in", true).apply()
        }

        // Now check the login status (this should always be true for testing)
        if (!sharedPreferences.getBoolean("logged_in", false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Initialize views (ensure these IDs exist in your activity_main.xml)
        rgTimeFrame = findViewById(R.id.rgTimeFrame)
        llTimeItems = findViewById(R.id.llTimeItems)
        switchGraphPercentage = findViewById(R.id.switchGraphPercentage)
        btnSettings = findViewById(R.id.btnSettings)
        btnHome = findViewById(R.id.btnHome)
        btnStart = findViewById(R.id.btnStart)
        btnReport = findViewById(R.id.btnReport)

        // Listener for Day/Week toggle
        rgTimeFrame.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbDay -> {
                    populateTimeItemsDay()
                    // TODO: Update graphs to show hourly data
                }
                R.id.rbWeek -> {
                    populateTimeItemsWeek()
                    // TODO: Update graphs to show daily data
                }
            }
        }
        // Initialize in Day mode (30 days)
        populateTimeItemsDay()

        // Settings button: show a drop-down menu with custom text color
        btnSettings.setOnClickListener { view ->
            showSettingsMenu(view)
        }

        // Footer navigation
        btnHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        btnStart.setOnClickListener {
            startActivity(Intent(this, StartActivity::class.java))
        }
        btnReport.setOnClickListener {
            // For now, re-launch MainActivity as the Report page
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun populateTimeItemsDay() {
        llTimeItems.removeAllViews()
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        for (i in 0 until 30) {
            val dateStr = sdf.format(calendar.time)
            val tv = TextView(this).apply {
                text = dateStr
                setPadding(16, 16, 16, 16)
                setTextColor(resources.getColor(R.color.lightText, null))
                setOnClickListener {
                    // Animate the view for visual feedback
                    it.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
                        it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }.start()
                    // Retrieve screen time data for the selected date
                    val selectedDate = calendar.time
                    fetchScreenTimeData(selectedDate)
                }
            }
            llTimeItems.addView(tv)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
    }

    private fun populateTimeItemsWeek() {
        llTimeItems.removeAllViews()
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = Calendar.getInstance()
        for (i in 0 until 7) {
            val dayStr = sdf.format(calendar.time)
            val tv = TextView(this).apply {
                text = dayStr
                setPadding(16, 16, 16, 16)
                setTextColor(resources.getColor(R.color.lightText, null))
                setOnClickListener {
                    it.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
                        it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }.start()
                    val selectedDay = calendar.time
                    fetchScreenTimeData(selectedDay)
                }
            }
            llTimeItems.addView(tv)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
    }

    // Dummy function to "retrieve" and display screen time data for a given date
    private fun fetchScreenTimeData(date: Date) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateStr = sdf.format(date)
        val tvReportData = findViewById<TextView>(R.id.tvReportData)
        tvReportData.text = "Screen Time Data for $dateStr:\nNo data available."
    }

    // Show a PopupMenu for settings with a custom style (dark background, light text)
    private fun showSettingsMenu(anchor: View) {
        val wrapper = ContextThemeWrapper(this, R.style.PopupMenuStyle)
        val popup = PopupMenu(wrapper, anchor)
        if (sharedPreferences.getBoolean("logged_in", false)) {
            popup.menu.add("Profile")
            popup.menu.add("About")
            popup.menu.add("Upgrade")
            popup.menu.add("Signout")
        } else {
            popup.menu.add("Login")
            popup.menu.add("Signup")
            popup.menu.add("About")
            popup.menu.add("Upgrade")
        }
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.title) {
                "Profile" -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                "About" -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                "Upgrade" -> {
                    startActivity(Intent(this, UpgradeActivity::class.java))
                    true
                }
                "Signout" -> {
                    sharedPreferences.edit().clear().apply()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                "Login" -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                    true
                }
                "Signup" -> {
                    startActivity(Intent(this, SignupActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
