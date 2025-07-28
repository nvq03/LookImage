package com.qateam.lookimage

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isFirstLaunch = sharedPref.getBoolean("isFirstLaunch", true)

        if (!isFirstLaunch) {
            // Đã vào rồi → chuyển luôn
            navigateNext()
            finish()
            return
        }

        setContentView(R.layout.activity_splash)
        btnNext = findViewById(R.id.button_next)

        btnNext.setOnClickListener {
            // Lưu trạng thái đã vào splash
            sharedPref.edit().putBoolean("isFirstLaunch", false).apply()

            navigateNext()
            finish()
        }
    }

    private fun navigateNext() {
            startActivity(Intent(this, PasswordActivity::class.java))
    }
}
