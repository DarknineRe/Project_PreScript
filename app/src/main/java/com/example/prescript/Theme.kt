package com.example.prescript

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.gridlayout.widget.GridLayout
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.example.prescript.R

class Theme : AppCompatActivity() {
    private val selectedThemes = mutableSetOf<String>()
    private lateinit var themeGrid: GridLayout
    
    // EASILY ADD NEW THEMES HERE
    private val themeList = listOf(
        "⚽ Sports",
        "🥳 Fun",
        "💀 Dangerous",
        "🗓 Daily Task",
        "🌙 Project Moon Game",
        "💪 Work out",
        "🤡 Prank",
        "🎲 Random",
        "🎨 Art",
        "🍳 Cooking",
        "🧘 Meditation",
        "🎮 Gaming",
        "📚 Reading",
        "🎵 Music",
        "🚫 Hardcore 🚫"
    )

    private val themeButtons = mutableListOf<MaterialButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_theme)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        themeGrid = findViewById(R.id.theme_grid)
        
        // Load previously saved themes
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val sharedPref = getSharedPreferences("PreScriptPrefs_$uid", Context.MODE_PRIVATE)
        val savedThemesString = sharedPref.getString("SELECTED_THEMES", "") ?: ""
        if (savedThemesString.isNotEmpty()) {
            selectedThemes.addAll(savedThemesString.split(","))
        }

        setupDynamicButtons()

        findViewById<Button>(R.id.btn_finish).setOnClickListener {
            saveThemesAndFinish()
        }
    }

    private fun setupDynamicButtons() {
        themeList.forEach { themeName ->
            // Using a standard MaterialButton constructor to avoid R resolution issues with Material attributes
            val button = MaterialButton(this).apply {
                text = themeName
                cornerRadius = 20.dpToPx()
                isAllCaps = false
                textSize = 14f
                setPadding(8.dpToPx(), 0, 8.dpToPx(), 0)
                
                // Layout Params for GridLayout
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                    setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                }
                
                // Special case for "Hardcore" to span 2 columns
                if (themeName.contains("Hardcore", ignoreCase = true)) {
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 2, 1f)
                }
                
                layoutParams = params
                
                setOnClickListener {
                    if (selectedThemes.contains(themeName)) {
                        selectedThemes.remove(themeName)
                    } else {
                        selectedThemes.add(themeName)
                    }
                    updateSingleButtonState(this, themeName)
                }
            }
            
            updateSingleButtonState(button, themeName)
            themeGrid.addView(button)
            themeButtons.add(button)
        }
    }

    private fun updateSingleButtonState(button: MaterialButton, themeName: String) {
        if (selectedThemes.contains(themeName)) {
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.hint_color))
            button.setTextColor(Color.WHITE)
        } else {
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.google_button_color))
            button.setTextColor(ContextCompat.getColor(this, R.color.hint_color))
        }
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun saveThemesAndFinish() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
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