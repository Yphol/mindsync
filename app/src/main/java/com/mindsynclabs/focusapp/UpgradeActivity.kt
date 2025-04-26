package com.mindsynclabs.focusapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class UpgradeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgrade)
        val tvUpgrade = findViewById<TextView>(R.id.tvUpgrade)
        tvUpgrade.text = "Upgrade to MindSync Pro for advanced analytics, personalized recommendations, and additional focus tools."
    }
}
