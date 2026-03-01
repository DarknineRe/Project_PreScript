package com.example.prescript

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class Theme : AppCompatActivity() {
    private var selectedTheme: String = "General"
    private lateinit var buttons: List<MaterialButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_theme)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttons = listOf(
            findViewById(R.id.btn_sport),
            findViewById(R.id.btn_fun),
            findViewById(R.id.btn_project_moon),
            findViewById(R.id.btn_game),
            findViewById(R.id.btn_general),
            findViewById(R.id.btn_daily_life)
        )

        buttons.forEach { button ->
            button.setOnClickListener {
                selectedTheme = button.text.toString()
                updateButtonStates()
            }
        }

        findViewById<Button>(R.id.btn_finish).setOnClickListener {
            saveThemeAndFinish()
        }

        // Load previously saved theme if any
        val sharedPref = getSharedPreferences("PreScriptPrefs", Context.MODE_PRIVATE)
        selectedTheme = sharedPref.getString("SELECTED_THEME", "General") ?: "General"
        updateButtonStates()
    }

    private fun updateButtonStates() {
        buttons.forEach { button ->
            if (button.text.toString() == selectedTheme) {
                button.strokeWidth = 4
                button.strokeColor = getColorStateList(android.R.color.white)
            } else {
                button.strokeWidth = 0
            }
        }
    }

    private fun saveThemeAndFinish() {
        val sharedPref = getSharedPreferences("PreScriptPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("SELECTED_THEME", selectedTheme)
            apply()
        }
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}