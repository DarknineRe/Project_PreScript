package com.example.prescript

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class Theme : AppCompatActivity() {
    private val selectedThemes = mutableSetOf<String>()
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
            findViewById(R.id.btn_sports),
            findViewById(R.id.btn_fun),
            findViewById(R.id.btn_dangerous),
            findViewById(R.id.btn_daily_task),
            findViewById(R.id.btn_project_moon),
            findViewById(R.id.btn_workout),
            findViewById(R.id.btn_prank),
            findViewById(R.id.btn_random),
            findViewById(R.id.btn_hardcore)
        )

        buttons.forEach { button ->
            button.setOnClickListener {
                val themeName = button.text.toString()
                if (selectedThemes.contains(themeName)) {
                    selectedThemes.remove(themeName)
                } else {
                    selectedThemes.add(themeName)
                }
                updateButtonStates()
            }
        }

        findViewById<Button>(R.id.btn_finish).setOnClickListener {
            saveThemesAndFinish()
        }

        // Load previously saved themes
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val sharedPref = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)
        val savedThemesString = sharedPref.getString("SELECTED_THEMES", "") ?: ""
        if (savedThemesString.isNotEmpty()) {
            selectedThemes.addAll(savedThemesString.split(","))
        }
        updateButtonStates()
    }

    private fun updateButtonStates() {
        buttons.forEach { button ->
            val themeName = button.text.toString()
            if (selectedThemes.contains(themeName)) {
                button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#475B74"))
                button.setTextColor(Color.WHITE)
            } else {
                button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E1DC"))
                button.setTextColor(Color.parseColor("#475B74"))
            }
        }
    }

    private fun saveThemesAndFinish() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val sharedPref = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("SELECTED_THEMES", selectedThemes.joinToString(","))
            putString("SELECTED_THEME", selectedThemes.joinToString(" and "))
            apply()
        }
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}