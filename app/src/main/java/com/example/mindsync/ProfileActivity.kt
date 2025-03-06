package com.example.mindsync

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var etEmail: EditText
    private lateinit var btnUpdate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        etEmail = findViewById(R.id.etEmail)
        btnUpdate = findViewById(R.id.btnUpdate)

        etEmail.setText(sharedPreferences.getString("email", ""))
        btnUpdate.setOnClickListener {
            val newEmail = etEmail.text.toString()
            sharedPreferences.edit().putString("email", newEmail).apply()
        }
    }
}
