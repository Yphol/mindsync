package com.mindsynclabs.focusapp

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

data class RestrictedAppData(
    val name: String,
    val icon: Drawable,
    val restrictedMinutes: Int,
    val timesOpened: Int,
    val dailyUsage: String,
    val weeklyUsage: String,
    val avgSessionLength: String,
    val lastOpened: String
)

class HomeActivity : AppCompatActivity() {

    private lateinit var llRestrictedApps: LinearLayout
    private lateinit var llRecommendedActions: LinearLayout
    private lateinit var tvScreenTimeToday: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tvScreenTimeToday = findViewById(R.id.tvScreenTimeToday)
        llRestrictedApps = findViewById(R.id.llRestrictedApps)
        llRecommendedActions = findViewById(R.id.llRecommendedActions)

        // For demo, set dummy screen time
        tvScreenTimeToday.text = "Screen Time Today: 2h 30m"

        // Populate restricted apps list with dummy data
        populateRestrictedApps()

        // Populate recommended actions list with installed apps
        populateRecommendedActions()
    }

    private fun populateRestrictedApps() {
        // Dummy data list for restricted apps
        val dummyData = listOf(
            RestrictedAppData("Instagram", getDrawable(R.mipmap.ic_launcher)!!, 60, 3, "1h", "5h", "10m", "8:00 AM"),
            RestrictedAppData("TikTok", getDrawable(R.mipmap.ic_launcher)!!, 45, 5, "45m", "4h", "8m", "9:00 AM"),
            RestrictedAppData("Snapchat", getDrawable(R.mipmap.ic_launcher)!!, 30, 2, "30m", "3h", "6m", "10:30 AM")
        )
        val inflater = LayoutInflater.from(this)
        for (app in dummyData) {
            val view = inflater.inflate(R.layout.item_restricted_app, llRestrictedApps, false)
            val tvAppName = view.findViewById<TextView>(R.id.tvAppName)
            val tvRestrictionType = view.findViewById<TextView>(R.id.tvRestrictionType)
            val imgAppIcon = view.findViewById<ImageView>(R.id.imgAppIcon)
            tvAppName.text = app.name
            imgAppIcon.setImageDrawable(app.icon)
            tvRestrictionType.text = "${app.restrictedMinutes}m remaining"
            llRestrictedApps.addView(view)
        }
    }

    private fun populateRecommendedActions() {
        val inflater = LayoutInflater.from(this)
        val packageManager: PackageManager = packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in apps) {
            if (packageManager.getLaunchIntentForPackage(appInfo.packageName) != null) {
                val view = inflater.inflate(R.layout.item_recommended_action, llRecommendedActions, false)
                val tvRecAppName = view.findViewById<TextView>(R.id.tvRecAppName)
                val imgRecAppIcon = view.findViewById<ImageView>(R.id.imgRecAppIcon)
                val btnRecAction = view.findViewById<Button>(R.id.btnRecAction)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                tvRecAppName.text = appName
                val icon = packageManager.getApplicationIcon(appInfo)
                imgRecAppIcon.setImageDrawable(icon)
                btnRecAction.text = "Action"
                btnRecAction.setOnClickListener {
                    val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    }
                }
                llRecommendedActions.addView(view)
            }
        }
    }
}
