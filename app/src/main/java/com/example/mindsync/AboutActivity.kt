package com.example.mindsync

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        val tvAbout = findViewById<TextView>(R.id.tvAbout)
        tvAbout.text = "MindSync helps you curtail your social media use and regain control of your attention. Enjoy a minimalist interface and detailed usage analytics."
    }
}
